# Diagramme C4 — Niveau Contexte

## Vue d'ensemble du système MBPal et ses interactions

```mermaid
graph TB
    subgraph Acteurs externes
        WMS["WMS / ERP<br/>(Système source)"]
        USER_BUSINESS["Utilisateur métier<br/>(Logisticien)"]
        USER_ADMIN["Administrateur règles<br/>(Métier / IT)"]
    end

    subgraph MBPal["MBPal — Application de palettisation automatique"]
        API["API REST"]
        UI["IHM React"]
    end

    DB[("Oracle Database")]

    WMS -->|"Soumet commandes<br/>(API REST)"| API
    USER_BUSINESS -->|"Consulte résultats<br/>Lance palettisation"| UI
    USER_ADMIN -->|"Crée/gère les règles<br/>Configure les rulesets"| UI
    UI -->|"Appels API"| API
    API -->|"Lecture/Écriture"| DB
```

## Description

| Élément | Rôle |
|---------|------|
| **WMS / ERP** | Système source qui soumet les commandes à palettiser via l'API REST |
| **Utilisateur métier** | Logisticien qui lance les palettisations et consulte les résultats via l'IHM |
| **Administrateur règles** | Utilisateur métier ou IT qui crée, modifie et priorise les règles de palettisation |
| **MBPal** | Application de calcul automatique de palettes |
| **Oracle Database** | Base de données centrale stockant référentiels, règles, exécutions et résultats |
