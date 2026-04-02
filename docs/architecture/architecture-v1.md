# Document d'architecture — v1

## Application de constitution automatique de palettes (MBPal)

---

## 1. Résumé exécutif

L'application MBPal prend en entrée une **commande client** (liste de produits avec quantités de colis) et produit en sortie un **plan de palettisation** indiquant :

- le nombre de palettes nécessaires ;
- la constitution détaillée de chaque palette ;
- l'ordre / positionnement logique des colis sur chaque palette (couches et positions) ;
- les justifications liées aux règles appliquées ;
- les éventuelles violations ou arbitrages.

**Particularité majeure** : les règles de constitution ne sont pas encore définies. L'architecture est donc conçue autour d'un cadre de décision flexible, capable de recevoir, versionner et exécuter de futures règles fonctionnelles.

**Nature du problème** : il s'agit d'un problème hybride combinant :
1. évaluation de contraintes / règles métier ;
2. optimisation combinatoire (répartition sur plusieurs palettes).

La solution combine donc un **moteur de règles versionné** avec un **solveur d'optimisation par heuristiques**.

---

## 2. Contexte et objectifs

### 2.1 Finalité métier

Optimiser la préparation logistique des commandes en automatisant la constitution de palettes à partir de critères comme :

- poids des colis ;
- température (ambiant, frais, surgelé) ;
- dimensions des colis ;
- dimensions et contraintes du support (palette, demi-palette, dolly) ;
- nombre de colis par commande ;
- contraintes de hauteur, stabilité, regroupement, compatibilité.

### 2.2 Objectifs du projet

| # | Objectif | Mesure de succès |
|---|----------|-----------------|
| 1 | Automatiser la constitution de palettes | 100 % des commandes traitées automatiquement |
| 2 | Respecter les contraintes métier | 0 violation de contrainte HARD |
| 3 | Minimiser le nombre de palettes | Taux de remplissage moyen > 75 % |
| 4 | Permettre l'évolution des règles sans code | Règles administrables via IHM |
| 5 | Tracer toutes les décisions | Chaque exécution est rejouable et auditable |
| 6 | Assurer des performances acceptables | < 5 secondes pour une commande standard |

---

## 3. Périmètre fonctionnel

### 3.1 Entrées

#### Référentiel produit

Chaque produit est défini avec les caractéristiques de son colis :

| Attribut | Description | Exemple |
|----------|-------------|---------|
| product_code | Code unique du produit | PROD-001 |
| temperature_type | Famille de température | AMBIANT / FRAIS / SURGELE |
| length_mm | Longueur du colis (mm) | 400 |
| width_mm | Largeur du colis (mm) | 300 |
| height_mm | Hauteur du colis (mm) | 250 |
| weight_kg | Poids du colis (kg) | 22.0 |
| fragility_level | Niveau de fragilité | FRAGILE / ROBUSTE |
| stackable_flag | Empilable ou non | Y / N |

> **Note** : la quantité commandée correspond directement au nombre de colis. Le conditionnement est déjà donné dans le référentiel produit.

#### Référentiel support

| Attribut | Description | Exemple |
|----------|-------------|---------|
| support_code | Code du type de support | EURO / HALF / DOLLY |
| dimensions | L × l × H (mm) | 1200 × 800 × 150 |
| max_load_kg | Charge maximale (kg) | 800 |
| max_total_height_mm | Hauteur max totale (mm) | 1800 |
| mergeable_flag | Fusionnable | Y / N |
| merge_target_code | Support cible après fusion | EURO (pour 2 HALF) |

#### Commande client

| Attribut | Description |
|----------|-------------|
| external_order_id | Identifiant de la commande dans le système source |
| customer_id | Identifiant client |
| lines[] | Liste de lignes : (product_id, box_quantity) |
| supportPolicy | Supports autorisés pour cette commande |

### 3.2 Sorties

Pour chaque exécution de palettisation :

| Sortie | Description |
|--------|-------------|
| execution_id | Identifiant unique d'exécution |
| total_pallets | Nombre de palettes générées |
| palettes[] | Liste détaillée des palettes |
| applied_ruleset | Version du ruleset appliqué |
| violations[] | Contraintes violées (SOFT uniquement) |
| global_score | Score de qualité global |
| decision_trace[] | Journal des décisions |

Pour chaque palette :

