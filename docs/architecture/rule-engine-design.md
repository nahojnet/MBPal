# Design du moteur de règles — MBPal

---

## 1. Objectifs

Le moteur de règles a pour rôle de :

1. **Découpler** les règles métier de l'algorithme de palettisation
2. **Versionner** les règles pour permettre le rejeu et l'audit
3. **Produire un ConstraintSet** exploitable par le solveur
4. **Tracer** chaque évaluation de règle pour expliquer les décisions
5. **Supporter les règles relationnelles** (inter-colis) dès le départ

---

## 2. Métamodèle de règle

### 2.1 Structure d'une règle

```json
{
  "ruleCode": "R_HEAVY_BOTTOM",
  "version": "1.0",
  "scope": "PACKAGE",
  "severity": "HARD",
  "weight": null,
  "condition": {
    "all": [
      { "field": "weight_kg", "operator": ">=", "value": 15 }
    ]
  },
  "effect": {
    "type": "SET_ATTRIBUTE",
    "attribute": "stackingClass",
    "value": "BOTTOM"
  },
  "explanation": "Les colis lourds (>= 15 kg) doivent être en couche basse"
}
```

### 2.2 Champs du métamodèle

| Champ | Type | Description |
|-------|------|-------------|
| `ruleCode` | string | Identifiant unique de la règle |
| `version` | string | Version sémantique (1.0, 1.1, 2.0) |
| `scope` | enum | `PACKAGE` / `PALLET` / `INTER_PACKAGE` |
| `severity` | enum | `HARD` (jamais violable) / `SOFT` (préférentielle) |
| `weight` | number | Poids de la règle SOFT dans le scoring (0-100), null si HARD |
| `condition` | object | Expression de condition (DSL) |
| `effect` | object | Effet à appliquer si la condition est vraie |
| `explanation` | string | Justification métier en langage naturel |

---

## 3. Système de conditions

### 3.1 Opérateurs

| Opérateur | Description | Exemple |
|-----------|-------------|---------|
| `=` | Égalité | `temperature_type = "SURGELE"` |
| `!=` | Différence | `fragility_level != "FRAGILE"` |
| `>` | Supérieur strict | `weight_kg > 20` |
| `>=` | Supérieur ou égal | `weight_kg >= 15` |
| `<` | Inférieur strict | `height_mm < 100` |
| `<=` | Inférieur ou égal | `weight_kg <= 5` |
| `IN` | Dans une liste | `temperature_type IN ["FRAIS", "SURGELE"]` |
| `NOT_IN` | Pas dans une liste | `support_code NOT_IN ["DOLLY"]` |
| `BETWEEN` | Entre deux valeurs | `weight_kg BETWEEN [5, 15]` |
| `IS_NULL` | Est null | `max_stack_weight_kg IS_NULL` |
| `IS_NOT_NULL` | N'est pas null | `max_stack_weight_kg IS_NOT_NULL` |

### 3.2 Combinateurs logiques

| Combinateur | Description | Comportement |
|-------------|-------------|--------------|
| `all` | ET logique | Toutes les conditions doivent être vraies |
| `any` | OU logique | Au moins une condition doit être vraie |
| `not` | Négation | La condition doit être fausse |

### 3.3 Conditions imbriquées

```json
{
  "all": [
    { "field": "temperature_type", "operator": "=", "value": "SURGELE" },
    {
      "any": [
        { "field": "weight_kg", "operator": ">=", "value": 15 },
        { "field": "fragility_level", "operator": "=", "value": "FRAGILE" }
      ]
    }
  ]
}
```

### 3.4 Champs de référence par portée

#### Portée PACKAGE (attributs d'un colis)

| Champ | Description |
|-------|-------------|
| `weight_kg` | Poids du colis |
| `height_mm` | Hauteur du colis |
| `length_mm` | Longueur du colis |
| `width_mm` | Largeur du colis |
| `temperature_type` | Famille de température |
| `fragility_level` | Niveau de fragilité |
| `stackable_flag` | Empilable Y/N |
| `max_stack_weight_kg` | Poids max supportable au-dessus |
| `orientation_fixed` | Orientation fixe Y/N |

#### Portée PALLET (attributs d'une palette en cours de constitution)

