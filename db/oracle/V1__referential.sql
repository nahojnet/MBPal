-- ============================================================================
-- MBPal — Script V1 : Tables référentielles
-- Base : Oracle
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table : PRODUCT
-- Description : Référentiel des produits avec leurs caractéristiques de colis
-- ----------------------------------------------------------------------------
CREATE TABLE PRODUCT (
    product_id          NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_code        VARCHAR2(50)    NOT NULL,
    label               VARCHAR2(200)   NOT NULL,
    temperature_type    VARCHAR2(20)    NOT NULL,
    length_mm           NUMBER(8)       NOT NULL,
    width_mm            NUMBER(8)       NOT NULL,
    height_mm           NUMBER(8)       NOT NULL,
    weight_kg           NUMBER(8,2)     NOT NULL,
    fragility_level     VARCHAR2(20)    DEFAULT 'ROBUSTE' NOT NULL,
    stackable_flag      CHAR(1)         DEFAULT 'Y' NOT NULL,
    max_stack_weight_kg NUMBER(8,2),
    orientation_fixed   CHAR(1)         DEFAULT 'N' NOT NULL,
    active_flag         CHAR(1)         DEFAULT 'Y' NOT NULL,
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_product UNIQUE (product_code),
    CONSTRAINT ck_product_temp CHECK (temperature_type IN ('AMBIANT', 'FRAIS', 'SURGELE')),
    CONSTRAINT ck_product_fragility CHECK (fragility_level IN ('FRAGILE', 'ROBUSTE')),
    CONSTRAINT ck_product_stackable CHECK (stackable_flag IN ('Y', 'N')),
    CONSTRAINT ck_product_orientation CHECK (orientation_fixed IN ('Y', 'N')),
    CONSTRAINT ck_product_active CHECK (active_flag IN ('Y', 'N')),
    CONSTRAINT ck_product_dimensions CHECK (length_mm > 0 AND width_mm > 0 AND height_mm > 0),
    CONSTRAINT ck_product_weight CHECK (weight_kg > 0)
);

CREATE INDEX idx_product_code ON PRODUCT(product_code);
CREATE INDEX idx_product_temp ON PRODUCT(temperature_type);
CREATE INDEX idx_product_active ON PRODUCT(active_flag);

COMMENT ON TABLE PRODUCT IS 'Référentiel des produits avec caractéristiques du colis';
COMMENT ON COLUMN PRODUCT.product_code IS 'Code unique du produit';
COMMENT ON COLUMN PRODUCT.temperature_type IS 'Famille de température : AMBIANT, FRAIS, SURGELE';
COMMENT ON COLUMN PRODUCT.length_mm IS 'Longueur du colis en millimètres';
COMMENT ON COLUMN PRODUCT.width_mm IS 'Largeur du colis en millimètres';
COMMENT ON COLUMN PRODUCT.height_mm IS 'Hauteur du colis en millimètres';
COMMENT ON COLUMN PRODUCT.weight_kg IS 'Poids du colis en kilogrammes';
COMMENT ON COLUMN PRODUCT.fragility_level IS 'Niveau de fragilité : FRAGILE, ROBUSTE';
COMMENT ON COLUMN PRODUCT.stackable_flag IS 'Colis empilable (Y) ou non (N)';
COMMENT ON COLUMN PRODUCT.max_stack_weight_kg IS 'Poids max supportable au-dessus du colis (null = illimité)';
COMMENT ON COLUMN PRODUCT.orientation_fixed IS 'Orientation fixe obligatoire (Y/N)';

-- ----------------------------------------------------------------------------
-- Table : SUPPORT_TYPE
-- Description : Référentiel des types de support (palette, demi-palette, dolly)
-- ----------------------------------------------------------------------------
CREATE TABLE SUPPORT_TYPE (
    support_type_id         NUMBER(8)       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    support_code            VARCHAR2(30)    NOT NULL,
    label                   VARCHAR2(100)   NOT NULL,
    length_mm               NUMBER(8)       NOT NULL,
    width_mm                NUMBER(8)       NOT NULL,
    height_mm               NUMBER(8)       NOT NULL,
    max_load_kg             NUMBER(8,2)     NOT NULL,
    max_total_height_mm     NUMBER(8)       NOT NULL,
    usable_length_mm        NUMBER(8),
    usable_width_mm         NUMBER(8),
    mergeable_flag          CHAR(1)         DEFAULT 'N' NOT NULL,
    merge_target_code       VARCHAR2(30),
    merge_quantity           NUMBER(2)       DEFAULT 2,
    active_flag             CHAR(1)         DEFAULT 'Y' NOT NULL,
    created_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at              TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_support_code UNIQUE (support_code),
    CONSTRAINT ck_support_mergeable CHECK (mergeable_flag IN ('Y', 'N')),
    CONSTRAINT ck_support_active CHECK (active_flag IN ('Y', 'N')),
    CONSTRAINT ck_support_dimensions CHECK (length_mm > 0 AND width_mm > 0 AND height_mm > 0),
    CONSTRAINT ck_support_load CHECK (max_load_kg > 0),
    CONSTRAINT ck_support_height CHECK (max_total_height_mm > 0)
);

CREATE INDEX idx_support_code ON SUPPORT_TYPE(support_code);
CREATE INDEX idx_support_active ON SUPPORT_TYPE(active_flag);

COMMENT ON TABLE SUPPORT_TYPE IS 'Référentiel des types de support de palettisation';
COMMENT ON COLUMN SUPPORT_TYPE.support_code IS 'Code unique du type de support (EURO, HALF, DOLLY, etc.)';
COMMENT ON COLUMN SUPPORT_TYPE.max_load_kg IS 'Charge maximale autorisée en kilogrammes';
COMMENT ON COLUMN SUPPORT_TYPE.max_total_height_mm IS 'Hauteur maximale totale autorisée (support + colis) en mm';
COMMENT ON COLUMN SUPPORT_TYPE.usable_length_mm IS 'Surface utile longueur (si différent de length_mm)';
COMMENT ON COLUMN SUPPORT_TYPE.usable_width_mm IS 'Surface utile largeur (si différent de width_mm)';
COMMENT ON COLUMN SUPPORT_TYPE.mergeable_flag IS 'Support fusionnable (Y/N) — ex: 2 demi-palettes → 1 palette';
COMMENT ON COLUMN SUPPORT_TYPE.merge_target_code IS 'Code du support cible après fusion (ex: EURO)';
COMMENT ON COLUMN SUPPORT_TYPE.merge_quantity IS 'Nombre de supports nécessaires pour une fusion (défaut: 2)';
