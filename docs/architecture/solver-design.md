# Design du solveur — MBPal

---

## 1. Objectif

Le solveur produit un **plan de palettisation** à partir de :
- une liste de colis (enrichis avec les attributs produit)
- un **ConstraintSet** (produit par le moteur de règles)

**Philosophie** : bonne solution rapide, pas solution optimale prouvée.

| Métrique | Cible |
|----------|-------|
| Commande standard (20-50 colis) | < 2 secondes |
| Commande volumineuse (200+ colis) | < 5 secondes |
| Contraintes HARD | 0 violation |
| Nombre de palettes | Minimisé |

---

## 2. Entrées

### 2.1 Liste de colis

Chaque colis est une instance enrichie :

```
BoxInstance
├── instanceId          (ex: "PROD-001-3")
├── productCode
├── weightKg
├── lengthMm, widthMm, heightMm
├── temperatureType
├── fragilityLevel
├── stackableFlag
├── maxStackWeightKg
├── stackingClass       (attribué par le moteur de règles)
├── temperatureGroup    (attribué par le moteur de règles)
└── orderLineId
```

### 2.2 ConstraintSet

Produit par le moteur de règles (voir [`rule-engine-design.md`](rule-engine-design.md)) :

```
ConstraintSet
├── classifications[]         → classes attribuées aux colis
├── groupings[]               → regroupements obligatoires
├── forbiddenPlacements[]     → interdictions de positionnement
├── capacityLimits[]          → limites de capacité par support
├── supportRules[]            → supports autorisés/préférés
└── optimizationObjectives[]  → objectifs d'optimisation pondérés
```

---

## 3. Sorties

```
PalletizationPlan
├── pallets[]
│     ├── palletNumber
│     ├── supportType
│     ├── totalWeightKg
│     ├── totalHeightMm
│     ├── fillRatePct
│     ├── stabilityScore
│     ├── layerCount
│     ├── items[]
│     │     ├── instanceId
│     │     ├── layerNo
│     │     ├── positionNo
│     │     └── stackingClass
│     └── satisfiedConstraints[]
├── violations[]              (SOFT uniquement, HARD = erreur)
├── globalScore
└── decisionTrace[]
```

---

## 4. Algorithme de palettisation

### Vue d'ensemble des étapes

```
Entrée: colis[] + ConstraintSet
        │
        ▼
  ┌─────────────────────┐
  │ Étape 1: Expansion   │  Commande → instances de colis
  │ et enrichissement     │
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 2: Pré-        │  Groupes par température,
  │ groupement            │  incompatibilités
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 3: Sélection   │  Supports autorisés
  │ des supports          │  par groupe
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 4: Répartition │  FFD modifié
  │ sur palettes (FFD)    │  (bin packing)
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 5: Ordonnanc.  │  Couches par poids
  │ intra-palette         │  et classe empilage
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 6: Amélioration │  Swaps, fusions,
  │ locale                 │  merge supports
  └──────────┬──────────┘
             ▼
  ┌─────────────────────┐
  │ Étape 7: Validation  │  Contraintes HARD,
  │ et scoring            │  score SOFT
  └──────────┬──────────┘
             ▼
        Sortie: PalletizationPlan
```

---

### Étape 1 — Expansion et enrichissement

À partir de la commande :
- Chaque ligne `(productCode, boxQuantity=N)` génère N instances de colis
- Chaque instance est enrichie avec les attributs du référentiel produit
- Les classifications du moteur de règles sont appliquées (stackingClass, temperatureGroup)

**Pseudocode** :
```
pour chaque ligne de commande (produit, quantité):
    produit = charger(produit.code)
    pour i de 1 à quantité:
        box = créer BoxInstance(produit, index=i)
        box.stackingClass = ConstraintSet.getClassification(box)
        box.temperatureGroup = ConstraintSet.getTemperatureGroup(box)
        boxes.ajouter(box)
```

---

### Étape 2 — Pré-groupement obligatoire

Créer des groupes de colis qui **doivent** être sur les mêmes palettes.

Critères de groupement (issus du ConstraintSet) :
- `GROUP_BY temperature_type` → tous les surgelés ensemble, tous les frais ensemble, etc.
- `MUST_BE_TOGETHER` → colis spécifiques liés
- `MUST_NOT_BE_TOGETHER` → incompatibilités (séparation en groupes distincts)

**Pseudocode** :
```
groupes = regrouper boxes par temperatureGroup

pour chaque contrainte MUST_NOT_BE_TOGETHER:
    séparer les colis incompatibles dans des groupes distincts

pour chaque contrainte MUST_BE_TOGETHER:
    fusionner les groupes contenant les colis liés
```

**Résultat** : liste de groupes indépendants. Chaque groupe sera palettisé séparément.