| Champ | Description |
|-------|-------------|
| `support.code` | Code du support |
| `support.max_load_kg` | Charge max du support |
| `support.max_total_height_mm` | Hauteur max du support |
| `totalWeight` | Poids total actuel |
| `totalHeight` | Hauteur totale actuelle |
| `boxCount` | Nombre de colis actuel |
| `fillRate` | Taux de remplissage actuel |

#### Portée INTER_PACKAGE (relation entre deux colis)

Pour les règles relationnelles, les conditions référencent **deux sujets** : `packageA` et `packageB`.

```json
{
  "scope": "INTER_PACKAGE",
  "condition": {
    "all": [
      { "subject": "packageA", "field": "weight_kg", "operator": ">=", "value": 15 },
      { "subject": "packageB", "field": "fragility_level", "operator": "=", "value": "FRAGILE" }
    ]
  },
  "effect": {
    "type": "FORBID_ABOVE",
    "above": "packageA",
    "below": "packageB"
  },
  "explanation": "Un colis lourd ne doit pas être placé au-dessus d'un colis fragile"
}
```

L'interprétation est : pour toute paire (A, B) de colis dans la même palette, si A pèse >= 15 kg et B est FRAGILE, alors A ne doit pas être au-dessus de B.

---

## 4. Catalogue des types d'effets

### 4.1 Effets de classification

| Type | Paramètres | Description |
|------|------------|-------------|
| `SET_ATTRIBUTE` | `attribute`, `value` | Attribue une classe au colis (stackingClass, temperatureGroup...) |

```json
{ "type": "SET_ATTRIBUTE", "attribute": "stackingClass", "value": "BOTTOM" }
```

### 4.2 Effets de compatibilité / regroupement

| Type | Paramètres | Description |
|------|------------|-------------|
| `GROUP_BY` | `attribute` | Regrouper les colis par valeur d'attribut (même palette) |
| `MUST_BE_TOGETHER` | — | Les colis matchés doivent être sur la même palette |
| `MUST_NOT_BE_TOGETHER` | — | Les colis matchés ne doivent pas être sur la même palette |

```json
{ "type": "GROUP_BY", "attribute": "temperature_type" }
```

### 4.3 Effets de positionnement

| Type | Paramètres | Description |
|------|------------|-------------|
| `MUST_BE_AT_BOTTOM` | — | Le colis doit être en couche basse |
| `MUST_BE_AT_TOP` | — | Le colis doit être en couche haute |
| `FORBID_ABOVE` | `above`, `below` | Interdit de placer `above` au-dessus de `below` |
| `STACKING_PRIORITY` | `priority` | Priorité d'empilage (1=bas, 10=haut) |

```json
{ "type": "FORBID_ABOVE", "above": "packageA", "below": "packageB" }
```

### 4.4 Effets de capacité

| Type | Paramètres | Description |
|------|------------|-------------|
| `SET_CONSTRAINT` | `constraint`, `value`, `unit` | Définit une contrainte de capacité sur la palette |

Contraintes possibles : `maxHeight`, `maxWeight`, `maxVolume`, `maxBoxCount`

```json
{ "type": "SET_CONSTRAINT", "constraint": "maxHeight", "value": 1800, "unit": "mm" }
```

### 4.5 Effets de choix de support

| Type | Paramètres | Description |
|------|------------|-------------|
| `ALLOWED_SUPPORTS` | `supports[]` | Liste des supports autorisés |
| `REQUIRED_SUPPORT` | `support` | Support obligatoire |
| `PREFERRED_SUPPORT` | `support` | Support préféré (SOFT) |

```json
{ "type": "PREFERRED_SUPPORT", "support": "EURO" }
```

### 4.6 Effets d'optimisation

| Type | Paramètres | Description |
|------|------------|-------------|
| `MINIMIZE_PALLETS` | — | Objectif : minimiser le nombre de palettes |
| `MINIMIZE_VOID` | — | Objectif : minimiser le vide |
| `MAXIMIZE_STABILITY` | — | Objectif : maximiser la stabilité |
| `MINIMIZE_MIX` | `attribute` | Objectif : minimiser les mélanges d'un attribut |

---

## 5. Cycle de vie des règles

### 5.1 États d'une version de règle

```
DRAFT  ──(publier)──▶  ACTIVE  ──(archiver)──▶  ARCHIVED
                          │
                          └──(nouvelle version)──▶  DRAFT (v+1)
```

