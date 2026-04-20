# Couche Vue : Les Ressources (`resources`)

Ce dossier contient les fichiers non-Java nécessaires au fonctionnement de l'interface et du design.

### Composants :
1. **Fichiers FXML** : Définissent la structure de l'interface utilisateur en format XML.
   - `Dashboard.fxml` : Layout principal.
   - `Statistiques.fxml` : Vue des graphiques.
   - `BackOfficeProjets.fxml` : Vue de gestion des projets.
2. **Design System** : Utilisation de styles CSS embarqués ou via des attributs pour implémenter la charte graphique MentorUI (Bleu nuit, Gris perle, effets de survol).

### Avantages :
- **Séparation des préoccupations** : On modifie le design (FXML) sans toucher au code métier (Java).
- **Outils** : Ces fichiers sont éditables avec Scene Builder pour un gain de productivité.
