-- ============================================================================
-- MBPal — Script V3 : Tables de commande et d'exécution
-- Base : Oracle
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Table : CUSTOMER_ORDER
-- Description : Commande client soumise à palettisation
-- ----------------------------------------------------------------------------
CREATE TABLE CUSTOMER_ORDER (
    order_id            NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_order_id   VARCHAR2(50)    NOT NULL,
    customer_id         VARCHAR2(50)    NOT NULL,
    customer_name       VARCHAR2(200),
    warehouse_code      VARCHAR2(20),
    order_date          DATE,
    received_at         TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    status              VARCHAR2(20)    DEFAULT 'RECEIVED' NOT NULL,
    CONSTRAINT uk_external_order UNIQUE (external_order_id),
    CONSTRAINT ck_order_status CHECK (status IN ('RECEIVED', 'PROCESSING', 'COMPLETED', 'ERROR'))
);

CREATE INDEX idx_order_external ON CUSTOMER_ORDER(external_order_id);
CREATE INDEX idx_order_customer ON CUSTOMER_ORDER(customer_id);
CREATE INDEX idx_order_date ON CUSTOMER_ORDER(order_date);
CREATE INDEX idx_order_received ON CUSTOMER_ORDER(received_at);
CREATE INDEX idx_order_status ON CUSTOMER_ORDER(status);

COMMENT ON TABLE CUSTOMER_ORDER IS 'Commande client soumise à palettisation';
COMMENT ON COLUMN CUSTOMER_ORDER.external_order_id IS 'Identifiant de la commande dans le système source (WMS/ERP)';

-- ----------------------------------------------------------------------------
-- Table : ORDER_LINE
-- Description : Ligne de commande (produit + quantité de colis)
-- ----------------------------------------------------------------------------
CREATE TABLE ORDER_LINE (
    order_line_id       NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id            NUMBER(12)      NOT NULL,
    product_id          NUMBER(12)      NOT NULL,
    box_quantity        NUMBER(6)       NOT NULL,
    line_number         NUMBER(4)       NOT NULL,
    CONSTRAINT fk_ol_order FOREIGN KEY (order_id) REFERENCES CUSTOMER_ORDER(order_id),
    CONSTRAINT fk_ol_product FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id),
    CONSTRAINT ck_ol_quantity CHECK (box_quantity > 0),
    CONSTRAINT ck_ol_line CHECK (line_number > 0),
    CONSTRAINT uk_order_line UNIQUE (order_id, line_number)
);

CREATE INDEX idx_ol_order ON ORDER_LINE(order_id);
CREATE INDEX idx_ol_product ON ORDER_LINE(product_id);

COMMENT ON TABLE ORDER_LINE IS 'Ligne de commande : produit et nombre de colis commandés';
COMMENT ON COLUMN ORDER_LINE.box_quantity IS 'Nombre de colis pour ce produit (quantité = nombre de colis)';

-- ----------------------------------------------------------------------------
-- Table : PALLETIZATION_EXECUTION
-- Description : Exécution d'un calcul de palettisation
-- Partitionnement par mois sur started_at recommandé (~1000/jour)
-- ----------------------------------------------------------------------------
CREATE TABLE PALLETIZATION_EXECUTION (
    execution_id        NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_code      VARCHAR2(50)    NOT NULL,
    order_id            NUMBER(12)      NOT NULL,
    ruleset_id          NUMBER(12)      NOT NULL,
    status              VARCHAR2(20)    DEFAULT 'PENDING' NOT NULL,
    dry_run_flag        CHAR(1)         DEFAULT 'N' NOT NULL,
    started_at          TIMESTAMP,
    ended_at            TIMESTAMP,
    duration_ms         NUMBER(10),
    total_pallets       NUMBER(4),
    total_boxes         NUMBER(6),
    global_score        NUMBER(5,2),
    error_message       VARCHAR2(2000),
    execution_params    CLOB,
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_pe_order FOREIGN KEY (order_id) REFERENCES CUSTOMER_ORDER(order_id),
    CONSTRAINT fk_pe_ruleset FOREIGN KEY (ruleset_id) REFERENCES RULESET(ruleset_id),
    CONSTRAINT uk_execution_code UNIQUE (execution_code),
    CONSTRAINT ck_pe_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'ERROR')),
    CONSTRAINT ck_pe_dryrun CHECK (dry_run_flag IN ('Y', 'N'))
);

CREATE INDEX idx_pe_order ON PALLETIZATION_EXECUTION(order_id);
CREATE INDEX idx_pe_ruleset ON PALLETIZATION_EXECUTION(ruleset_id);
CREATE INDEX idx_pe_status ON PALLETIZATION_EXECUTION(status);
CREATE INDEX idx_pe_started ON PALLETIZATION_EXECUTION(started_at);
CREATE INDEX idx_pe_code ON PALLETIZATION_EXECUTION(execution_code);