---

### Étape 3 — Sélection des supports

Pour chaque groupe, déterminer les supports utilisables :

```
pour chaque groupe:
    supportsAutorisés = ConstraintSet.getAllowedSupports(groupe)
    
    si ConstraintSet contient REQUIRED_SUPPORT pour ce groupe:
        supportsAutorisés = [supportObligatoire]
    
    si supportsAutorisés est vide:
        ERREUR("Aucun support compatible pour ce groupe")
    
    supportPréféré = ConstraintSet.getPreferredSupport(groupe)
    groupe.supports = supportsAutorisés
    groupe.supportPréféré = supportPréféré
```

---

### Étape 4 — Répartition sur palettes (bin packing FFD)

Algorithme **First Fit Decreasing modifié** :

```
pour chaque groupe:
    // Trier les colis par poids décroissant (les plus lourds d'abord)
    colis = groupe.colis.trierPar(poids DESC)
    
    palettes = []
    
    pour chaque colis dans colis:
        meilleurePalette = null
        meilleurScore = -infini
        
        pour chaque palette dans palettes:
            si peutAjouter(palette, colis):
                score = calculerScorePlacement(palette, colis)
                si score > meilleurScore:
                    meilleurePalette = palette
                    meilleurScore = score
        
        si meilleurePalette != null:
            meilleurePalette.ajouter(colis)
        sinon:
            // Créer une nouvelle palette
            support = choisirSupport(groupe, colis)
            nouvellePalette = créer Palette(support)
            nouvellePalette.ajouter(colis)
            palettes.ajouter(nouvellePalette)
```

**Fonction `peutAjouter(palette, colis)`** :
```
retourner:
    palette.poidsTotal + colis.poids <= palette.support.maxLoadKg
    ET palette.hauteurEstimée + colis.hauteur <= palette.support.maxTotalHeightMm
    ET pasDeContrainteForbiddenViolée(palette, colis)
    ET compatibilitéTemperature(palette, colis)
```

**Fonction `calculerScorePlacement(palette, colis)`** — Best Fit variant :
```
// Préférer la palette qui a le moins de place restante (meilleur remplissage)
capacitéRestante = palette.support.maxLoadKg - palette.poidsTotal - colis.poids
retourner -capacitéRestante  // plus la capacité restante est faible, meilleur le score
```

---

### Étape 5 — Ordonnancement intra-palette (couches)

Pour chaque palette, ordonner les colis en couches :

```
pour chaque palette:
    colis = palette.colis
    
    // Séparer par classe d'empilage
    colisBottom = colis.filtrer(stackingClass == "BOTTOM")
    colisMiddle = colis.filtrer(stackingClass == "MIDDLE" ou null)
    colisTop = colis.filtrer(stackingClass == "TOP")
    
    // Trier chaque groupe par poids décroissant
    colisBottom.trierPar(poids DESC)
    colisMiddle.trierPar(poids DESC)
    colisTop.trierPar(poids DESC)
    
    // Assembler dans l'ordre : BOTTOM → MIDDLE → TOP
    colisOrdonnés = colisBottom + colisMiddle + colisTop
    
    // Attribuer couches et positions
    couche = 1
    position = 1
    hauteurCumulée = palette.support.heightMm  // hauteur du support
    
    pour chaque colis dans colisOrdonnés:
        si hauteurCumulée + colis.hauteur > palette.support.maxTotalHeightMm:
            // Dépasse la hauteur max → déplacer vers une nouvelle palette
            déplacerVersNouvellePalette(colis)
            continuer
        
        colis.layerNo = couche
        colis.positionNo = position
        hauteurCumulée += colis.hauteur
        position++
        
        // Logique simplifiée : 1 colis par position en MVP
        couche++
        position = 1
```

> **Note MVP** : en placement logique, chaque couche contient un ou plusieurs colis. La logique de surface (combien de colis tiennent côte à côte sur une couche) sera affinée dans une version ultérieure.

---

### Étape 6 — Amélioration locale

Après l'assignation initiale, tenter d'améliorer la solution :

#### 6.1 Swap entre palettes

```
pour i de 1 à MAX_ITERATIONS:
    pour chaque paire de palettes (P1, P2):
        pour chaque colis C1 dans P1:
            pour chaque colis C2 dans P2:
                si swap(C1, C2) améliore le score global:
                    si swap(C1, C2) ne viole aucune contrainte HARD:
                        effectuer le swap
```

#### 6.2 Fusion de palettes sous-remplies

```
pour chaque paire de palettes (P1, P2) triée par remplissage croissant:
    si P1 et P2 sont compatibles (même température):
        si P1.poids + P2.poids <= support.maxLoad:
            si P1.hauteur + P2.hauteur <= support.maxHeight:
                fusionner P2 dans P1
```

