# Diagramme du modèle de données

## Modèle entité-relation

```mermaid
erDiagram
    PRODUCT {
        NUMBER product_id PK
        VARCHAR2 product_code UK
        VARCHAR2 label
        VARCHAR2 temperature_type
        NUMBER length_mm
        NUMBER width_mm
        NUMBER height_mm
        NUMBER weight_kg
        VARCHAR2 fragility_level
        CHAR stackable_flag
    }

    SUPPORT_TYPE {
        NUMBER support_type_id PK
        VARCHAR2 support_code UK
        VARCHAR2 label
        NUMBER length_mm
        NUMBER width_mm
        NUMBER height_mm
        NUMBER max_load_kg
        NUMBER max_total_height_mm
        CHAR mergeable_flag
        VARCHAR2 merge_target_code
    }

    CUSTOMER_ORDER {
        NUMBER order_id PK
        VARCHAR2 external_order_id UK
        VARCHAR2 customer_id
        VARCHAR2 customer_name
        VARCHAR2 warehouse_code
        DATE order_date
        VARCHAR2 status
    }

    ORDER_LINE {
        NUMBER order_line_id PK
        NUMBER order_id FK
        NUMBER product_id FK
        NUMBER box_quantity
        NUMBER line_number
    }

    RULE {
        NUMBER rule_id PK
        VARCHAR2 rule_code UK
        VARCHAR2 domain
        VARCHAR2 scope
        VARCHAR2 severity
        VARCHAR2 description
    }

    RULE_VERSION {
        NUMBER rule_version_id PK
        NUMBER rule_id FK
        VARCHAR2 semantic_version
        CLOB condition_json
        CLOB effect_json
        VARCHAR2 explanation
        VARCHAR2 status
    }

    RULESET {
        NUMBER ruleset_id PK
        VARCHAR2 ruleset_code UK
        VARCHAR2 label
        VARCHAR2 status
    }

    RULESET_RULE {
        NUMBER ruleset_rule_id PK
        NUMBER ruleset_id FK
        NUMBER rule_version_id FK
    }

    RULE_PRIORITY {
        NUMBER rule_priority_id PK
        NUMBER ruleset_id FK
        NUMBER rule_id FK
        NUMBER priority_order
        NUMBER weight
    }

    PALLETIZATION_EXECUTION {
        NUMBER execution_id PK
        VARCHAR2 execution_code UK
        NUMBER order_id FK
        NUMBER ruleset_id FK
        VARCHAR2 status
        NUMBER total_pallets
        NUMBER global_score
        NUMBER duration_ms
    }

    PALLET {
        NUMBER pallet_id PK
        NUMBER execution_id FK
        NUMBER support_type_id FK
        NUMBER pallet_number
        NUMBER total_weight_kg
        NUMBER total_height_mm
        NUMBER fill_rate_pct
        NUMBER stability_score
    }

    PALLET_ITEM {
        NUMBER pallet_item_id PK
        NUMBER pallet_id FK
        NUMBER product_id FK
        NUMBER order_line_id FK
        NUMBER box_instance_index
        NUMBER layer_no
        NUMBER position_no
        VARCHAR2 stacking_class
    }

    CONSTRAINT_VIOLATION {
        NUMBER violation_id PK
        NUMBER execution_id FK
        NUMBER pallet_id FK
        NUMBER rule_version_id FK
        VARCHAR2 severity
        VARCHAR2 description
    }

    DECISION_TRACE {
        NUMBER trace_id PK
        NUMBER execution_id FK
        VARCHAR2 step_name
        NUMBER pallet_id FK
        NUMBER rule_version_id FK
        CLOB description
    }

    CUSTOMER_ORDER ||--o{ ORDER_LINE : "contient"
    PRODUCT ||--o{ ORDER_LINE : "référence"
    CUSTOMER_ORDER ||--o{ PALLETIZATION_EXECUTION : "palettisée par"
    RULESET ||--o{ PALLETIZATION_EXECUTION : "utilise"
    PALLETIZATION_EXECUTION ||--o{ PALLET : "génère"
    SUPPORT_TYPE ||--o{ PALLET : "type de support"
    PALLET ||--o{ PALLET_ITEM : "contient"
    PRODUCT ||--o{ PALLET_ITEM : "produit"
    ORDER_LINE ||--o{ PALLET_ITEM : "ligne source"
    RULE ||--o{ RULE_VERSION : "versions"
    RULESET ||--o{ RULESET_RULE : "inclut"
    RULE_VERSION ||--o{ RULESET_RULE : "version incluse"
    RULESET ||--o{ RULE_PRIORITY : "priorise"
    RULE ||--o{ RULE_PRIORITY : "règle priorisée"
    PALLETIZATION_EXECUTION ||--o{ CONSTRAINT_VIOLATION : "violations"
    RULE_VERSION ||--o{ CONSTRAINT_VIOLATION : "règle violée"
    PALLETIZATION_EXECUTION ||--o{ DECISION_TRACE : "traces"
```

## Groupes d'entités

| Groupe | Tables | Description |
|--------|--------|-------------|
| **Référentiel** | PRODUCT, SUPPORT_TYPE | Données de référence stables |
| **Commande** | CUSTOMER_ORDER, ORDER_LINE | Commandes client soumises |
| **Règles** | RULE, RULE_VERSION, RULESET, RULESET_RULE, RULE_PRIORITY | Moteur de règles versionné |
| **Exécution** | PALLETIZATION_EXECUTION, PALLET, PALLET_ITEM, CONSTRAINT_VIOLATION, DECISION_TRACE | Résultats de palettisation |
| **Audit** | API_REQUEST_LOG, EXECUTION_METRIC | Journalisation et métriques |