COMMENT ON TABLE PALLETIZATION_EXECUTION IS 'Exécution d''un calcul de palettisation';
COMMENT ON COLUMN PALLETIZATION_EXECUTION.execution_code IS 'Code unique d''exécution (ex: PAL-EXEC-000987)';
COMMENT ON COLUMN PALLETIZATION_EXECUTION.dry_run_flag IS 'Simulation sans sauvegarde (Y/N)';
COMMENT ON COLUMN PALLETIZATION_EXECUTION.duration_ms IS 'Durée de calcul en millisecondes';
COMMENT ON COLUMN PALLETIZATION_EXECUTION.global_score IS 'Score global de qualité de la palettisation (0 à 100)';

-- ----------------------------------------------------------------------------
-- Table : PALLET
-- Description : Palette générée par une exécution
-- ----------------------------------------------------------------------------
CREATE TABLE PALLET (
    pallet_id           NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id        NUMBER(12)      NOT NULL,
    support_type_id     NUMBER(8)       NOT NULL,
    pallet_number       NUMBER(4)       NOT NULL,
    total_weight_kg     NUMBER(8,2),
    total_height_mm     NUMBER(8),
    fill_rate_pct       NUMBER(5,2),
    stability_score     NUMBER(5,2),
    layer_count         NUMBER(4),
    box_count           NUMBER(6),
    merged_flag         CHAR(1)         DEFAULT 'N' NOT NULL,
    CONSTRAINT fk_pal_execution FOREIGN KEY (execution_id) REFERENCES PALLETIZATION_EXECUTION(execution_id),
    CONSTRAINT fk_pal_support FOREIGN KEY (support_type_id) REFERENCES SUPPORT_TYPE(support_type_id),
    CONSTRAINT uk_pallet_exec_num UNIQUE (execution_id, pallet_number),
    CONSTRAINT ck_pal_merged CHECK (merged_flag IN ('Y', 'N'))
);

CREATE INDEX idx_pal_execution ON PALLET(execution_id);
CREATE INDEX idx_pal_support ON PALLET(support_type_id);

COMMENT ON TABLE PALLET IS 'Palette générée par le calcul de palettisation';
COMMENT ON COLUMN PALLET.pallet_number IS 'Numéro de la palette dans l''exécution (1, 2, 3...)';
COMMENT ON COLUMN PALLET.fill_rate_pct IS 'Taux de remplissage en pourcentage';
COMMENT ON COLUMN PALLET.stability_score IS 'Score de stabilité (0 à 100)';
COMMENT ON COLUMN PALLET.merged_flag IS 'Palette issue d''une fusion de supports (Y/N)';

-- ----------------------------------------------------------------------------
-- Table : PALLET_ITEM
-- Description : Colis affecté à une palette avec positionnement logique
-- ----------------------------------------------------------------------------
CREATE TABLE PALLET_ITEM (
    pallet_item_id      NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pallet_id           NUMBER(12)      NOT NULL,
    product_id          NUMBER(12)      NOT NULL,
    order_line_id       NUMBER(12)      NOT NULL,
    box_instance_index  NUMBER(6)       NOT NULL,
    layer_no            NUMBER(4)       NOT NULL,
    position_no         NUMBER(4)       NOT NULL,
    stacking_class      VARCHAR2(20),
    CONSTRAINT fk_pi_pallet FOREIGN KEY (pallet_id) REFERENCES PALLET(pallet_id),
    CONSTRAINT fk_pi_product FOREIGN KEY (product_id) REFERENCES PRODUCT(product_id),
    CONSTRAINT fk_pi_orderline FOREIGN KEY (order_line_id) REFERENCES ORDER_LINE(order_line_id),
    CONSTRAINT uk_pi_position UNIQUE (pallet_id, layer_no, position_no),
    CONSTRAINT ck_pi_layer CHECK (layer_no > 0),
    CONSTRAINT ck_pi_position CHECK (position_no > 0),
    CONSTRAINT ck_pi_instance CHECK (box_instance_index > 0)
);

CREATE INDEX idx_pi_pallet ON PALLET_ITEM(pallet_id);
CREATE INDEX idx_pi_product ON PALLET_ITEM(product_id);
CREATE INDEX idx_pi_orderline ON PALLET_ITEM(order_line_id);

COMMENT ON TABLE PALLET_ITEM IS 'Colis affecté à une palette avec positionnement logique (couche/position)';
COMMENT ON COLUMN PALLET_ITEM.box_instance_index IS 'Index de l''instance du colis (1 à N pour N colis du même produit)';
COMMENT ON COLUMN PALLET_ITEM.layer_no IS 'Numéro de couche (1 = bas)';
COMMENT ON COLUMN PALLET_ITEM.position_no IS 'Position dans la couche';
COMMENT ON COLUMN PALLET_ITEM.stacking_class IS 'Classe d''empilage assignée par le moteur de règles (BOTTOM, MIDDLE, TOP)';