#### 6.3 Fusion de supports fusionnables

```
demiPalettes = palettes.filtrer(support.code == "HALF")
trierPar(demiPalettes, temperature, poids)

pour chaque paire consécutive de demiPalettes de même température:
    poidsCombiné = dp1.poids + dp2.poids
    hauteurCombinée = max(dp1.hauteur, dp2.hauteur)
    
    supportEuro = charger("EURO")
    si poidsCombiné <= supportEuro.maxLoadKg:
        si hauteurCombinée <= supportEuro.maxTotalHeightMm:
            fusionner dp1 et dp2 en une palette EURO
```

#### 6.4 Limites

- Nombre d'itérations borné : `MAX_ITERATIONS = 100`
- Temps borné : si l'amélioration dépasse 2 secondes, arrêter et retourner la meilleure solution trouvée

---

### Étape 7 — Validation et scoring

#### 7.1 Validation des contraintes HARD

```
pour chaque palette:
    vérifier: poids <= maxLoad
    vérifier: hauteur <= maxHeight
    vérifier: pas de FORBID_ABOVE violé
    vérifier: pas de mélange température (si HARD)
    vérifier: colis non empilables en dernière couche

si violation HARD détectée:
    status = ERROR
    enregistrer violation
```

#### 7.2 Scoring des contraintes SOFT

```
scoreTotal = 0
poidsTotal = 0

pour chaque règle SOFT dans le ConstraintSet:
    satisfaction = évaluerSatisfaction(règle, palettes)  // 0.0 à 1.0
    scoreTotal += règle.weight * satisfaction
    poidsTotal += règle.weight

globalScore = (scoreTotal / poidsTotal) * 100
```

#### 7.3 Métriques de qualité

| Métrique | Calcul |
|----------|--------|
| **Taux de remplissage** | Σ volume colis / Σ volume disponible palette |
| **Score stabilité** | % de palettes avec lourds en bas et légers en haut |
| **Score température** | % de palettes sans mélange de température |
| **Nombre de palettes** | Total (moins = mieux) |

---

## 5. Gestion des supports fusionnables

### Logique de fusion

Après la palettisation initiale, le solveur vérifie si des supports fusionnables peuvent être combinés.

**Conditions de fusion** (exemple : 2 demi-palettes → 1 EURO) :
1. Les deux demi-palettes sont de même groupe de température
2. Le poids combiné ≤ charge max du support cible (EURO : 800 kg)
3. La hauteur combinée (max des deux) ≤ hauteur max du support cible
4. La fusion respecte toutes les contraintes HARD

**Bénéfice** : réduction du nombre total de supports → meilleur score.

---

## 6. Gestion des cas limites

| Cas | Comportement |
|-----|-------------|
| Commande vide (0 colis) | Erreur : "Aucun colis à palettiser" |
| Commande avec 1 seul colis | 1 palette avec le plus petit support adapté |
| Colis trop lourd pour tout support | Erreur avec violation HARD |
| Colis trop grand pour tout support | Erreur avec violation HARD |
| Tous les colis même température | Un seul groupe, pas de séparation |
| Plus de colis que la capacité totale | Créer autant de palettes que nécessaire |
| Conflit entre SOFT (ex: minimiser palettes vs séparer) | Résolu par pondération dans le scoring |

---

## 7. Complexité et performance

| Phase | Complexité | Justification |
|-------|-----------|---------------|
| Expansion | O(n) | Parcours linéaire |
| Pré-groupement | O(n) | Tri par attribut |
| FFD (tri) | O(n log n) | Tri par poids |
| FFD (assignation) | O(n × p) | n colis × p palettes |
| Ordonnancement | O(n log n) | Tri intra-palette |
| Amélioration locale | O(k × n²) | k itérations bornées |
| Validation | O(n × p) | Vérification complète |

Avec n = nombre de colis, p = nombre de palettes, k = itérations d'amélioration.

**Budget temps** :
- Étapes 1-5 : < 500 ms pour 200 colis
- Étape 6 (amélioration) : bornée à 2 secondes
- Étape 7 (validation) : < 500 ms
- **Total max** : < 5 secondes

Si le budget est atteint pendant l'amélioration locale, le solveur retourne la meilleure solution trouvée jusque-là.

---

## 8. Évolutions futures

| Version | Fonctionnalité |
|---------|---------------|
| v2 | Placement 2D : calcul de surface par couche (combien de colis par couche en fonction de L×l) |
| v3 | Placement 3D : coordonnées x, y, z exactes, gestion des rotations |
| v4 | Métaheuristiques : recuit simulé ou algorithme génétique pour améliorer l'optimisation |
| v5 | Apprentissage : exploiter l'historique des palettisations pour améliorer les heuristiques |
