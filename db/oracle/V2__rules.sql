-- ============================================================================
-- MBPal — Script V2 : Tables du moteur de règles
-- Base : Oracle
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table : RULE
-- Description : Définition d'une règle métier de palettisation
-- ----------------------------------------------------------------------------
CREATE TABLE RULE (
    rule_id         NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_code       VARCHAR2(50)    NOT NULL,
    domain          VARCHAR2(50)    DEFAULT 'PALLETIZATION' NOT NULL,
    scope           VARCHAR2(20)    NOT NULL,
    severity        VARCHAR2(10)    NOT NULL,
    description     VARCHAR2(500)   NOT NULL,
    active_flag     CHAR(1)         DEFAULT 'Y' NOT NULL,
    created_at      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_rule_code UNIQUE (rule_code),
    CONSTRAINT ck_rule_scope CHECK (scope IN ('PACKAGE', 'PALLET', 'INTER_PACKAGE')),
    CONSTRAINT ck_rule_severity CHECK (severity IN ('HARD', 'SOFT')),
    CONSTRAINT ck_rule_active CHECK (active_flag IN ('Y', 'N'))
);

CREATE INDEX idx_rule_code ON RULE(rule_code);
CREATE INDEX idx_rule_scope ON RULE(scope);
CREATE INDEX idx_rule_severity ON RULE(severity);
CREATE INDEX idx_rule_active ON RULE(active_flag);

COMMENT ON TABLE RULE IS 'Définition des règles métier de palettisation';
COMMENT ON COLUMN RULE.rule_code IS 'Code unique de la règle (ex: R_HEAVY_BOTTOM)';
COMMENT ON COLUMN RULE.domain IS 'Domaine fonctionnel de la règle';
COMMENT ON COLUMN RULE.scope IS 'Portée : PACKAGE (colis), PALLET (palette), INTER_PACKAGE (entre colis)';
COMMENT ON COLUMN RULE.severity IS 'Sévérité : HARD (jamais violable) ou SOFT (préférentielle)';

-- ----------------------------------------------------------------------------
-- Table : RULE_VERSION
-- Description : Version d'une règle avec condition et effet en JSON
-- ----------------------------------------------------------------------------
CREATE TABLE RULE_VERSION (
    rule_version_id     NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_id             NUMBER(12)      NOT NULL,
    semantic_version    VARCHAR2(20)    NOT NULL,
    condition_json      CLOB            NOT NULL,
    effect_json         CLOB            NOT NULL,
    explanation         VARCHAR2(1000)  NOT NULL,
    valid_from          TIMESTAMP,
    valid_to            TIMESTAMP,
    status              VARCHAR2(20)    DEFAULT 'DRAFT' NOT NULL,
    published_by        VARCHAR2(100),
    published_at        TIMESTAMP,
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_ruleversion_rule FOREIGN KEY (rule_id) REFERENCES RULE(rule_id),
    CONSTRAINT ck_ruleversion_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uk_rule_version UNIQUE (rule_id, semantic_version),
    CONSTRAINT ck_ruleversion_json CHECK (condition_json IS JSON),
    CONSTRAINT ck_ruleversion_effect CHECK (effect_json IS JSON)
);

CREATE INDEX idx_ruleversion_rule ON RULE_VERSION(rule_id);
CREATE INDEX idx_ruleversion_status ON RULE_VERSION(status);
CREATE INDEX idx_ruleversion_valid ON RULE_VERSION(valid_from, valid_to);

COMMENT ON TABLE RULE_VERSION IS 'Versions des règles avec conditions et effets en JSON';
COMMENT ON COLUMN RULE_VERSION.condition_json IS 'Expression de condition en JSON (DSL)';
COMMENT ON COLUMN RULE_VERSION.effect_json IS 'Effet de la règle en JSON (DSL)';
COMMENT ON COLUMN RULE_VERSION.semantic_version IS 'Version sémantique (ex: 1.0, 1.1, 2.0)';
COMMENT ON COLUMN RULE_VERSION.status IS 'Statut : DRAFT, ACTIVE, ARCHIVED';