-- ----------------------------------------------------------------------------
-- Table : CONSTRAINT_VIOLATION
-- Description : Violation de contrainte détectée lors d'une exécution
-- ----------------------------------------------------------------------------
CREATE TABLE CONSTRAINT_VIOLATION (
    violation_id        NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id        NUMBER(12)      NOT NULL,
    pallet_id           NUMBER(12),
    rule_version_id     NUMBER(12)      NOT NULL,
    severity            VARCHAR2(10)    NOT NULL,
    description         VARCHAR2(1000)  NOT NULL,
    impact_score        NUMBER(5,2),
    CONSTRAINT fk_cv_execution FOREIGN KEY (execution_id) REFERENCES PALLETIZATION_EXECUTION(execution_id),
    CONSTRAINT fk_cv_pallet FOREIGN KEY (pallet_id) REFERENCES PALLET(pallet_id),
    CONSTRAINT fk_cv_ruleversion FOREIGN KEY (rule_version_id) REFERENCES RULE_VERSION(rule_version_id),
    CONSTRAINT ck_cv_severity CHECK (severity IN ('HARD', 'SOFT'))
);

CREATE INDEX idx_cv_execution ON CONSTRAINT_VIOLATION(execution_id);
CREATE INDEX idx_cv_pallet ON CONSTRAINT_VIOLATION(pallet_id);

COMMENT ON TABLE CONSTRAINT_VIOLATION IS 'Violations de contraintes détectées lors de la palettisation';

-- ----------------------------------------------------------------------------
-- Table : DECISION_TRACE
-- Description : Trace des décisions prises par le moteur lors d'une exécution
-- ----------------------------------------------------------------------------
CREATE TABLE DECISION_TRACE (
    trace_id            NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id        NUMBER(12)      NOT NULL,
    trace_order         NUMBER(6)       NOT NULL,
    step_name           VARCHAR2(100)   NOT NULL,
    pallet_id           NUMBER(12),
    pallet_item_id      NUMBER(12),
    rule_version_id     NUMBER(12),
    description         CLOB            NOT NULL,
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_dt_execution FOREIGN KEY (execution_id) REFERENCES PALLETIZATION_EXECUTION(execution_id),
    CONSTRAINT fk_dt_pallet FOREIGN KEY (pallet_id) REFERENCES PALLET(pallet_id),
    CONSTRAINT fk_dt_palletitem FOREIGN KEY (pallet_item_id) REFERENCES PALLET_ITEM(pallet_item_id),
    CONSTRAINT fk_dt_ruleversion FOREIGN KEY (rule_version_id) REFERENCES RULE_VERSION(rule_version_id)
);

CREATE INDEX idx_dt_execution ON DECISION_TRACE(execution_id);
CREATE INDEX idx_dt_order ON DECISION_TRACE(execution_id, trace_order);

COMMENT ON TABLE DECISION_TRACE IS 'Journal des décisions prises à chaque étape de la palettisation';
COMMENT ON COLUMN DECISION_TRACE.step_name IS 'Nom de l''étape (NORMALIZATION, GROUPING, ASSIGNMENT, LAYERING, VALIDATION)';
COMMENT ON COLUMN DECISION_TRACE.trace_order IS 'Ordre séquentiel de la trace dans l''exécution';

-- ----------------------------------------------------------------------------
-- Table : API_REQUEST_LOG
-- Description : Journal des requêtes API
-- ----------------------------------------------------------------------------
CREATE TABLE API_REQUEST_LOG (
    log_id              NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    endpoint            VARCHAR2(200)   NOT NULL,
    method              VARCHAR2(10)    NOT NULL,
    request_body        CLOB,
    response_status     NUMBER(3),
    response_time_ms    NUMBER(10),
    user_id             VARCHAR2(100),
    created_at          TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE INDEX idx_arl_endpoint ON API_REQUEST_LOG(endpoint);
CREATE INDEX idx_arl_created ON API_REQUEST_LOG(created_at);

COMMENT ON TABLE API_REQUEST_LOG IS 'Journal des requêtes API pour audit';

-- ----------------------------------------------------------------------------
-- Table : EXECUTION_METRIC
-- Description : Métriques de performance d'une exécution
-- ----------------------------------------------------------------------------
CREATE TABLE EXECUTION_METRIC (
    metric_id           NUMBER(12)      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    execution_id        NUMBER(12)      NOT NULL,
    metric_name         VARCHAR2(100)   NOT NULL,
    metric_value        NUMBER(12,4)    NOT NULL,
    unit                VARCHAR2(20),
    CONSTRAINT fk_em_execution FOREIGN KEY (execution_id) REFERENCES PALLETIZATION_EXECUTION(execution_id)
);

CREATE INDEX idx_em_execution ON EXECUTION_METRIC(execution_id);

COMMENT ON TABLE EXECUTION_METRIC IS 'Métriques de performance collectées lors d''une exécution';