| État | Description |
|------|-------------|
| `DRAFT` | Brouillon, modifiable, non utilisable en production |
| `ACTIVE` | Publiée, utilisable dans un ruleset, non modifiable |
| `ARCHIVED` | Historisée, non utilisable, conservée pour audit |

### 5.2 Ruleset

Un **ruleset** est un ensemble nommé de versions de règles actives.

- Un ruleset a un état : `DRAFT`, `ACTIVE`, `ARCHIVED`
- Un seul ruleset est `ACTIVE` à la fois pour la production
- Chaque exécution de palettisation référence le ruleset utilisé
- On peut créer un nouveau ruleset pour simuler un changement de règles

### 5.3 Association Ruleset ↔ Règles

```
RULESET "RULESET-2026-04-V1" (ACTIVE)
  ├── R_HEAVY_BOTTOM v1.0
  ├── R_LIGHT_TOP v1.0
  ├── R_FROZEN_GROUP v1.0
  ├── R_TEMP_SEPARATION v1.0
  ├── R_MAX_HEIGHT_EURO v1.0
  ├── R_MAX_WEIGHT_EURO v1.0
  ├── R_FRAGILE_NO_HEAVY_ABOVE v1.0
  ├── R_PREFER_EURO v1.0
  └── R_MINIMIZE_PALLETS v1.0
```

---

## 6. Mécanisme de priorisation des règles SOFT

### 6.1 Principe

Chaque règle SOFT dans un ruleset a :
- un **ordre de priorité** (1 = plus prioritaire)
- un **poids** (0 à 100) pour le scoring

### 6.2 Calcul du score

Lorsque le solveur évalue une solution :

```
score_global = Σ (poids_i × satisfaction_i) / Σ poids_i × 100
```

Où :
- `poids_i` = poids de la règle SOFT i
- `satisfaction_i` = 1 si la règle est satisfaite, 0 sinon (ou un ratio entre 0 et 1)

### 6.3 Tableau de priorisation

Administré via l'IHM. Exemple :

| Ordre | Code | Nom | Poids |
|-------|------|-----|-------|
| 1 | R_MINIMIZE_PALLETS | Minimiser palettes | 100 |
| 2 | R_MERGE_HALF_PALLETS | Fusion demi-palettes | 70 |
| 3 | R_LIGHT_TOP | Légers en haut | 60 |
| 4 | R_PREFER_EURO | Préférer EURO | 40 |

### 6.4 Résolution de conflits

En cas de conflit entre deux règles SOFT, celle avec le poids le plus élevé l'emporte. En cas d'égalité, l'ordre de priorité départage.

Les règles HARD ne sont jamais en conflit : si elles ne peuvent pas toutes être satisfaites, l'exécution est en erreur.

---

## 7. Traduction règles fonctionnelles → techniques

### Exemple 1 : Les colis lourds en bas

**Règle fonctionnelle** : "Les colis lourds doivent être en bas de palette"

**Traduction** :
1. Définir "lourd" : seuil métier à préciser (ex: >= 15 kg)
2. Scope : `PACKAGE`
3. Condition : `weight_kg >= 15`
4. Effet : `SET_ATTRIBUTE: stackingClass = BOTTOM`
5. Sévérité : `HARD`

```json
{
  "ruleCode": "R_HEAVY_BOTTOM",
  "scope": "PACKAGE",
  "severity": "HARD",
  "condition": { "all": [{ "field": "weight_kg", "operator": ">=", "value": 15 }] },
  "effect": { "type": "SET_ATTRIBUTE", "attribute": "stackingClass", "value": "BOTTOM" }
}
```

### Exemple 2 : Surgelés ensemble

**Règle fonctionnelle** : "Les produits surgelés doivent rester ensemble"

**Traduction** :
1. Scope : `PACKAGE`
2. Condition : `temperature_type = SURGELE`
3. Effet : `GROUP_BY: temperature_type`
4. Sévérité : `HARD`

```json
{
  "ruleCode": "R_FROZEN_GROUP",
  "scope": "PACKAGE",
  "severity": "HARD",
  "condition": { "all": [{ "field": "temperature_type", "operator": "=", "value": "SURGELE" }] },
  "effect": { "type": "GROUP_BY", "attribute": "temperature_type" }
}
```

