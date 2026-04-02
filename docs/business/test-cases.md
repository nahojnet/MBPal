# Jeux de cas de test métier — Palettisation

## 1. Objectif

Ce document définit les **cas de test métier** permettant de valider le moteur de palettisation.
Chaque cas décrit une commande en entrée, les règles applicables, et le résultat attendu.

---

## 2. Référentiel de test

### 2.1 Produits de test

| product_id | Nom | Température | L (mm) | l (mm) | H (mm) | Poids (kg) | Fragilité | Empilable |
|------------|-----|-------------|---------|---------|---------|------------|-----------|-----------|
| PROD-001 | Carton lourd ambiant | AMBIANT | 400 | 300 | 250 | 22.0 | ROBUSTE | OUI |
| PROD-002 | Carton léger ambiant | AMBIANT | 300 | 200 | 150 | 3.5 | ROBUSTE | OUI |
| PROD-003 | Carton surgelé standard | SURGELÉ | 400 | 300 | 200 | 12.0 | ROBUSTE | OUI |
| PROD-004 | Carton frais standard | FRAIS | 350 | 250 | 200 | 8.0 | ROBUSTE | OUI |
| PROD-005 | Carton fragile ambiant | AMBIANT | 300 | 300 | 300 | 6.0 | FRAGILE | OUI |
| PROD-006 | Carton non empilable | AMBIANT | 500 | 400 | 400 | 15.0 | ROBUSTE | NON |
| PROD-007 | Petit colis surgelé | SURGELÉ | 200 | 150 | 100 | 2.0 | ROBUSTE | OUI |
| PROD-008 | Carton très lourd | AMBIANT | 600 | 400 | 300 | 45.0 | ROBUSTE | OUI |

### 2.2 Supports de test

| support_type_id | Code | Nom | L (mm) | l (mm) | H support (mm) | Charge max (kg) | Hauteur max (mm) | Fusionnable |
|-----------------|------|-----|---------|---------|-----------------|-----------------|-------------------|-------------|
| SUP-EURO | EURO | Palette EURO | 1200 | 800 | 150 | 800 | 1800 | NON |
| SUP-HALF | HALF | Demi-palette | 600 | 800 | 150 | 400 | 1800 | OUI (→ EURO) |
| SUP-DOLLY | DOLLY | Dolly | 600 | 400 | 170 | 250 | 1500 | NON |

---

## 3. Cas de test

### CAS-001 — Commande simple mono-température

**Objectif** : Vérifier le cas de base — une commande simple avec des colis homogènes.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (lourd ambiant, 22 kg) | 4 |
| PROD-002 (léger ambiant, 3.5 kg) | 6 |

**Supports autorisés** : EURO

**Règles applicables** : R_HEAVY_BOTTOM, R_LIGHT_TOP, R_MAX_HEIGHT_EURO, R_MAX_WEIGHT_EURO, R_MINIMIZE_PALLETS

**Résultat attendu** :
- 1 palette EURO
- Poids total : 4 × 22 + 6 × 3.5 = 109 kg (< 800 kg ✓)
- Couches basses : colis PROD-001 (lourds)
- Couches hautes : colis PROD-002 (légers)
- Vérifier hauteur totale < 1800 mm

**Vérifications** :
- [ ] Tous les colis lourds sont en couche basse
- [ ] Tous les colis légers sont en couche haute
- [ ] Hauteur totale respectée
- [ ] Poids total respecté
- [ ] 1 seule palette utilisée

---

### CAS-002 — Commande multi-température (séparation obligatoire)

**Objectif** : Vérifier la séparation par température.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (ambiant, 22 kg) | 3 |
| PROD-003 (surgelé, 12 kg) | 4 |
| PROD-004 (frais, 8 kg) | 2 |

**Supports autorisés** : EURO

**Règles applicables** : R_FROZEN_GROUP, R_TEMP_SEPARATION, R_HEAVY_BOTTOM, R_MINIMIZE_PALLETS

