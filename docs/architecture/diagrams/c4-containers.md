# Diagramme C4 — Niveau Conteneurs

## Architecture des conteneurs applicatifs

```mermaid
graph TB
    WMS["WMS / ERP"]
    USER["Utilisateurs"]

    subgraph Docker["Conteneurs Docker"]
        subgraph Frontend["Frontend Container"]
            REACT["IHM React<br/><i>React 18+</i><br/>Création règles, visualisation palettes,<br/>dashboard, comparaison"]
        end

        subgraph Backend["Backend Container"]
            API_IN["API d'entrée<br/><i>Spring Boot 3</i><br/>POST /palletizations<br/>Validation, déclenchement"]
            
            ORCH["Orchestrateur<br/><i>Spring Boot 3</i><br/>Workflow palettisation,<br/>gestion async"]
            
            RULES["Moteur de règles<br/><i>Java 21</i><br/>Évaluation DSL,<br/>production ConstraintSet"]
            
            SOLVER["Solveur<br/><i>Java 21</i><br/>Heuristiques FFD,<br/>optimisation locale"]
            
            API_OUT["API de consultation<br/><i>Spring Boot 3</i><br/>GET résultats, explications,<br/>comparaison"]
            
            PERSIST["Couche de persistance<br/><i>Spring Data JPA</i><br/>Accès Oracle"]
        end
    end

    DB[("Oracle Database<br/><i>Exadata</i><br/>Référentiels, règles,<br/>exécutions, résultats")]

    WMS -->|"REST/JSON"| API_IN
    USER -->|"HTTPS"| REACT
    REACT -->|"REST/JSON"| API_IN
    REACT -->|"REST/JSON"| API_OUT

    API_IN --> ORCH
    ORCH --> RULES
    ORCH --> SOLVER
    RULES -->|"ConstraintSet"| SOLVER
    ORCH --> PERSIST
    API_OUT --> PERSIST
    PERSIST -->|"JDBC"| DB
```

## Description des conteneurs

| Conteneur | Technologie | Responsabilité |
|-----------|-------------|----------------|
| **IHM React** | React 18+ | Interface utilisateur : création de règles par formulaires, visualisation des palettes, dashboard, comparaison d'exécutions |
| **API d'entrée** | Spring Boot 3 | Réception des commandes, validation du format, déclenchement asynchrone du calcul |
| **Orchestrateur** | Spring Boot 3 | Pilotage du workflow : préparation → règles → solveur → validation → persistance |
| **Moteur de règles** | Java 21 | Chargement et évaluation du DSL de règles, production du ConstraintSet |
| **Solveur** | Java 21 | Calcul de la palettisation par heuristiques (FFD + amélioration locale) |
| **API de consultation** | Spring Boot 3 | Restitution des résultats, historique, explications, comparaison |
| **Couche de persistance** | Spring Data JPA | Accès unifié à la base Oracle |
| **Oracle Database** | Oracle Exadata | Stockage central : référentiels, règles versionnées, exécutions, palettes, traces |
