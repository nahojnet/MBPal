# Spécification des APIs REST — MBPal

---

## 1. Conventions générales

| Aspect | Convention |
|--------|-----------|
| **Base URL** | `/api/v1` |
| **Format** | JSON (application/json) |
| **Authentification** | À définir (JWT / OAuth2 / API Key) |
| **Pagination** | `?page=0&size=20` (défaut: page 0, 20 éléments) |
| **Tri** | `?sort=field,direction` (ex: `?sort=createdAt,desc`) |
| **Erreurs** | Format standard (voir section 7) |

---

## 2. API de palettisation

### 2.1 Soumettre une palettisation

**`POST /api/v1/palletizations`**

Soumet une commande pour palettisation asynchrone.

**Request** :
```json
{
  "externalOrderId": "CMD-2026-000123",
  "customerId": "C001",
  "customerName": "Client ABC",
  "warehouseCode": "WH01",
  "supportPolicy": {
    "allowedSupports": ["EURO", "HALF", "DOLLY"]
  },
  "rulesetCode": "RULESET-2026-04-V1",
  "lines": [
    {
      "productCode": "PROD-001",
      "boxQuantity": 5
    },
    {
      "productCode": "PROD-003",
      "boxQuantity": 3
    }
  ]
}
```

**Response 202 Accepted** :
```json
{
  "executionId": "PAL-EXEC-000987",
  "status": "PENDING",
  "createdAt": "2026-04-02T10:30:00Z"
}
```

| Statut | Description |
|--------|-------------|
| 202 | Commande acceptée, traitement en cours |
| 400 | Erreur de validation (produit inconnu, quantité invalide...) |
| 409 | Commande déjà soumise (external_order_id en doublon) |

---

### 2.2 Consulter le résultat

**`GET /api/v1/palletizations/{executionId}`**

**Response 200** (status = COMPLETED) :
```json
{
  "executionId": "PAL-EXEC-000987",
  "status": "COMPLETED",
  "externalOrderId": "CMD-2026-000123",
  "customerId": "C001",
  "rulesetCode": "RULESET-2026-04-V1",
  "startedAt": "2026-04-02T10:30:01Z",
  "endedAt": "2026-04-02T10:30:02Z",
  "durationMs": 1234,
  "totalPallets": 2,
  "totalBoxes": 8,
  "globalScore": 87.5,
  "pallets": [
    {
      "palletNumber": 1,
      "supportType": "EURO",
      "totalWeightKg": 110.0,
      "totalHeightMm": 1650,
      "fillRatePct": 72.5,
      "stabilityScore": 90.0,
      "layerCount": 3,
      "boxCount": 5,
      "items": [
        {
          "productCode": "PROD-001",
          "boxInstanceIndex": 1,
          "layerNo": 1,
          "positionNo": 1,
          "stackingClass": "BOTTOM"
        },
        {
          "productCode": "PROD-001",
          "boxInstanceIndex": 2,
          "layerNo": 1,
          "positionNo": 2,
          "stackingClass": "BOTTOM"
        }
      ]
    },
    {
      "palletNumber": 2,
      "supportType": "EURO",
      "totalWeightKg": 36.0,
      "totalHeightMm": 600,
      "fillRatePct": 45.0,
      "stabilityScore": 85.0,
      "layerCount": 2,
      "boxCount": 3,
      "items": [
        {
          "productCode": "PROD-003",
          "boxInstanceIndex": 1,
          "layerNo": 1,
          "positionNo": 1,
          "stackingClass": "MIDDLE"
        }
      ]
    }
  ],
  "violations": []
}
```

**Response 200** (status = PENDING ou PROCESSING) :
```json
{
  "executionId": "PAL-EXEC-000987",
  "status": "PROCESSING",
  "externalOrderId": "CMD-2026-000123",
  "createdAt": "2026-04-02T10:30:00Z"
}
```

**Response 200** (status = ERROR) :
```json
{
  "executionId": "PAL-EXEC-000987",
  "status": "ERROR",
  "externalOrderId": "CMD-2026-000123",
  "errorMessage": "Le colis PROD-999 dépasse la capacité de tous les supports autorisés"
}
```

| Statut | Description |
|--------|-------------|
| 200 | Exécution trouvée (quel que soit son status) |
| 404 | Exécution inconnue |

---

### 2.3 Consulter les explications

**`GET /api/v1/palletizations/{executionId}/explanations`**