-- ----------------------------------------------------------------------------
-- Table : RULESET
-- Description : Ensemble nommé de versions de règles actives
-- ----------------------------------------------------------------------------
CREATE TABLE RULESET (
    ruleset_id      NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_code    VARCHAR2(50)    NOT NULL,
    label           VARCHAR2(200)   NOT NULL,
    description     VARCHAR2(1000),
    status          VARCHAR2(20)    DEFAULT 'DRAFT' NOT NULL,
    created_at      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    published_at    TIMESTAMP,
    archived_at     TIMESTAMP,
    CONSTRAINT uk_ruleset_code UNIQUE (ruleset_code),
    CONSTRAINT ck_ruleset_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_ruleset_status ON RULESET(status);

COMMENT ON TABLE RULESET IS 'Ensemble nommé de versions de règles constituant une configuration de palettisation';
COMMENT ON COLUMN RULESET.ruleset_code IS 'Code unique du ruleset (ex: RULESET-2026-04-V1)';

-- ----------------------------------------------------------------------------
-- Table : RULESET_RULE
-- Description : Association ruleset ↔ version de règle (many-to-many)
-- ----------------------------------------------------------------------------
CREATE TABLE RULESET_RULE (
    ruleset_rule_id     NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id          NUMBER(12)      NOT NULL,
    rule_version_id     NUMBER(12)      NOT NULL,
    CONSTRAINT fk_rsr_ruleset FOREIGN KEY (ruleset_id) REFERENCES RULESET(ruleset_id),
    CONSTRAINT fk_rsr_ruleversion FOREIGN KEY (rule_version_id) REFERENCES RULE_VERSION(rule_version_id),
    CONSTRAINT uk_ruleset_ruleversion UNIQUE (ruleset_id, rule_version_id)
);

CREATE INDEX idx_rsr_ruleset ON RULESET_RULE(ruleset_id);
CREATE INDEX idx_rsr_ruleversion ON RULESET_RULE(rule_version_id);

COMMENT ON TABLE RULESET_RULE IS 'Association entre un ruleset et ses versions de règles';

-- ----------------------------------------------------------------------------
-- Table : RULE_PRIORITY
-- Description : Priorisation et pondération des règles SOFT dans un ruleset
-- ----------------------------------------------------------------------------
CREATE TABLE RULE_PRIORITY (
    rule_priority_id    NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ruleset_id          NUMBER(12)      NOT NULL,
    rule_id             NUMBER(12)      NOT NULL,
    priority_order      NUMBER(4)       NOT NULL,
    weight              NUMBER(5,2)     DEFAULT 50.00 NOT NULL,
    CONSTRAINT fk_rprio_ruleset FOREIGN KEY (ruleset_id) REFERENCES RULESET(ruleset_id),
    CONSTRAINT fk_rprio_rule FOREIGN KEY (rule_id) REFERENCES RULE(rule_id),
    CONSTRAINT uk_ruleset_rule_prio UNIQUE (ruleset_id, rule_id),
    CONSTRAINT ck_rprio_weight CHECK (weight >= 0 AND weight <= 100),
    CONSTRAINT ck_rprio_order CHECK (priority_order > 0)
);

CREATE INDEX idx_rprio_ruleset ON RULE_PRIORITY(ruleset_id);

COMMENT ON TABLE RULE_PRIORITY IS 'Tableau de priorisation des règles SOFT au sein d''un ruleset';
COMMENT ON COLUMN RULE_PRIORITY.priority_order IS 'Ordre de priorité (1 = plus prioritaire)';
COMMENT ON COLUMN RULE_PRIORITY.weight IS 'Poids de la règle dans le scoring (0 à 100)';

-- ----------------------------------------------------------------------------
-- Table : RULE_TEST_CASE
-- Description : Cas de test unitaire associé à une règle
-- ----------------------------------------------------------------------------
CREATE TABLE RULE_TEST_CASE (
    test_case_id        NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    rule_id             NUMBER(12)      NOT NULL,
    test_name           VARCHAR2(200)   NOT NULL,
    input_json          CLOB            NOT NULL,
    expected_result     VARCHAR2(20)    NOT NULL,
    description         VARCHAR2(500),
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_rtc_rule FOREIGN KEY (rule_id) REFERENCES RULE(rule_id),
    CONSTRAINT ck_rtc_result CHECK (expected_result IN ('MATCH', 'NO_MATCH')),
    CONSTRAINT ck_rtc_input CHECK (input_json IS JSON)
);

CREATE INDEX idx_rtc_rule ON RULE_TEST_CASE(rule_id);

COMMENT ON TABLE RULE_TEST_CASE IS 'Cas de test unitaire pour validation des règles';
