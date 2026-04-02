# Catalogue de règles métier — Template

## 1. Objectif

Ce document sert de **template** pour formaliser les règles métier de palettisation.
Chaque règle doit être décrite de manière structurée afin de pouvoir être traduite en règle technique dans le moteur de règles.

> **Ce catalogue est à remplir avec le métier.** Les exemples ci-dessous sont des illustrations pour guider la formalisation.

---

## 2. Structure d'une règle

| Champ | Description | Exemple |
|-------|-------------|---------|
| **Code** | Identifiant unique de la règle | `R_HEAVY_BOTTOM` |
| **Nom** | Nom court lisible | Colis lourds en bas |
| **Description** | Formulation métier de la règle | Les colis dont le poids dépasse 15 kg doivent être placés dans la couche inférieure de la palette |
| **Portée** | Sur quoi s'applique la règle | `COLIS` / `PALETTE` / `INTER_COLIS` |
| **Sévérité** | Obligatoire ou préférentielle | `HARD` (jamais violable) / `SOFT` (préférentielle) |
| **Poids (si SOFT)** | Importance relative (1 à 100) | 80 |
| **Critères déclencheurs** | Quelles conditions activent la règle | Poids du colis >= 15 kg |
| **Effet attendu** | Quel comportement doit en résulter | Le colis est classé "BOTTOM" et placé en couche basse |
| **Exemples** | Cas concrets d'application | Colis de 22 kg → couche 1 ; Colis de 5 kg → couche haute |
| **Contre-exemples** | Cas où la règle ne s'applique pas | Colis de 10 kg → pas concerné |
| **Interactions connues** | Conflits potentiels avec d'autres règles | Peut entrer en conflit avec R_FRAGILE_TOP si un colis est lourd ET fragile |
| **Statut** | État de formalisation | `PROPOSÉE` / `VALIDÉE` / `IMPLÉMENTÉE` |

---

## 3. Catalogue des règles

### 3.1 Règles de classification

#### R_HEAVY_BOTTOM — Colis lourds en bas

| Champ | Valeur |
|-------|--------|
| **Code** | `R_HEAVY_BOTTOM` |
| **Nom** | Colis lourds en bas |
| **Description** | Les colis dont le poids dépasse un seuil défini doivent être placés dans les couches inférieures de la palette |
| **Portée** | `COLIS` |
| **Sévérité** | `HARD` |
| **Critères** | `poids >= SEUIL_LOURD` (seuil à définir : 15 kg ?) |
| **Effet** | `SET_ATTRIBUTE: stackingClass = BOTTOM` |
| **Exemples** | Colis 22 kg → BOTTOM, Colis 4 kg → non concerné |
| **Interactions** | R_LIGHT_TOP, R_FRAGILE_TOP |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Quel est le seuil de poids pour considérer un colis comme "lourd" ?

---

#### R_LIGHT_TOP — Colis légers en haut

| Champ | Valeur |
|-------|--------|
| **Code** | `R_LIGHT_TOP` |
| **Nom** | Colis légers en haut |
| **Description** | Les colis dont le poids est inférieur à un seuil défini doivent être placés dans les couches supérieures |
| **Portée** | `COLIS` |
| **Sévérité** | `SOFT` |
| **Poids** | 60 |
| **Critères** | `poids <= SEUIL_LEGER` (seuil à définir : 5 kg ?) |
| **Effet** | `SET_ATTRIBUTE: stackingClass = TOP` |
| **Exemples** | Colis 3 kg → TOP |
| **Interactions** | R_HEAVY_BOTTOM |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Quel est le seuil de poids pour considérer un colis comme "léger" ?

---

### 3.2 Règles de regroupement

#### R_FROZEN_GROUP — Surgelés ensemble

| Champ | Valeur |
|-------|--------|
| **Code** | `R_FROZEN_GROUP` |
| **Nom** | Surgelés ensemble |
| **Description** | Les colis à température "SURGELÉ" doivent être regroupés sur la même palette. Pas de mélange avec d'autres températures |
| **Portée** | `COLIS` |
| **Sévérité** | `HARD` |
| **Critères** | `température = SURGELÉ` |
| **Effet** | `GROUP_BY: temperature` |
| **Exemples** | 3 colis surgelés + 5 colis ambiants → palettes séparées |
| **Interactions** | R_FRESH_GROUP, R_TEMP_SEPARATION |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Cette règle s'applique-t-elle à toutes les températures (frais aussi) ou uniquement aux surgelés ?

---

