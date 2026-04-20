# Couche Modèle : Les Entités (`entities`)

Le dossier `entities` contient les classes qui représentent les données fondamentales de l'application MentorAI. Ce sont des Plain Old Java Objects (POJO).

### Rôles des fichiers :
1. **`Projet.java`** : Définit ce qu'est un projet (Titre, description, dates, type).
2. **`Parcours.java`** : Représente une structure pédagogique ou un cheminement regroupant plusieurs projets.
3. **`Ressource.java`** : Définit les documents ou outils (PDF, liens, photos) attachés à un projet.

### Pourquoi cette couche ?
- **Encapsulation** : Utilisation de `private` pour les attributs et de getters/setters.
- **Transférabilité** : Ces objets sont utilisés par les Services pour être enregistrés en base de données, et par les Contrôleurs pour être affichés.
