# Couche de Tests (`tests`)

Le dossier `tests` contient les classes permettant de vérifier le bon fonctionnement de l'application de manière isolée.

### Utilité :
1. **Fiabilité** : S'assurer que les méthodes CRUD fonctionnent avant de les intégrer à l'interface.
2. **Débogage** : Tester la connexion à la base de données et la lecture des données sans lancer toute l'application JavaFX.
3. **Évolution** : Permet de vérifier que de nouvelles modifications n'ont pas cassé les fonctionnalités existantes (non-régression).

### Exemple :
- **`TestProjet.java`** : Une classe simple avec une méthode `main` qui appelle `ProjetService` pour insérer ou afficher un projet dans la console.