#### R_TEMP_SEPARATION — Séparation par température

| Champ | Valeur |
|-------|--------|
| **Code** | `R_TEMP_SEPARATION` |
| **Nom** | Séparation par famille de température |
| **Description** | Les colis de familles de température différentes ne doivent pas être mélangés sur une même palette |
| **Portée** | `INTER_COLIS` |
| **Sévérité** | À définir (`HARD` ou `SOFT` ?) |
| **Critères** | `colisA.température != colisB.température` |
| **Effet** | `MUST_NOT_BE_TOGETHER` |
| **Exemples** | Colis surgelé + colis ambiant → palettes différentes |
| **Interactions** | R_FROZEN_GROUP |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Est-ce une règle absolue (HARD) ? Ou est-ce tolérable dans certains cas (SOFT) ?

---

### 3.3 Règles de capacité

#### R_MAX_HEIGHT_EURO — Hauteur max palette EURO

| Champ | Valeur |
|-------|--------|
| **Code** | `R_MAX_HEIGHT_EURO` |
| **Nom** | Hauteur maximale palette EURO |
| **Description** | La hauteur totale (support + colis empilés) d'une palette EURO ne doit pas dépasser une hauteur maximale |
| **Portée** | `PALETTE` |
| **Sévérité** | `HARD` |
| **Critères** | `support.type = EURO` |
| **Effet** | `SET_CONSTRAINT: maxHeight = 1800 mm` |
| **Exemples** | Palette à 1750 mm → OK ; Palette à 1850 mm → violation |
| **Interactions** | Aucune |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Quelle est la hauteur maximale exacte ? 1800 mm ? Varie-t-elle selon le client ou l'entrepôt ?

---

#### R_MAX_WEIGHT_EURO — Poids max palette EURO

| Champ | Valeur |
|-------|--------|
| **Code** | `R_MAX_WEIGHT_EURO` |
| **Nom** | Poids maximal palette EURO |
| **Description** | Le poids total des colis sur une palette EURO ne doit pas dépasser la charge maximale |
| **Portée** | `PALETTE` |
| **Sévérité** | `HARD` |
| **Critères** | `support.type = EURO` |
| **Effet** | `SET_CONSTRAINT: maxWeight = 800 kg` |
| **Exemples** | Palette à 750 kg → OK ; Palette à 850 kg → violation |
| **Interactions** | Aucune |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Quel est le poids maximal exact ? Varie-t-il par type de support ?

---

### 3.4 Règles de compatibilité inter-colis

#### R_FRAGILE_NO_HEAVY_ABOVE — Pas de colis lourd sur un fragile

| Champ | Valeur |
|-------|--------|
| **Code** | `R_FRAGILE_NO_HEAVY_ABOVE` |
| **Nom** | Pas de colis lourd au-dessus d'un fragile |
| **Description** | Un colis marqué "fragile" ne doit jamais avoir un colis lourd placé au-dessus de lui |
| **Portée** | `INTER_COLIS` |
| **Sévérité** | `HARD` |
| **Critères** | `colisA.fragilité = FRAGILE ET colisB.poids >= SEUIL_LOURD` |
| **Effet** | `FORBID_ABOVE: colisB ne doit pas être au-dessus de colisA` |
| **Exemples** | Colis fragile en couche 2, colis 20 kg en couche 3 → violation |
| **Interactions** | R_HEAVY_BOTTOM (peut créer un conflit si un colis est lourd et doit être en bas, mais un fragile est déjà en bas) |
| **Statut** | `PROPOSÉE` |

> **Question métier** : Le critère est-il uniquement le poids ? Ou aussi le volume / la pression exercée ?

---

### 3.5 Règles de choix de support

#### R_PREFER_EURO — Préférer les palettes EURO

| Champ | Valeur |
|-------|--------|
| **Code** | `R_PREFER_EURO` |
| **Nom** | Préférence pour les palettes EURO |
| **Description** | Quand le choix est possible, privilégier les palettes EURO plutôt que les demi-palettes ou les dollies |
| **Portée** | `PALETTE` |
| **Sévérité** | `SOFT` |
| **Poids** | 40 |
| **Critères** | Toujours applicable |
| **Effet** | `PREFERRED_SUPPORT: EURO` |
| **Exemples** | 3 colis pouvant tenir sur 1 EURO ou 2 demi-palettes → choisir 1 EURO |
| **Interactions** | R_MERGE_HALF_PALLETS |
| **Statut** | `PROPOSÉE` |

---