**Résultat attendu** :
- Minimum 2 palettes (idéalement 3 si séparation totale frais/ambiant/surgelé)
- Palette 1 : uniquement surgelés (PROD-003 × 4)
- Palette 2 : ambiants (PROD-001 × 3)
- Palette 3 : frais (PROD-004 × 2) — OU fusionnée avec ambiants si R_TEMP_SEPARATION est SOFT

**Vérifications** :
- [ ] Aucun mélange de température sur une palette (si HARD)
- [ ] Colis lourds en bas dans chaque palette
- [ ] Nombre de palettes cohérent avec les contraintes

---

### CAS-003 — Dépassement de capacité (poids)

**Objectif** : Vérifier la gestion du dépassement de la charge maximale.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-008 (très lourd, 45 kg) | 20 |

**Supports autorisés** : EURO (charge max 800 kg)

**Règles applicables** : R_MAX_WEIGHT_EURO, R_MINIMIZE_PALLETS

**Résultat attendu** :
- Poids total : 20 × 45 = 900 kg → dépasse 800 kg
- Minimum 2 palettes
- Palette 1 : max 17 colis (765 kg, le 18e ferait 810 kg)
- Palette 2 : 3 colis (135 kg)

**Vérifications** :
- [ ] Aucune palette ne dépasse 800 kg
- [ ] Nombre de palettes minimisé (2)
- [ ] Répartition équilibrée si possible

---

### CAS-004 — Dépassement de capacité (hauteur)

**Objectif** : Vérifier la gestion du dépassement de la hauteur maximale.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (H=250mm, 22 kg) | 10 |

**Supports autorisés** : EURO (hauteur max 1800 mm, hauteur support 150 mm)

**Règles applicables** : R_MAX_HEIGHT_EURO, R_MINIMIZE_PALLETS

**Résultat attendu** :
- Hauteur utile : 1800 - 150 = 1650 mm
- Par palette : max 6 colis empilés (6 × 250 = 1500 mm ≤ 1650 mm), 7 ferait 1750 mm (OK aussi)
- Vérifier combien de colis par couche en fonction de la surface
- Au minimum 2 palettes si empilage vertical

**Vérifications** :
- [ ] Aucune palette ne dépasse 1800 mm de hauteur totale
- [ ] Nombre de palettes minimisé

---

### CAS-005 — Colis fragiles avec colis lourds

