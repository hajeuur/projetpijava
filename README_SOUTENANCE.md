# Projet MentorAI - Documentation Architecture 🎓

Ce projet est une application de gestion administrative et de suivi de projets éducatifs, développée en Java (JavaFX) avec une persistance MySQL.

## Architecture Générale
L'application adopte une architecture en **couches (N-Tier)** respectant le principe de **responsabilité unique** :

1. **Entities** : Représentation des données.
2. **Interfaces** : Contrats de services.
3. **Services** : Logique d'accès aux données (DAO/CRUD).
4. **Controllers** : Logique de contrôle de l'interface utilisateur.
5. **Resources** : Vues graphiques (FXML) et styles.

## Points Forts Techniques à valoriser :
- **Pattern MVC** pour une maintenance facilitée.
- **Reporting PDF** dynamique via la librairie iText.
- **Visualisation de Données** complexe avec des graphiques JavaFX stylisés.
- **Pagination et Filtrage** optimisés pour la manipulation de gros volumes de données.
- **Gestion des Threads** (`Platform.runLater`) pour une interface fluide et réactive.
