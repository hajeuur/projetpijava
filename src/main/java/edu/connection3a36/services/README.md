# Couche Service : Accès aux Données (`services`)

Le dossier `services` contient la logique d'accès à la base de données (Data Access Object - DAO). C'est ici que le code SQL est écrit et exécuté.

### Rôles des fichiers :
1. **`ProjetService.java`** : Contient les méthodes `ajouter`, `modifier`, `supprimer` et `afficher` pour les projets.
2. **`ParcoursService.java`** : Gère la persistance des parcours.
3. **`RessourceService.java`** : Gère les opérations liées aux ressources.

### Architecture Technique :
- **JDBC (Java Database Connectivity)** : Utilisation des classes `Connection`, `PreparedStatement` et `ResultSet`.
- **Méthodes CRUD** : Chaque service suit le pattern standard Create, Read, Update, Delete.
- **Gestion des Exceptions** : Utilisation de `try-catch` et `SQLException` pour assurer que l'application ne plante pas en cas de problème de connexion.
- **Découplage** : Les contrôleurs ne savent pas *comment* les données sont enregistrées, ils appellent juste les méthodes du service.
