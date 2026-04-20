# Couche Contrôleur : La Logique d'Affichage (`Controller`)

Le dossier `Controller` contient les classes Java responsables de la manipulation de l'Interface Utilisateur (JavaFX). 

### Fichiers Clés et Utilité :
1. **`DashboardController.java`** : Le chef d'orchestre. Gère la navigation entre les différentes pages (Sidebar) et le chargement dynamique du contenu central.
2. **`BackOfficeProjetsController.java`** : Gère la liste des projets, la pagination, les filtres de recherche et la génération de rapports PDF.
3. **`StatistiquesController.java`** : Logique métier visuelle. Extrait les données des services pour les transformer en graphiques (BarChart, PieChart).
4. **`Afficher/Ajouter/ModifierParcoursController.java`** : Gère le cycle de vie des parcours (CRUD visuel).

### Mécanismes Techniques :
- **`@FXML`** : Annotation permettant de lier les variables Java aux éléments définis dans les fichiers FXML.
- **`initialize()`** : Méthode appelée automatiquement au chargement de la vue pour préparer les données.
- **`Platform.runLater()`** : Utilisé pour mettre à jour l'interface graphique de manière sécurisée depuis des threads différents.