#### R_MERGE_HALF_PALLETS — Fusion de demi-palettes

| Champ | Valeur |
|-------|--------|
| **Code** | `R_MERGE_HALF_PALLETS` |
| **Nom** | Fusion de demi-palettes |
| **Description** | Quand deux demi-palettes de même température peuvent être fusionnées en une palette complète, le faire |
| **Portée** | `PALETTE` |
| **Sévérité** | `SOFT` |
| **Poids** | 70 |
| **Critères** | `2 demi-palettes ET même température ET poids combiné <= max ET hauteur combinée <= max` |
| **Effet** | `MERGE_SUPPORTS` |
| **Exemples** | 2 demi-palettes surgelées à moitié remplies → 1 palette EURO |
| **Interactions** | R_PREFER_EURO, R_TEMP_SEPARATION |
| **Statut** | `PROPOSÉE` |

---

### 3.6 Règles d'optimisation

#### R_MINIMIZE_PALLETS — Minimiser le nombre de palettes

| Champ | Valeur |
|-------|--------|
| **Code** | `R_MINIMIZE_PALLETS` |
| **Nom** | Minimiser le nombre de palettes |
| **Description** | L'objectif principal est de minimiser le nombre total de palettes utilisées |
| **Portée** | `PALETTE` |
| **Sévérité** | `SOFT` |
| **Poids** | 100 |
| **Critères** | Toujours applicable |
| **Effet** | `MINIMIZE_PALLETS` |
| **Interactions** | Toutes les règles de capacité et de regroupement |
| **Statut** | `PROPOSÉE` |

---

## 4. Règles à formaliser

Liste des règles identifiées mais pas encore formalisées. À compléter avec le métier.

| # | Nom provisoire | Description courte | Portée | Sévérité | Statut |
|---|---|---|---|---|---|
| 1 | Non-empilable | Certains colis ne peuvent pas avoir d'autres colis au-dessus | COLIS | HARD | À formaliser |
| 2 | Orientation obligatoire | Certains colis doivent rester dans une orientation spécifique | COLIS | HARD | À formaliser |
| 3 | Nombre max de colis par palette | Limite sur le nombre de colis (pas seulement poids/hauteur) | PALETTE | HARD | À formaliser |
| 4 | Stabilité minimale | Score de stabilité minimum requis pour valider une palette | PALETTE | SOFT | À formaliser |
| 5 | Regroupement par client/destination | Si multi-client, regrouper par destination | COLIS | À définir | À formaliser |
| 6 | Produits dangereux (ADR) | Séparation des produits dangereux | INTER_COLIS | HARD | À formaliser |
| 7 | Schéma de gerbage | Respect d'un schéma de gerbage prédéfini | PALETTE | À définir | À formaliser |
| 8 | Surplomb interdit | Un colis ne doit pas dépasser de la surface du colis en dessous | INTER_COLIS | HARD | À formaliser |

---

## 5. Matrice de priorisation des règles SOFT

Une fois les règles SOFT formalisées, remplir cette matrice pour définir les priorités relatives.

| Ordre | Code règle | Nom | Poids (1-100) | Justification |
|-------|-----------|-----|---------------|---------------|
| 1 | R_MINIMIZE_PALLETS | Minimiser le nombre de palettes | 100 | Objectif principal d'optimisation |
| 2 | R_MERGE_HALF_PALLETS | Fusion de demi-palettes | 70 | Réduit le nombre de supports |
| 3 | R_LIGHT_TOP | Colis légers en haut | 60 | Améliore la stabilité |
| 4 | R_PREFER_EURO | Préférer les palettes EURO | 40 | Standardisation des supports |
| ... | ... | ... | ... | ... |

---

## 6. Questions ouvertes pour le métier

1. Quels sont les seuils de poids exacts (lourd / moyen / léger) ?
2. Quelles sont les familles de température à gérer (AMBIANT, FRAIS, SURGELÉ, autre ?) ?
3. La séparation par température est-elle toujours absolue (HARD) ?
4. Les hauteurs/poids max varient-ils par type de support, par client, par entrepôt ?
5. Faut-il gérer des produits dangereux (ADR) ?
6. Faut-il gérer l'orientation des colis ?
7. Faut-il gérer le surplomb (un colis ne doit pas dépasser de celui du dessous) ?
8. Faut-il gérer des regroupements par destination / client ?
9. Y a-t-il des schémas de gerbage prédéfinis à respecter ?
10. Quel est l'objectif principal : minimiser les palettes ? Maximiser la stabilité ? Les deux ?
