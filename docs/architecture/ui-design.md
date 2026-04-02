# Design de l'IHM — MBPal

---

## 1. Objectifs

L'IHM React permet aux utilisateurs de :

1. **Créer et gérer les règles** de palettisation via des formulaires (pas de JSON)
2. **Visualiser les résultats** de palettisation (palettes, couches, scores)
3. **Lancer** des palettisations et des simulations
4. **Comparer** deux exécutions
5. **Administrer** les rulesets et les référentiels

---

## 2. Navigation principale

```
┌──────────────────────────────────────────────────────────────┐
│  MBPal          Dashboard │ Palettisation │ Règles │ Réf.   │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                     Zone de contenu                          │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

| Menu | Sous-menus |
|------|-----------|
| **Dashboard** | Vue d'ensemble, métriques |
| **Palettisation** | Lancer, Historique, Comparer |
| **Règles** | Créer/Éditer, Rulesets, Priorisation |
| **Référentiels** | Produits, Supports |

---

## 3. Écrans principaux

### 3.1 Dashboard

Vue d'ensemble des métriques clés.

```
┌─────────────────────────────────────────────────────────┐
│  Dashboard                                     [Aujourd'hui ▼]  │
├──────────┬──────────┬──────────┬──────────────────┤
│ 📊 156    │ 📊 2.3    │ 📊 78%    │ 📊 3              │
│ Commandes│ Palettes │ Taux     │ Erreurs          │
│ traitées │ /commande│ remplis. │                  │
├──────────┴──────────┴──────────┴──────────────────┤
│                                                    │
│  Dernières exécutions                              │
│  ┌────────────────────────────────────────────┐   │
│  │ CMD-000123 │ OK   │ 2 pal │ 87.5 │ 1.2s  │   │
│  │ CMD-000122 │ OK   │ 4 pal │ 72.0 │ 2.8s  │   │
│  │ CMD-000121 │ ERR  │ -     │ -    │ -     │   │
│  └────────────────────────────────────────────┘   │
│                                                    │
│  [Recherche rapide : ________________________________]  │
└────────────────────────────────────────────────────┘
```

---

### 3.2 Lancement de palettisation

```
┌─────────────────────────────────────────────────────────┐
│  Nouvelle palettisation                                  │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ID Commande :  [CMD-2026-000123          ]              │
│  Client :       [C001 - Client ABC        ▼]            │
│  Entrepôt :     [WH01                     ▼]            │
│                                                          │
│  Supports autorisés :                                    │
│    ☑ EURO (1200×800)                                     │
│    ☑ Demi-palette (600×800)                              │
│    ☐ Dolly (600×400)                                     │
│                                                          │
│  Ruleset :  [RULESET-2026-04-V1 (actif)   ▼]           │
│                                                          │
│  ─── ou ── Simulation ──                                 │
│    ☐ Mode simulation (dry run)                           │
│                                                          │
│                        [Lancer la palettisation]         │
└─────────────────────────────────────────────────────────┘
```

---

### 3.3 Résultat de palettisation

```
┌─────────────────────────────────────────────────────────┐
│  PAL-EXEC-000987                              [Exporter] │
├─────────────────────────────────────────────────────────┤
│  Commande : CMD-2026-000123    Client : C001             │
│  Statut : ● COMPLETED          Durée : 1.2s             │
│  Ruleset : RULESET-2026-04-V1  Score : 87.5 / 100       │
│                                                          │
│  ┌─────────────────────────────────────────────────┐     │
│  │  Résumé : 2 palettes │ 8 colis │ Remplissage 72% │   │
│  └─────────────────────────────────────────────────┘     │
│                                                          │
│  ┌─── Palette 1 (EURO) ──────────────────────────┐      │
│  │  Poids: 110 kg │ Hauteur: 1650 mm │ Stab: 90  │      │
│  │                                                │      │
│  │  Couche 3 (haut) ┌────────┐                    │      │
│  │                   │PROD-002│  3.5 kg  AMBIANT   │      │
│  │  Couche 2         ├────────┤                    │      │
│  │                   │PROD-001│ 22.0 kg  AMBIANT   │      │
│  │  Couche 1 (bas)   ├────────┤                    │      │
│  │                   │PROD-001│ 22.0 kg  AMBIANT   │      │
│  │                   └────────┘                    │      │
│  │  ═══════════════════════════  (palette EURO)    │      │
│  └────────────────────────────────────────────────┘      │
│                                                          │
│  ┌─── Palette 2 (EURO) ──────────────────────────┐      │
│  │  Poids: 36 kg │ Hauteur: 600 mm │ Stab: 85    │      │
│  │                                                │      │
│  │  Couche 2 (haut) ┌────────┐                    │      │
│  │                   │PROD-003│ 12.0 kg  SURGELÉ   │      │
│  │  Couche 1 (bas)   ├────────┤                    │      │
│  │                   │PROD-003│ 12.0 kg  SURGELÉ   │      │
│  │                   └────────┘                    │      │
│  │  ═══════════════════════════  (palette EURO)    │      │
│  └────────────────────────────────────────────────┘      │
│                                                          │
│  ─── Règles appliquées ─────────────────────────────     │
│  ● R_HEAVY_BOTTOM (HARD) : 5 colis classés BOTTOM       │
│  ● R_TEMP_SEPARATION (HARD) : 2 groupes séparés         │
│  ● R_MINIMIZE_PALLETS (SOFT, 100) : ✓ satisfaite        │
│                                                          │
│  ─── Violations ────────────────────────────────────     │
│  Aucune violation.                                       │
└─────────────────────────────────────────────────────────┘
```

**Code couleur par température** :
- Bleu : SURGELÉ
- Vert : FRAIS
- Orange : AMBIANT

**Badges de statut** :
- Gris : PENDING
- Bleu : PROCESSING
- Vert : COMPLETED
- Rouge : ERROR

---

### 3.4 Création / Édition de règles

C'est l'écran clé. Tout se fait par formulaires et menus déroulants, **jamais par édition JSON**.

#### Étape 1 — Informations de base

```
┌─────────────────────────────────────────────────────────┐
│  Créer une règle                          Étape 1/5     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Code :        [R_NEW_RULE                    ]          │
│  Description : [Les colis fragiles ne doivent  ]         │
│                [pas supporter de charge lourde  ]        │
│                                                          │
│  Portée :      [INTER_COLIS              ▼]             │
│                 ○ COLIS (sur un colis)                    │
│                 ○ PALETTE (sur une palette)               │
│                 ● INTER_COLIS (entre deux colis)          │
│                                                          │
│  Sévérité :    [HARD                     ▼]             │
│                 ● HARD (jamais violable)                  │
│                 ○ SOFT (préférentielle)                   │
│                                                          │
│  Poids (si SOFT) :  [___] (grisé si HARD)               │
│                                                          │
│                           [Suivant →]                    │
└─────────────────────────────────────────────────────────┘
```

#### Étape 2 — Constructeur de conditions

```
┌─────────────────────────────────────────────────────────┐
│  Conditions                               Étape 2/5     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Combinateur : [TOUTES les conditions (ET)  ▼]          │
│                                                          │
│  ┌─ Condition 1 ────────────────────────────────┐       │
│  │ Sujet:   [Colis A       ▼]                   │       │
│  │ Champ:   [poids_kg      ▼]                   │       │
│  │ Opér.:   [>=             ▼]                   │       │
│  │ Valeur:  [15               ]                  │       │
│  │                                    [✕ Suppr]  │       │
│  └──────────────────────────────────────────────┘       │
│                                                          │
│  ┌─ Condition 2 ────────────────────────────────┐       │
│  │ Sujet:   [Colis B       ▼]                   │       │
│  │ Champ:   [fragilité     ▼]                   │       │
│  │ Opér.:   [=              ▼]                   │       │
│  │ Valeur:  [FRAGILE        ▼]  ← dropdown enum │       │
│  │                                    [✕ Suppr]  │       │
│  └──────────────────────────────────────────────┘       │
│                                                          │
│  [+ Ajouter condition]  [+ Ajouter groupe (OU)]         │
│                                                          │
│  ─── Aperçu JSON (lecture seule) ──────────────────     │
│  {"all":[                                                │
│    {"subject":"packageA","field":"weight_kg",             │
│     "operator":">=","value":15},                         │
│    {"subject":"packageB","field":"fragility_level",      │
│     "operator":"=","value":"FRAGILE"}                    │
│  ]}                                                      │
│                                                          │
│                   [← Précédent]  [Suivant →]            │
└─────────────────────────────────────────────────────────┘
```

**Champs dynamiques selon la portée** :
- Si COLIS : pas de sélecteur de sujet (un seul colis)
- Si PALETTE : champs palette (support.code, totalWeight, etc.)
- Si INTER_COLIS : sélecteur "Colis A" / "Colis B"

**Valeurs dynamiques selon le champ** :
- `temperature_type` → dropdown : AMBIANT, FRAIS, SURGELÉ
- `fragility_level` → dropdown : FRAGILE, ROBUSTE
- `stackable_flag` → dropdown : OUI, NON
- `weight_kg`, `height_mm`, etc. → input numérique

#### Étape 3 — Constructeur d'effet

```
┌─────────────────────────────────────────────────────────┐
│  Effet                                    Étape 3/5     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Type d'effet :  [Interdire au-dessus     ▼]           │
│                                                          │
│  ┌─ Options (dynamiques selon le type) ────────────┐   │
│  │                                                  │   │
│  │  Au-dessus :  [Colis A (lourd)           ▼]     │   │
│  │  En-dessous : [Colis B (fragile)         ▼]     │   │
│  │                                                  │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│                   [← Précédent]  [Suivant →]            │
└─────────────────────────────────────────────────────────┘
```

**Types d'effets disponibles (dropdown)** :

| Catégorie | Types |
|-----------|-------|
| Classification | Définir un attribut (classe empilage, groupe température...) |
| Compatibilité | Regrouper par attribut, Doit être ensemble, Ne doit pas être ensemble |
| Positionnement | Doit être en bas, Doit être en haut, Interdire au-dessus, Priorité d'empilage |
| Capacité | Définir une contrainte (hauteur max, poids max, volume max, nb colis max) |
| Support | Supports autorisés, Support obligatoire, Support préféré |
| Optimisation | Minimiser palettes, Minimiser vide, Maximiser stabilité, Minimiser mélanges |

**Champs dynamiques selon le type sélectionné** :

| Type sélectionné | Champs affichés |
|-----------------|-----------------|
| Définir un attribut | Attribut [dropdown], Valeur [input/dropdown] |
| Regrouper par attribut | Attribut [dropdown] |
| Définir une contrainte | Contrainte [dropdown], Valeur [nombre], Unité [dropdown] |
| Interdire au-dessus | Au-dessus [dropdown], En-dessous [dropdown] |
| Support préféré | Support [dropdown] |
| Minimiser palettes | (aucun champ supplémentaire) |

#### Étape 4 — Explication

```
┌─────────────────────────────────────────────────────────┐
│  Explication                              Étape 4/5     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Explication métier (obligatoire) :                      │
│  ┌──────────────────────────────────────────────────┐   │
│  │ Un colis lourd (>= 15 kg) ne doit jamais être    │   │
│  │ placé au-dessus d'un colis fragile sur la même    │   │
│  │ palette.                                          │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│                   [← Précédent]  [Suivant →]            │
└─────────────────────────────────────────────────────────┘
```

#### Étape 5 — Résumé et sauvegarde

```
┌─────────────────────────────────────────────────────────┐
│  Résumé                                   Étape 5/5     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Code :        R_FRAGILE_NO_HEAVY_ABOVE                  │
│  Portée :      INTER_COLIS                               │
│  Sévérité :    HARD                                      │
│  Conditions :  Colis A poids >= 15 kg                    │
│                ET Colis B fragilité = FRAGILE             │
│  Effet :       Interdire Colis A au-dessus de Colis B    │
│  Explication : Un colis lourd ne doit jamais être        │
│                placé au-dessus d'un colis fragile        │
│                                                          │
│            [← Modifier]  [Sauvegarder (brouillon)]      │
│                          [Sauvegarder et publier]        │
└─────────────────────────────────────────────────────────┘
```

---

### 3.5 Tableau de priorisation des règles SOFT

```
┌─────────────────────────────────────────────────────────┐
│  Priorisation des règles SOFT                            │
│  Ruleset : [RULESET-2026-04-V1         ▼]              │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ≡  │ Ordre │ Code                │ Nom            │ Poids │
│  ───┼───────┼────────────────────┼────────────────┼───────│
│  ☰  │   1   │ R_MINIMIZE_PALLETS │ Min. palettes  │ [100] │
│  ☰  │   2   │ R_MERGE_HALF       │ Fusion demi-p. │ [ 70] │
│  ☰  │   3   │ R_LIGHT_TOP        │ Légers en haut │ [ 60] │
│  ☰  │   4   │ R_PREFER_EURO      │ Préférer EURO  │ [ 40] │
│                                                          │
│  ☰ = glisser-déposer pour réordonner                    │
│  Poids = éditable en ligne (0 à 100)                     │
│                                                          │
│                                    [Sauvegarder]        │
└─────────────────────────────────────────────────────────┘
```

---

### 3.6 Comparaison d'exécutions

```
┌─────────────────────────────────────────────────────────┐
│  Comparer deux exécutions                                │
├──────────────────────┬──────────────────────────────────┤
│  Exécution 1         │  Exécution 2                      │
│  PAL-EXEC-000987     │  PAL-EXEC-000988                  │
│  Ruleset V1          │  Ruleset V2                       │
│                      │                                   │
│  2 palettes          │  3 palettes        ▲ +1           │
│  Score: 87.5         │  Score: 72.0       ▼ -15.5        │
│  Remplissage: 72%    │  Remplissage: 65%  ▼ -7%          │
│                      │                                   │
│  PAL-1: 5 colis      │  PAL-1: 3 colis                   │
│  PAL-2: 3 colis      │  PAL-2: 3 colis                   │
│                      │  PAL-3: 2 colis    ← nouvelle     │
├──────────────────────┴──────────────────────────────────┤
│  Différences détaillées :                                │
│  • PROD-001-4 : PAL-1 → PAL-3                           │
│  • PROD-001-5 : PAL-1 → PAL-3                           │
│  • Règle R_LIGHT_TOP mieux satisfaite en V2 (+0.2)      │
└─────────────────────────────────────────────────────────┘
```

---

### 3.7 Historique et recherche

```
┌─────────────────────────────────────────────────────────┐
│  Historique des exécutions                               │
├─────────────────────────────────────────────────────────┤
│  Commande: [____________] Client: [________▼]           │
│  Du: [2026-04-01] Au: [2026-04-02] Statut: [Tous  ▼]  │
│                                          [Rechercher]   │
│                                                          │
│  ID Exécution    │ Commande     │ Stat │ Pal │ Score │ Durée │
│  ────────────────┼──────────────┼──────┼─────┼───────┼───────│
│  PAL-EXEC-000987 │ CMD-000123   │  ●   │  2  │ 87.5  │ 1.2s  │
│  PAL-EXEC-000986 │ CMD-000122   │  ●   │  4  │ 72.0  │ 2.8s  │
│  PAL-EXEC-000985 │ CMD-000121   │  ●   │  -  │  -    │  -    │
│  PAL-EXEC-000984 │ CMD-000120   │  ●   │  1  │ 95.0  │ 0.3s  │
│                                                          │
│  Page 1 / 50                     [< Préc] [Suiv >]      │
└─────────────────────────────────────────────────────────┘
```

---

### 3.8 Référentiels (produits et supports)

Tables CRUD standard avec :
- Tableau paginé, triable, filtrable
- Boutons Créer / Modifier / Désactiver
- Import CSV (lot)
- Export CSV

---

## 4. Design system

### 4.1 Couleurs par température

| Température | Couleur | Code |
|-------------|---------|------|
| SURGELÉ | Bleu | `#3B82F6` |
| FRAIS | Vert | `#10B981` |
| AMBIANT | Orange | `#F59E0B` |

### 4.2 Badges de statut

| Statut | Couleur | Code |
|--------|---------|------|
| PENDING | Gris | `#9CA3AF` |
| PROCESSING | Bleu | `#3B82F6` |
| COMPLETED | Vert | `#10B981` |
| ERROR | Rouge | `#EF4444` |

### 4.3 Sévérité

| Sévérité | Couleur | Affichage |
|----------|---------|-----------|
| HARD | Rouge | Badge rouge "OBLIGATOIRE" |
| SOFT | Jaune | Badge jaune "PRÉFÉRENTIELLE" |

---

## 5. Responsive

- **Desktop-first** : usage principal sur poste de travail (entrepôt / bureau)
- **Tablette** : compatible pour la consultation des résultats
- **Mobile** : non prioritaire (consultation uniquement)

---

## 6. Stack technique frontend

| Outil | Usage |
|-------|-------|
| React 18+ | Framework UI |
| React Router | Navigation |
| Axios / fetch | Appels API |
| Composants formulaires | Création de règles (dropdowns, condition builder) |
| Bibliothèque drag-and-drop | Tableau de priorisation |
| Bibliothèque de graphiques | Dashboard métriques |