### Exemple 3 : Hauteur max palette EURO

**Règle fonctionnelle** : "La palette EURO ne doit pas dépasser 1800 mm"

**Traduction** :
1. Scope : `PALLET`
2. Condition : `support.code = EURO`
3. Effet : `SET_CONSTRAINT: maxHeight = 1800 mm`
4. Sévérité : `HARD`

```json
{
  "ruleCode": "R_MAX_HEIGHT_EURO",
  "scope": "PALLET",
  "severity": "HARD",
  "condition": { "all": [{ "field": "support.code", "operator": "=", "value": "EURO" }] },
  "effect": { "type": "SET_CONSTRAINT", "constraint": "maxHeight", "value": 1800, "unit": "mm" }
}
```

### Exemple 4 : Fragile jamais sous un lourd (INTER_PACKAGE)

**Règle fonctionnelle** : "Un colis fragile ne doit jamais avoir un colis lourd au-dessus"

**Traduction** :
1. Scope : `INTER_PACKAGE`
2. Condition : packageA lourd (>= 15 kg) ET packageB fragile
3. Effet : `FORBID_ABOVE` — A ne doit pas être au-dessus de B
4. Sévérité : `HARD`

```json
{
  "ruleCode": "R_FRAGILE_NO_HEAVY_ABOVE",
  "scope": "INTER_PACKAGE",
  "severity": "HARD",
  "condition": {
    "all": [
      { "subject": "packageA", "field": "weight_kg", "operator": ">=", "value": 15 },
      { "subject": "packageB", "field": "fragility_level", "operator": "=", "value": "FRAGILE" }
    ]
  },
  "effect": { "type": "FORBID_ABOVE", "above": "packageA", "below": "packageB" }
}
```

### Exemple 5 : Préférer les palettes EURO (SOFT)

**Règle fonctionnelle** : "Quand le choix est possible, privilégier les palettes EURO"

**Traduction** :
1. Scope : `PALLET`
2. Condition : toujours (pas de condition spécifique)
3. Effet : `PREFERRED_SUPPORT: EURO`
4. Sévérité : `SOFT`, poids : 40

```json
{
  "ruleCode": "R_PREFER_EURO",
  "scope": "PALLET",
  "severity": "SOFT",
  "weight": 40,
  "condition": { "all": [] },
  "effect": { "type": "PREFERRED_SUPPORT", "support": "EURO" }
}
```

---

## 8. Intégration avec le solveur

### 8.1 Flux

```
Règles (DB)  →  Moteur de règles  →  ConstraintSet  →  Solveur  →  Plan de palettisation
                     ↑                                      ↑
              Colis enrichis                          Colis enrichis
```

### 8.2 ConstraintSet (objet produit par le moteur)

Le ConstraintSet est un objet structuré contenant toutes les contraintes techniques dérivées des règles :

```
ConstraintSet
├── classifications[]
│     └── {packageFilter, attribute, value}
├── groupings[]
│     └── {attribute, mandatory: true/false}
├── forbiddenPlacements[]
│     └── {aboveFilter, belowFilter}
├── capacityLimits[]
│     └── {supportFilter, constraint, value, unit}
├── supportRules[]
│     └── {type: ALLOWED/REQUIRED/PREFERRED, supports[]}
├── optimizationObjectives[]
│     └── {type: MINIMIZE_PALLETS/MINIMIZE_VOID/..., weight}
└── appliedRules[]
      └── {ruleCode, version, severity, explanation}
```

Le solveur consomme ce ConstraintSet sans connaître les règles d'origine.

---

## 9. Testabilité

### 9.1 Tests unitaires par règle

Chaque règle peut avoir des cas de test stockés en base (table `RULE_TEST_CASE`) :

```json
{
  "testName": "Colis 22 kg → BOTTOM",
  "input": { "weight_kg": 22.0, "temperature_type": "AMBIANT" },
  "expectedResult": "MATCH"
}
```

### 9.2 Tests de non-régression

Quand une nouvelle version de règle est publiée, les tests de la version précédente sont rejoués pour détecter les régressions.

### 9.3 Validation à la publication

Avant de publier une version de règle, le moteur vérifie :
- syntaxe JSON valide ;
- champs de condition reconnus ;
- type d'effet reconnu ;
- pas de conflit avec d'autres règles HARD actives (si détectable).
