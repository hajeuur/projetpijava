# 🎯 MentorAI - Skill Gap Analysis (Documentation IA)

Cette fonctionnalité permet aux utilisateurs de mesurer l'écart entre leurs compétences actuelles (extraites de leurs projets) et les exigences du marché pour un métier spécifique.

## 🏗️ Architecture du Système

Le module fonctionne sur un modèle **Hybride Java/Python** :
1.  **Frontend (JavaFX)** : Interface utilisateur pour choisir un métier et visualiser les résultats.
2.  **Service de Pont (Java)** : Exécute un sous-processus Python et gère l'échange de données via JSON.
3.  **Moteur d'Analyse (Python)** : Calcule les scores, identifie les compétences manquantes et génère les données du graphique.

---

## 📂 Détail des Fichiers

### 🐍 Partie Python (Moteur IA)
*   **`python_app/job_requirements.json`** : 
    *   La base de connaissances. Elle contient les listes de compétences (mots-clés) requises pour chaque métier.
*   **`python_app/skill_gap_analyzer.py`** : 
    *   Le cerveau de l'analyse initiale. Il compare les compétences et prépare les données du graphique Radar.

### 🤖 Partie Générative (Groq API)
*   **Générateur de Roadmap Dynamique** : 
    *   Directement intégré dans `SkillGapController.java`. Lorsqu'une compétence manquante est cliquée, l'application sollicite l'API Groq pour générer un plan de formation personnalisé sur 3 mois.

### ☕ Partie Java (Intégration & UI)
*   **`src/main/java/edu/connection3a36/services/PythonIAIService.java`** : 
    *   Le connecteur. Il lance l'interpréteur Python, envoie les données via `stdin` (flux d'entrée) pour éviter les erreurs de caractères spéciaux sur Windows, et récupère la réponse JSON.
*   **`src/main/resources/SkillGap.fxml`** : 
    *   La vue utilisateur. Elle définit le design moderne du tableau de bord, les zones de texte et le conteneur pour le graphique.
*   **`src/main/java/edu/connection3a36/Controller/SkillGapController.java`** : 
    *   Le chef d'orchestre. Il récupère les technologies des projets enregistrés en base de données, appelle le service Python en arrière-plan (Multi-threading) pour ne pas figer l'application, et dessine manuellement le graphique Radar à l'aide de coordonnées polaires.

### 🔗 Intégration Globale
*   **`src/main/resources/FrontLayout.fxml`** / **`FrontLayoutController.java`** : 
    *   Ajout du lien "Analyse IA" dans la barre de navigation principale pour accéder au module.

---

## 🚀 Comment l'étendre ?

1.  **Ajouter un métier** : Modifiez simplement le fichier `python/job_requirements.json` en ajoutant une nouvelle clé et sa liste de compétences.
2.  **Améliorer l'analyse** : Vous pouvez modifier `skill_gap_analyzer.py` pour utiliser des bibliothèques comme `spacy` ou `nltk` afin de gérer les synonymes (ex: "JS" = "Javascript").
3.  **Graphiques** : La méthode `drawRadarChart` dans le contrôleur Java peut être personnalisée pour changer les couleurs ou la taille du graphique.

---
*Développé pour MentorAI - Système de Mentorat Intelligent.*