**Response 200** :
```json
{
  "executionId": "PAL-EXEC-000987",
  "rulesetCode": "RULESET-2026-04-V1",
  "appliedRules": [
    {
      "ruleCode": "R_HEAVY_BOTTOM",
      "version": "1.0",
      "severity": "HARD",
      "explanation": "Les colis >= 15 kg sont classés BOTTOM",
      "matchedBoxes": 5
    },
    {
      "ruleCode": "R_TEMP_SEPARATION",
      "version": "1.0",
      "severity": "HARD",
      "explanation": "Séparation par température",
      "matchedBoxes": 8
    }
  ],
  "violations": [],
  "decisionTrace": [
    {
      "traceOrder": 1,
      "stepName": "NORMALIZATION",
      "description": "8 colis instanciés à partir de 2 lignes de commande"
    },
    {
      "traceOrder": 2,
      "stepName": "GROUPING",
      "description": "2 groupes créés : AMBIANT (5 colis), SURGELE (3 colis)"
    },
    {
      "traceOrder": 3,
      "stepName": "ASSIGNMENT",
      "description": "Groupe AMBIANT → PAL-1 (EURO), Groupe SURGELE → PAL-2 (EURO)"
    },
    {
      "traceOrder": 4,
      "stepName": "LAYERING",
      "description": "PAL-1 : couche 1 = PROD-001 (BOTTOM), couche 2-3 = PROD-001 (MIDDLE)"
    },
    {
      "traceOrder": 5,
      "stepName": "VALIDATION",
      "description": "Toutes les contraintes HARD satisfaites. Score global : 87.5"
    }
  ]
}
```

---

### 2.4 Rechercher des exécutions

**`GET /api/v1/palletizations`**

| Paramètre | Type | Description |
|-----------|------|-------------|
| `orderId` | string | Filtrer par ID commande externe |
| `customerId` | string | Filtrer par client |
| `status` | string | Filtrer par statut |
| `dateFrom` | ISO date | Date de début |
| `dateTo` | ISO date | Date de fin |
| `page` | int | Page (défaut: 0) |
| `size` | int | Taille de page (défaut: 20) |

