# Couche Interfaces : Les Contrats (`interfaces`)

Le dossier `interfaces` définit les signatures des méthodes que les services doivent implémenter.

### Utilité :
1. **Abstraction** : Permet de définir *quoi* faire sans se soucier de l'*implémentation* immédiate.
2. **Polymorphisme** : On peut changer l'implémentation (ex: passer de MySQL à une autre base de données) sans modifier le reste du code de l'application.
3. **Maintenance** : Assure que tous les services respectent une structure commune (ex: tous doivent avoir une méthode `ajouter`).

### Fichiers :
- **`IService.java`** : Une interface générique utilisant les "Generics" `<T>` pour rationaliser le développement de tous les services CRUD.
