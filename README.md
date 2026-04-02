# MBPal — Application de palettisation automatique

Application de constitution automatique de palettes a partir de commandes clients. Le systeme prend en entree une commande (liste de produits avec quantites de colis) et produit un plan de palettisation optimal.

## Architecture

```
[WMS / ERP] --> [API REST] --> [Orchestrateur]
                                    |
                     +--------------+--------------+
                     |              |              |
                [Preparation] [Moteur regles] [Solveur FFD]
                     |              |              |
                     +--------------+              |
                            |                      |
                     [Validation / scoring] <------+
                            |
                     [Oracle Database]
                            |
                  +---------+---------+
                  |                   |
            [API consultation]   [IHM React]
```

**Deux briques centrales :**
- **Moteur de regles** : DSL JSON versionne, interprete par l'application. Produit un `ConstraintSet`.
- **Solveur** : Heuristique First Fit Decreasing + amelioration locale. Consomme le `ConstraintSet` et produit le plan de palettisation.

## Stack technique

| Composant | Technologie |
|-----------|-------------|
| Backend | Java 21 + Spring Boot 3 |
| Base de donnees | Oracle (Exadata) |
| Frontend | React 18 + Vite |
| Conteneurisation | Docker |

## Structure du projet

```
MBPal/
├── backend/                    # API Spring Boot
│   └── src/main/java/com/mbpal/
│       ├── api/                # Controllers REST, DTOs, exceptions
│       ├── config/             # Configuration Spring (async, Jackson)
│       ├── domain/             # Entites JPA + enums
│       ├── engine/             # Moteur de regles (DSL interpreter)
│       ├── orchestrator/       # Workflow async de palettisation
│       ├── repository/         # Spring Data JPA repositories
│       ├── service/            # Services metier
│       └── solver/             # Solveur de palettisation (FFD)
├── frontend/                   # IHM React
│   └── src/
│       ├── components/         # Layout, composants communs
│       ├── pages/              # 12 pages (dashboard, regles, palettisation...)
│       ├── services/           # Client API Axios
│       └── utils/              # Constantes, enums
├── db/oracle/                  # Scripts DDL Oracle
│   ├── V1__referential.sql     # Tables PRODUCT, SUPPORT_TYPE
│   ├── V2__rules.sql           # Tables RULE, RULE_VERSION, RULESET...
│   ├── V3__execution.sql       # Tables ORDER, EXECUTION, PALLET...
│   └── V4__seed_data.sql       # Donnees de reference
├── docs/
│   ├── architecture/           # Documents d'architecture
│   └── business/               # Catalogue regles, cas de test
├── docker/                     # Dockerfiles + nginx
└── docker-compose.yml          # Orchestration des 3 containers
```

## Lancement rapide

### Avec Docker Compose

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080/api/v1 |
| Oracle DB | localhost:1521 |

### Developpement local

**Backend :**
```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=dev'
```

**Frontend :**
```bash
cd frontend
npm install
npm run dev
```

## API principales

| Methode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/v1/palletizations` | Soumettre une palettisation (async) |
| GET | `/api/v1/palletizations/{id}` | Resultat d'une execution |
| GET | `/api/v1/palletizations/{id}/explanations` | Explications detaillees |
| POST | `/api/v1/palletizations/compare` | Comparer deux executions |
| POST | `/api/v1/palletizations/simulate` | Simulation (dry run) |
| GET/POST | `/api/v1/products` | Referentiel produits |
| GET/POST | `/api/v1/supports` | Referentiel supports |
| GET/POST | `/api/v1/rules` | Gestion des regles |
| GET/POST | `/api/v1/rulesets` | Gestion des rulesets |

Voir la specification complete : [`docs/architecture/api-specification.md`](docs/architecture/api-specification.md)

## Fonctionnalites IHM

- **Dashboard** : metriques, dernières executions
- **Palettisation** : lancement, historique, resultat avec visualisation par couches, comparaison
- **Regles** : creation par formulaires/dropdowns (5 etapes), pas d'edition JSON
- **Rulesets** : gestion, publication, tableau de priorisation des regles SOFT
- **Referentiels** : CRUD produits et supports

## Documentation

| Document | Description |
|----------|-------------|
| [`architecture-v1.md`](docs/architecture/architecture-v1.md) | Architecture complete |
| [`data-model.md`](docs/architecture/data-model.md) | Modele de donnees Oracle |
| [`rule-engine-design.md`](docs/architecture/rule-engine-design.md) | Design du moteur de regles |
| [`solver-design.md`](docs/architecture/solver-design.md) | Design du solveur |
| [`api-specification.md`](docs/architecture/api-specification.md) | Specification des 26 endpoints |
| [`ui-design.md`](docs/architecture/ui-design.md) | Design de l'IHM |
| [`rule-catalog-template.md`](docs/business/rule-catalog-template.md) | Catalogue de regles metier |
| [`test-cases.md`](docs/business/test-cases.md) | 10 cas de test metier |