**Response 200** :
```json
{
  "content": [
    {
      "executionId": "PAL-EXEC-000987",
      "externalOrderId": "CMD-2026-000123",
      "customerId": "C001",
      "status": "COMPLETED",
      "totalPallets": 2,
      "globalScore": 87.5,
      "startedAt": "2026-04-02T10:30:01Z",
      "durationMs": 1234
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 2.5 Comparer deux exécutions

**`POST /api/v1/palletizations/compare`**

**Request** :
```json
{
  "executionId1": "PAL-EXEC-000987",
  "executionId2": "PAL-EXEC-000988"
}
```

**Response 200** :
```json
{
  "execution1": {
    "executionId": "PAL-EXEC-000987",
    "rulesetCode": "RULESET-2026-04-V1",
    "totalPallets": 2,
    "globalScore": 87.5
  },
  "execution2": {
    "executionId": "PAL-EXEC-000988",
    "rulesetCode": "RULESET-2026-04-V2",
    "totalPallets": 3,
    "globalScore": 72.0
  },
  "differences": {
    "palletCountDiff": 1,
    "scoreDiff": -15.5,
    "boxMoves": [
      {
        "productCode": "PROD-003",
        "boxInstanceIndex": 1,
        "fromPallet": 1,
        "toPallet": 3
      }
    ]
  }
}
```

---

### 2.6 Simuler une palettisation (dry run)

**`POST /api/v1/palletizations/simulate`**

**Request** :
```json
{
  "externalOrderId": "CMD-2026-000123",
  "rulesetCode": "RULESET-2026-04-V2",
  "supportPolicy": {
    "allowedSupports": ["EURO", "HALF"]
  }
}
```

Le comportement est identique à une palettisation normale, mais :
- le résultat est marqué `dry_run_flag = Y`
- il peut être optionnellement non persisté selon le paramètre

**Response 202** : même format que POST /palletizations.

---

## 3. API de référentiel produit

### 3.1 Lister les produits

**`GET /api/v1/products`**

Paramètres : `?temperatureType=SURGELE&active=true&page=0&size=20`

**Response 200** :
```json
{
  "content": [
    {
      "productId": 1,
      "productCode": "PROD-001",
      "label": "Carton lourd ambiant",
      "temperatureType": "AMBIANT",
      "lengthMm": 400,
      "widthMm": 300,
      "heightMm": 250,
      "weightKg": 22.0,
      "fragilityLevel": "ROBUSTE",
      "stackableFlag": true,
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1
}
```

### 3.2 Détail d'un produit

**`GET /api/v1/products/{productCode}`**

### 3.3 Créer un produit

**`POST /api/v1/products`**

**Request** :
```json
{
  "productCode": "PROD-009",
  "label": "Nouveau produit",
  "temperatureType": "FRAIS",
  "lengthMm": 350,
  "widthMm": 250,
  "heightMm": 200,
  "weightKg": 8.5,
  "fragilityLevel": "ROBUSTE",
  "stackableFlag": true
}
```

**Response 201** : produit créé avec ID.

### 3.4 Modifier un produit

**`PUT /api/v1/products/{productCode}`**

Même format que POST.

**Response 200** : produit mis à jour.

---

## 4. API de référentiel support

### 4.1 Lister les supports

**`GET /api/v1/supports`**

### 4.2 Détail d'un support

**`GET /api/v1/supports/{supportCode}`**

### 4.3 Créer un support

**`POST /api/v1/supports`**

**Request** :
```json
{
  "supportCode": "CUSTOM-01",
  "label": "Support personnalisé",
  "lengthMm": 1000,
  "widthMm": 600,
  "heightMm": 150,
  "maxLoadKg": 500.0,
  "maxTotalHeightMm": 1600,
  "mergeableFlag": false
}
```

### 4.4 Modifier un support

**`PUT /api/v1/supports/{supportCode}`**

---

## 5. API de gestion des règles

### 5.1 Lister les règles

**`GET /api/v1/rules`**

Paramètres : `?scope=PACKAGE&severity=HARD&active=true`

**Response 200** :
```json
{
  "content": [
    {
      "ruleCode": "R_HEAVY_BOTTOM",
      "domain": "PALLETIZATION",
      "scope": "PACKAGE",
      "severity": "HARD",
      "description": "Les colis lourds doivent être en couche basse",
      "active": true,
      "latestVersion": "1.0",
      "latestVersionStatus": "ACTIVE"
    }
  ]
}
```

### 5.2 Détail d'une règle avec ses versions

**`GET /api/v1/rules/{ruleCode}`**

**Response 200** :
```json
{
  "ruleCode": "R_HEAVY_BOTTOM",
  "domain": "PALLETIZATION",
  "scope": "PACKAGE",
  "severity": "HARD",
  "description": "Les colis lourds doivent être en couche basse",
  "active": true,
  "versions": [
    {
      "ruleVersionId": 1,
      "semanticVersion": "1.0",
      "conditionJson": {"all": [{"field": "weight_kg", "operator": ">=", "value": 15}]},
      "effectJson": {"type": "SET_ATTRIBUTE", "attribute": "stackingClass", "value": "BOTTOM"},
      "explanation": "Les colis >= 15 kg sont classés BOTTOM",
      "status": "ACTIVE",
      "publishedAt": "2026-04-01T08:00:00Z"
    }
  ]
}
```

### 5.3 Créer une règle

**`POST /api/v1/rules`**

**Request** :
```json
{
  "ruleCode": "R_NEW_RULE",
  "scope": "PACKAGE",
  "severity": "SOFT",
  "description": "Description de la nouvelle règle",
  "version": {
    "conditionJson": {
      "all": [{"field": "weight_kg", "operator": "<=", "value": 5}]
    },
    "effectJson": {
      "type": "SET_ATTRIBUTE",
      "attribute": "stackingClass",
      "value": "TOP"
    },
    "explanation": "Les colis <= 5 kg sont classés TOP"
  }
}
```

**Response 201** : règle créée avec version 1.0 en DRAFT.

### 5.4 Publier une version de règle

**`POST /api/v1/rules/{ruleCode}/versions/{versionId}/publish`**

**Response 200** : version passée en ACTIVE.

### 5.5 Valider une définition de règle (sans sauvegarder)

**`POST /api/v1/rules/validate`**

Vérifie la syntaxe de la condition et de l'effet sans persister.

**Response 200** :
```json
{
  "valid": true,
  "warnings": []
}
```

**Response 200** (avec erreurs) :
```json
{
  "valid": false,
  "errors": [
    "Champ inconnu 'poids' dans la condition. Vouliez-vous dire 'weight_kg' ?"
  ]
}
```

---

## 6. API de gestion des rulesets

### 6.1 Lister les rulesets

**`GET /api/v1/rulesets`**

### 6.2 Détail d'un ruleset

**`GET /api/v1/rulesets/{rulesetCode}`**

**Response 200** :
```json
{
  "rulesetCode": "RULESET-2026-04-V1",
  "label": "Ruleset Avril 2026 V1",
  "status": "ACTIVE",
  "publishedAt": "2026-04-01T08:00:00Z",
  "rules": [
    {
      "ruleCode": "R_HEAVY_BOTTOM",
      "semanticVersion": "1.0",
      "severity": "HARD"
    },
    {
      "ruleCode": "R_MINIMIZE_PALLETS",
      "semanticVersion": "1.0",
      "severity": "SOFT"
    }
  ]
}
```

### 6.3 Créer un ruleset

**`POST /api/v1/rulesets`**

### 6.4 Publier un ruleset

**`POST /api/v1/rulesets/{rulesetCode}/publish`**

### 6.5 Consulter les priorités SOFT

**`GET /api/v1/rulesets/{rulesetCode}/priorities`**

**Response 200** :
```json
{
  "rulesetCode": "RULESET-2026-04-V1",
  "priorities": [
    { "ruleCode": "R_MINIMIZE_PALLETS", "priorityOrder": 1, "weight": 100.0 },
    { "ruleCode": "R_MERGE_HALF_PALLETS", "priorityOrder": 2, "weight": 70.0 },
    { "ruleCode": "R_LIGHT_TOP", "priorityOrder": 3, "weight": 60.0 },
    { "ruleCode": "R_PREFER_EURO", "priorityOrder": 4, "weight": 40.0 }
  ]
}
```

### 6.6 Mettre à jour les priorités SOFT

**`PUT /api/v1/rulesets/{rulesetCode}/priorities`**

**Request** :
```json
{
  "priorities": [
    { "ruleCode": "R_MINIMIZE_PALLETS", "priorityOrder": 1, "weight": 100.0 },
    { "ruleCode": "R_LIGHT_TOP", "priorityOrder": 2, "weight": 80.0 },
    { "ruleCode": "R_MERGE_HALF_PALLETS", "priorityOrder": 3, "weight": 50.0 },
    { "ruleCode": "R_PREFER_EURO", "priorityOrder": 4, "weight": 30.0 }
  ]
}
```

---

## 7. Format d'erreur standard

Toutes les erreurs suivent ce format :

```json
{
  "error": "VALIDATION_ERROR",
  "message": "Le produit PROD-999 n'existe pas dans le référentiel",
  "timestamp": "2026-04-02T10:30:00Z",
  "path": "/api/v1/palletizations",
  "details": [
    "Ligne 2 : productCode 'PROD-999' inconnu"
  ]
}
```

| Code erreur | HTTP | Description |
|-------------|------|-------------|
| `VALIDATION_ERROR` | 400 | Données d'entrée invalides |
| `NOT_FOUND` | 404 | Ressource introuvable |
| `CONFLICT` | 409 | Conflit (doublon, état invalide) |
| `RULE_SYNTAX_ERROR` | 400 | Syntaxe de règle invalide |
| `PALLETIZATION_ERROR` | 500 | Erreur interne lors du calcul |

---

## 8. Récapitulatif des endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/palletizations` | Soumettre une palettisation |
| GET | `/api/v1/palletizations/{executionId}` | Résultat d'une exécution |
| GET | `/api/v1/palletizations/{executionId}/explanations` | Explications détaillées |
| GET | `/api/v1/palletizations` | Rechercher des exécutions |
| POST | `/api/v1/palletizations/compare` | Comparer deux exécutions |
| POST | `/api/v1/palletizations/simulate` | Simulation (dry run) |
| GET | `/api/v1/products` | Lister les produits |
| GET | `/api/v1/products/{productCode}` | Détail d'un produit |
| POST | `/api/v1/products` | Créer un produit |
| PUT | `/api/v1/products/{productCode}` | Modifier un produit |
| GET | `/api/v1/supports` | Lister les supports |
| GET | `/api/v1/supports/{supportCode}` | Détail d'un support |
| POST | `/api/v1/supports` | Créer un support |
| PUT | `/api/v1/supports/{supportCode}` | Modifier un support |
| GET | `/api/v1/rules` | Lister les règles |
| GET | `/api/v1/rules/{ruleCode}` | Détail d'une règle |
| POST | `/api/v1/rules` | Créer une règle |
| POST | `/api/v1/rules/{ruleCode}/versions/{versionId}/publish` | Publier une version |
| POST | `/api/v1/rules/validate` | Valider une règle |
| GET | `/api/v1/rulesets` | Lister les rulesets |
| GET | `/api/v1/rulesets/{rulesetCode}` | Détail d'un ruleset |
| POST | `/api/v1/rulesets` | Créer un ruleset |
| POST | `/api/v1/rulesets/{rulesetCode}/publish` | Publier un ruleset |
| GET | `/api/v1/rulesets/{rulesetCode}/priorities` | Priorités SOFT |
| PUT | `/api/v1/rulesets/{rulesetCode}/priorities` | Modifier les priorités |
