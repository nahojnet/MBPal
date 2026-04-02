-- ============================================================================
-- MBPal — Script V4 : Données de référence initiales
-- Base : Oracle
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Supports standards
-- ----------------------------------------------------------------------------
INSERT INTO SUPPORT_TYPE (support_code, label, length_mm, width_mm, height_mm, max_load_kg, max_total_height_mm, usable_length_mm, usable_width_mm, mergeable_flag, merge_target_code, merge_quantity)
VALUES ('EURO', 'Palette EURO (1200x800)', 1200, 800, 150, 800.00, 1800, 1200, 800, 'N', NULL, NULL);

INSERT INTO SUPPORT_TYPE (support_code, label, length_mm, width_mm, height_mm, max_load_kg, max_total_height_mm, usable_length_mm, usable_width_mm, mergeable_flag, merge_target_code, merge_quantity)
VALUES ('HALF', 'Demi-palette (600x800)', 600, 800, 150, 400.00, 1800, 600, 800, 'Y', 'EURO', 2);

INSERT INTO SUPPORT_TYPE (support_code, label, length_mm, width_mm, height_mm, max_load_kg, max_total_height_mm, usable_length_mm, usable_width_mm, mergeable_flag, merge_target_code, merge_quantity)
VALUES ('DOLLY', 'Dolly (600x400)', 600, 400, 170, 250.00, 1500, 600, 400, 'N', NULL, NULL);

-- ----------------------------------------------------------------------------
-- Produits de test (pour les cas de test métier)
-- ----------------------------------------------------------------------------
INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-001', 'Carton lourd ambiant', 'AMBIANT', 400, 300, 250, 22.00, 'ROBUSTE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-002', 'Carton léger ambiant', 'AMBIANT', 300, 200, 150, 3.50, 'ROBUSTE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-003', 'Carton surgelé standard', 'SURGELE', 400, 300, 200, 12.00, 'ROBUSTE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-004', 'Carton frais standard', 'FRAIS', 350, 250, 200, 8.00, 'ROBUSTE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-005', 'Carton fragile ambiant', 'AMBIANT', 300, 300, 300, 6.00, 'FRAGILE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-006', 'Carton non empilable', 'AMBIANT', 500, 400, 400, 15.00, 'ROBUSTE', 'N');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-007', 'Petit colis surgelé', 'SURGELE', 200, 150, 100, 2.00, 'ROBUSTE', 'Y');

INSERT INTO PRODUCT (product_code, label, temperature_type, length_mm, width_mm, height_mm, weight_kg, fragility_level, stackable_flag)
VALUES ('PROD-008', 'Carton très lourd', 'AMBIANT', 600, 400, 300, 45.00, 'ROBUSTE', 'Y');

-- ----------------------------------------------------------------------------
-- Règles exemples
-- ----------------------------------------------------------------------------

-- Règle : Colis lourds en bas
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_HEAVY_BOTTOM', 'PALLETIZATION', 'PACKAGE', 'HARD', 'Les colis lourds doivent être positionnés dans les couches inférieures de la palette');

-- Règle : Colis légers en haut
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_LIGHT_TOP', 'PALLETIZATION', 'PACKAGE', 'SOFT', 'Les colis légers doivent être positionnés dans les couches supérieures de la palette');

-- Règle : Surgelés ensemble
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_FROZEN_GROUP', 'PALLETIZATION', 'PACKAGE', 'HARD', 'Les colis surgelés doivent être regroupés sur la même palette');

-- Règle : Séparation par température
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_TEMP_SEPARATION', 'PALLETIZATION', 'INTER_PACKAGE', 'HARD', 'Les colis de familles de température différentes ne doivent pas être mélangés sur une même palette');

-- Règle : Hauteur max palette EURO
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_MAX_HEIGHT_EURO', 'PALLETIZATION', 'PALLET', 'HARD', 'La hauteur totale de la palette EURO ne doit pas dépasser 1800 mm');

-- Règle : Poids max palette EURO
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_MAX_WEIGHT_EURO', 'PALLETIZATION', 'PALLET', 'HARD', 'Le poids total des colis sur palette EURO ne doit pas dépasser 800 kg');

-- Règle : Fragile pas sous un lourd
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_FRAGILE_NO_HEAVY_ABOVE', 'PALLETIZATION', 'INTER_PACKAGE', 'HARD', 'Un colis lourd ne doit jamais être placé au-dessus d''un colis fragile');

-- Règle : Préférer EURO
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_PREFER_EURO', 'PALLETIZATION', 'PALLET', 'SOFT', 'Quand le choix est possible, privilégier les palettes EURO');

-- Règle : Minimiser le nombre de palettes
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_MINIMIZE_PALLETS', 'PALLETIZATION', 'PALLET', 'SOFT', 'Minimiser le nombre total de palettes utilisées');

-- Règle : Fusion de demi-palettes
INSERT INTO RULE (rule_code, domain, scope, severity, description)
VALUES ('R_MERGE_HALF_PALLETS', 'PALLETIZATION', 'PALLET', 'SOFT', 'Fusionner les demi-palettes compatibles en palettes complètes quand possible');

COMMIT;