| Attribut | Description |
|----------|-------------|
| support_type | Type de support choisi |
| total_weight_kg | Poids total |
| total_height_mm | Hauteur totale |
| fill_rate_pct | Taux de remplissage (%) |
| stability_score | Score de stabilité |
| items[] | Liste ordonnée des colis (couche, position, classe d'empilage) |

### 3.3 Fonctions attendues

**Fonctions métier :**
- Calculer une palettisation à partir d'une commande
- Appliquer des règles versionnées
- Tracer chaque décision
- Rejouer une exécution avec une autre version de règles
- Simuler plusieurs stratégies (dry run)
- Comparer deux résultats de palettisation

**Fonctions techniques :**
- Exposer des API REST d'entrée et de consultation
- Stocker les données et résultats dans Oracle
- Proposer une IHM de visualisation et d'administration des règles
- Journaliser et auditer les exécutions

---

## 4. Principes d'architecture

### 4.1 Principe fondamental

> **Découpler la définition des règles de l'algorithme de calcul.**

Cela permet :
- d'éviter le code en dur ;
- de faire évoluer les règles indépendamment ;
- de rejouer des calculs selon une version donnée ;
- d'assurer l'audit.

### 4.2 Principe de calcul — Trois étages

| Étage | Rôle |
|-------|------|
| **1. Préparation / normalisation** | Enrichir les colis avec les attributs produit, calculer les classes logistiques |
| **2. Évaluation des règles** | Transformer les règles métier en contraintes techniques (ConstraintSet) |
| **3. Résolution** | Répartir les colis sur des palettes, ordonner, vérifier, scorer |

### 4.3 Autres principes

- **Tout asynchrone** : soumission → exécution en arrière-plan → consultation du résultat
- **Placement logique** : couches et positions (pas de coordonnées 3D au MVP)
- **Traçabilité complète** : chaque décision est tracée avec la règle qui l'a motivée
- **Persistance systématique** : les palettes calculées sont sauvegardées en base

---

## 5. Architecture cible

### 5.1 Vue d'ensemble

```
  [WMS / ERP]          [Utilisateur métier]       [Admin règles]
       |                       |                        |
       v                       v                        v
  [API d'entrée REST]    [IHM React]  <-- Création règles par formulaires
       |                       |
       v                       v
  [Service d'orchestration palettisation]
       |
       +--------------------+--------------------+
       |                    |                    |
       v                    v                    v
  [Préparation]     [Moteur de règles]   [Solveur heuristique]
       |                    |                    |
       +--------------------+                    |
                |                                |
                v                                |
        [Validation / scoring] <-----------------+
                |
                v
        [Persistance Oracle]
                |
        +-------+-------+
        |               |
        v               v
  [API consultation]  [IHM React]
```

### 5.2 Conteneurisation

Deux conteneurs Docker :

| Conteneur | Contenu |
|-----------|---------|
| **Backend** | Java 21 + Spring Boot 3 : API, orchestrateur, moteur de règles, solveur, persistance |
| **Frontend** | React 18+ : IHM de visualisation et d'administration des règles |

La base Oracle est fournie séparément (instance existante).

La haute disponibilité est gérée par une double instance Docker passive (hors périmètre applicatif).

---

## 6. Composants applicatifs

### 6.1 API d'entrée

| Aspect | Détail |
|--------|--------|
| **Rôle** | Recevoir une commande, valider, déclencher le traitement async |
| **Endpoint principal** | `POST /api/v1/palletizations` |
| **Réponse** | 202 Accepted + executionId |
| **Technologie** | Spring Boot 3 REST Controller |

### 6.2 Service d'orchestration

| Aspect | Détail |
|--------|--------|
| **Rôle** | Piloter le workflow : préparation → règles → solveur → validation → persistance |
| **Mode** | Asynchrone (exécution en arrière-plan) |
| **Technologie** | Spring @Async ou file de traitement interne |

### 6.3 Moteur de règles

| Aspect | Détail |
|--------|--------|
| **Rôle** | Charger les règles actives, les évaluer, produire un ConstraintSet |
| **Approche** | DSL maison (JSON) stocké en base, interprété par l'application |
| **Versioning** | Règles versionnées, regroupées en rulesets |
| **Portées** | PACKAGE (colis), PALLET (palette), INTER_PACKAGE (entre colis) |
| **Sévérités** | HARD (jamais violable), SOFT (préférentielle, avec poids) |

> Voir le document détaillé : [`rule-engine-design.md`](rule-engine-design.md)

### 6.4 Solveur

| Aspect | Détail |
|--------|--------|
| **Rôle** | Calculer la meilleure répartition des colis en palettes |
| **Approche** | Heuristiques : First Fit Decreasing + amélioration locale |
| **Objectif** | Bonne solution rapide (< 5 secondes) |
| **Entrées** | Liste de colis enrichis + ConstraintSet |
| **Sorties** | Plan de palettisation (palettes, couches, positions, score) |

> Voir le document détaillé : [`solver-design.md`](solver-design.md)

### 6.5 Couche de persistance

| Aspect | Détail |
|--------|--------|
| **Rôle** | Stocker référentiels, règles, commandes, exécutions, palettes, traces |
| **Technologie** | Spring Data JPA + Oracle JDBC |
| **Base** | Oracle (instance fournie) |

> Voir le document détaillé : [`data-model.md`](data-model.md)

### 6.6 API de consultation

| Aspect | Détail |
|--------|--------|
| **Rôle** | Restituer les résultats, historique, explications, comparaisons |
| **Endpoints** | `GET /api/v1/palletizations/{id}`, `/explanations`, `/compare` |
| **Technologie** | Spring Boot 3 REST Controller |

> Voir le document détaillé : [`api-specification.md`](api-specification.md)

### 6.7 IHM React

| Aspect | Détail |
|--------|--------|
| **Rôle** | Visualisation des palettes, création de règles par formulaires, dashboard, comparaison |
| **Création de règles** | Formulaires avec menus déroulants (portée, sévérité, conditions, effets) — pas d'édition JSON |
| **Priorisation** | Tableau de priorisation des règles SOFT (drag-and-drop, pondération) |
| **Visualisation** | Vue logique des palettes par couches |
| **Technologie** | React 18+ |

> Voir le document détaillé : [`ui-design.md`](ui-design.md)

---

## 7. Stack technique

| Composant | Technologie | Justification |
|-----------|-------------|---------------|
| **Backend** | Java 21 + Spring Boot 3 | Écosystème mature pour orchestration, API REST, JPA, async |
| **Solveur** | Java 21 (heuristiques maison) | FFD + amélioration locale, intégré au backend |
| **Moteur de règles** | DSL JSON + interpréteur Java | Simple, traçable, adapté à un domaine fermé |
| **Base de données** | Oracle (Exadata) | Imposé, instance fournie, schéma à créer |
| **Frontend** | React 18+ | IHM moderne, composants formulaires riches |
| **Conteneurisation** | Docker | Backend + Frontend dans des conteneurs séparés |
| **Build** | Maven / Gradle (backend), npm (frontend) | Standards de l'industrie |

---

## 8. Exigences non fonctionnelles

### 8.1 Performance

| Métrique | Cible |
|----------|-------|
| API de soumission | < 500 ms (acceptation async) |
| Calcul palettisation (commande standard) | < 2 secondes |
| Calcul palettisation (commande volumineuse 200+ colis) | < 5 secondes |
| API de consultation | < 500 ms |

### 8.2 Volumétrie

| Métrique | Volume |
|----------|--------|
| Commandes / jour | ~1 000 |
| Colis / commande (typique) | 20 à 50 |
| Colis / commande (max) | 200+ |
| Historique à conserver (exécutions) | 12 mois |
| Historique à conserver (logs/traces) | 6 mois |

### 8.3 Traçabilité

Obligatoire. Chaque exécution conserve :
- version du ruleset utilisé ;
- données d'entrée (commande) ;
- solution produite (palettes) ;
- contraintes violées ;
- score global ;
- journal de décision étape par étape.

### 8.4 Sécurité

- Authentification API (à définir : JWT, OAuth2, ou API key) ;
- Journal d'accès (API_REQUEST_LOG) ;
- Séparation des droits : lecture / écriture / administration des règles.

### 8.5 Testabilité

- Tests unitaires par règle ;
- Cas de test métier stockés en base (RULE_TEST_CASE) ;
- Tests de non-régression sur règles versionnées ;
- Tests de performance sur commandes volumineuses.

> Voir les cas de test métier : [`test-cases.md`](../business/test-cases.md)

### 8.6 Haute disponibilité

Gérée hors périmètre applicatif par une double instance Docker passive. L'application elle-même est stateless (état en base Oracle).

---

## 9. Stratégie de mise en oeuvre

### 9.1 Approche en 3 lots

#### Lot 1 — Socle

| Composant | Contenu |
|-----------|---------|
| Modèle de données | Schéma Oracle complet (référentiels, règles, exécutions) |
| API | Soumission (POST) et consultation (GET) |
| Moteur de règles | Version minimale avec DSL de base |
| Solveur | FFD simple + respect des contraintes hard |
| IHM | Consultation basique des résultats |
| Persistance | Commandes, colis, palettes, traces |

#### Lot 2 — Règles avancées

| Composant | Contenu |
|-----------|---------|
| Moteur de règles | Versioning complet, règles hard/soft, règles inter-colis |
| Scoring | Score de qualité, pondération des règles soft |
| IHM règles | Création de règles par formulaires, tableau de priorisation |
| Simulation | Dry run, rejeu avec autre ruleset |
| Explications | API d'explication détaillée |

#### Lot 3 — Visualisation avancée

| Composant | Contenu |
|-----------|---------|
| IHM palettes | Vue graphique des couches, code couleur par température |
| Comparaison | Comparaison côte à côte de deux exécutions |
| Dashboard | Métriques, tendances, alertes |
| Administration | Gestion complète des rulesets, import/export référentiels |

---

## 10. Risques et points d'attention

### Risque 1 — Vouloir tout résoudre par des règles

Le nombre de palettes et leur constitution relèvent d'un problème d'optimisation, pas seulement d'un moteur de règles. La séparation moteur de règles / solveur est essentielle.

**Mitigation** : architecture à deux briques clairement séparées.

### Risque 2 — Règles mal formalisées

Les règles métier doivent être transformées de formulations floues en contraintes techniques testables.

**Mitigation** : catalogue de règles structuré ([`rule-catalog-template.md`](../business/rule-catalog-template.md)) + cas de test métier ([`test-cases.md`](../business/test-cases.md)).

### Risque 3 — Absence de priorisation des contraintes

Sans distinction claire entre HARD et SOFT, et sans pondération des SOFT, le solveur ne peut pas arbitrer.

**Mitigation** : tableau de priorisation des règles SOFT avec poids, administrable via IHM.

### Risque 4 — Visualisation trop tardive

Une représentation claire des résultats est essentielle pour l'acceptation métier.

**Mitigation** : IHM de consultation basique dès le Lot 1, visualisation avancée au Lot 3.

### Risque 5 — Performance du solveur

Sur des commandes volumineuses (200+ colis), le calcul pourrait dépasser les 5 secondes.

**Mitigation** : solveur à temps borné (retourne la meilleure solution trouvée dans le budget temps).

---

## 11. Documents associés

| Document | Description |
|----------|-------------|
| [`data-model.md`](data-model.md) | Modèle de données Oracle détaillé |
| [`rule-engine-design.md`](rule-engine-design.md) | Design du moteur de règles |
| [`solver-design.md`](solver-design.md) | Design du solveur |
| [`api-specification.md`](api-specification.md) | Spécification des APIs REST |
| [`ui-design.md`](ui-design.md) | Design de l'IHM |
| [`rule-catalog-template.md`](../business/rule-catalog-template.md) | Template catalogue de règles métier |
| [`test-cases.md`](../business/test-cases.md) | Cas de test métier |
| [`diagrams/`](diagrams/) | Diagrammes C4, flux, séquence, modèle de données |

---

## 12. Formulation de synthèse

> L'application de palettisation MBPal reçoit en entrée une commande client composée d'un ensemble de produits avec leurs quantités de colis, caractérisés par leur poids, leurs dimensions et leur température de conservation, ainsi que par les caractéristiques des supports de palettisation autorisés. En sortie, l'application détermine le nombre de palettes nécessaires et la constitution détaillée de chacune d'elles.
>
> Le système est conçu pour intégrer un ensemble de règles de constitution encore en cours de définition. L'architecture dissocie le référentiel de règles versionnées, leur évaluation technique via un DSL interprété, et le mécanisme de calcul de la solution optimale par heuristiques.
>
> La solution combine un moteur de règles versionné avec un solveur d'optimisation sous contraintes, le tout exposé via des API REST, persisté dans Oracle et administrable via une IHM React.