**Objectif** : Vérifier la contrainte inter-colis (pas de lourd au-dessus d'un fragile).

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (lourd 22 kg, robuste) | 3 |
| PROD-005 (fragile 6 kg) | 3 |

**Supports autorisés** : EURO

**Règles applicables** : R_HEAVY_BOTTOM, R_FRAGILE_NO_HEAVY_ABOVE

**Résultat attendu** :
- 1 palette
- Les colis lourds (PROD-001) en couche basse
- Les colis fragiles (PROD-005) en couche haute
- Jamais un PROD-001 au-dessus d'un PROD-005

**Vérifications** :
- [ ] Aucun colis lourd n'est au-dessus d'un colis fragile
- [ ] Les colis lourds sont en bas
- [ ] Les colis fragiles sont en haut

---

### CAS-006 — Colis non empilable

**Objectif** : Vérifier la gestion des colis non empilables (rien au-dessus).

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-006 (non empilable, 15 kg) | 2 |
| PROD-002 (léger, 3.5 kg) | 5 |

**Supports autorisés** : EURO

**Règles applicables** : R_NON_STACKABLE (à créer), R_MINIMIZE_PALLETS

**Résultat attendu** :
- Les colis PROD-006 doivent être en couche supérieure (rien au-dessus)
- Les colis PROD-002 peuvent être en dessous ou sur une autre palette

**Vérifications** :
- [ ] Aucun colis au-dessus d'un PROD-006
- [ ] Nombre de palettes minimisé

---

### CAS-007 — Commande volumineuse (stress test)

**Objectif** : Vérifier la performance et la qualité sur une commande importante.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (lourd ambiant) | 50 |
| PROD-002 (léger ambiant) | 100 |
| PROD-003 (surgelé) | 30 |
| PROD-004 (frais) | 20 |
| PROD-005 (fragile) | 15 |

**Supports autorisés** : EURO, HALF

**Règles applicables** : Toutes

**Résultat attendu** :
- Temps de calcul < 5 secondes
- Séparation par température
- Lourds en bas, légers en haut
- Fragiles protégés
- Nombre de palettes minimisé
- Score de qualité calculé

**Vérifications** :
- [ ] Temps de calcul < 5 secondes
- [ ] Aucune violation de contrainte HARD
- [ ] Séparation par température respectée
- [ ] Colis lourds en bas dans chaque palette
- [ ] Score de qualité > seuil minimum

---

### CAS-008 — Utilisation de demi-palettes avec fusion

**Objectif** : Vérifier la gestion des supports fusionnables.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-007 (petit surgelé, 2 kg) | 4 |
| PROD-002 (léger ambiant, 3.5 kg) | 4 |

**Supports autorisés** : EURO, HALF

**Règles applicables** : R_TEMP_SEPARATION, R_MERGE_HALF_PALLETS, R_PREFER_EURO

**Résultat attendu** :
- Si séparation température HARD :
  - 1 demi-palette surgelés + 1 demi-palette ambiants
  - Fusion impossible (températures différentes)
  - Résultat : 2 demi-palettes
- Si séparation température SOFT :
  - Possibilité de fusionner en 1 EURO (selon le poids de la règle)

**Vérifications** :
- [ ] Supports choisis cohérents avec les quantités
- [ ] Fusion tentée si conditions remplies
- [ ] Température respectée selon sévérité

---

### CAS-009 — Commande avec un seul colis

**Objectif** : Vérifier le cas limite d'un seul colis.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| PROD-001 (lourd ambiant) | 1 |

**Supports autorisés** : EURO, HALF, DOLLY

**Résultat attendu** :
- 1 palette (le plus petit support adapté, ou EURO si préféré)
- 1 colis en couche 1, position 1

**Vérifications** :
- [ ] 1 seule palette
- [ ] Support adapté choisi
- [ ] Colis correctement positionné

---

### CAS-010 — Colis dépassant la capacité d'un support

**Objectif** : Vérifier la gestion d'erreur quand un colis est trop lourd/grand pour tous les supports.

**Entrée** :

| Produit | Quantité |
|---------|----------|
| Produit fictif (poids 500 kg, H=2000mm) | 1 |

**Supports autorisés** : DOLLY (charge max 250 kg)

**Résultat attendu** :
- Erreur explicite : "Le colis dépasse la capacité de tous les supports autorisés"
- Pas de palette générée
- Violation HARD remontée

**Vérifications** :
- [ ] Erreur correctement identifiée
- [ ] Message d'erreur clair
- [ ] Exécution en statut ERROR

---

## 4. Matrice de couverture

| Cas | Classification | Regroupement | Capacité poids | Capacité hauteur | Inter-colis | Support | Fusion | Performance | Erreur |
|-----|---------------|--------------|----------------|------------------|-------------|---------|--------|-------------|--------|
| CAS-001 | ✓ | | | | | | | | |
| CAS-002 | ✓ | ✓ | | | | | | | |
| CAS-003 | | | ✓ | | | | | | |
| CAS-004 | | | | ✓ | | | | | |
| CAS-005 | ✓ | | | | ✓ | | | | |
| CAS-006 | | | | | ✓ | | | | |
| CAS-007 | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | | ✓ | |
| CAS-008 | | ✓ | | | | ✓ | ✓ | | |
| CAS-009 | | | | | | ✓ | | | |
| CAS-010 | | | ✓ | | | | | | ✓ |

---

## 5. Cas de test à ajouter ultérieurement

| # | Description | Dépendance |
|---|-------------|------------|
| CAS-011 | Commande avec produits dangereux (ADR) | Formalisation règle ADR |
| CAS-012 | Commande avec colis à orientation obligatoire | Formalisation règle orientation |
| CAS-013 | Rejeu avec version de règles différente | Implémentation versioning |
| CAS-014 | Comparaison de deux stratégies de palettisation | Implémentation comparaison |
| CAS-015 | Commande de 500+ colis (test de charge) | Validation performance |
