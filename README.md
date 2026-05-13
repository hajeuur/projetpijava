# MentorAI 🚀 - Se former autrement

MentorAI est une plateforme innovante conçue pour accompagner les étudiants de l'**ESPRIT** (Tunisie) dans leur réussite académique. En intégrant l'Intelligence Artificielle au cœur de l'apprentissage, MentorAI répond aux défis de compréhension, de motivation et d'organisation.

---

## 📖 Sommaire
1. [Introduction](#introduction)
2. [Fonctionnalités Principales](#fonctionnalités-principales)
3. [Modules du Projet](#modules-du-projet)
4. [Architecture du Projet (MVC)](#architecture-du-projet-mvc)
5. [Technologies Utilisées](#technologies-utilisées)
6. [Installation et Utilisation](#installation-et-utilisation)
7. [L'Équipe](#léquipe)

---

## 🌟 Introduction
Dans le milieu exigeant des formations d'ingénierie, les étudiants font face à une pression académique élevée et une surcharge cognitive. MentorAI propose un suivi intelligent et individualisé pour chaque étudiant, tout en offrant des outils décisionnels puissants pour les enseignants et l'administration.

---

## 🛠 Fonctionnalités Principales
- **Apprentissage personnalisé** : Adaptation du contenu selon le profil psychologique et le rythme de l'étudiant.
- **Assistance intelligente** : Chatbot IA intégré et analyse de performance en temps réel.
- **Organisation & Productivité** : Suivi des objectifs (Goal Tracker) avec système de scoring et médailles.
- **Aide à la décision** : Analyse des données académiques pour identifier les risques de décrochage.

---

## 📦 Modules du Projet

### 01. Gestion des Utilisateurs
- Authentification sécurisée et gestion des profils.
- Attribution des rôles (Étudiant, Enseignant, Administrateur).

### 02. Dashboard Enseignants & Admin
- Analyse comportementale et académique.
- Évaluation de l'état global (performance, risque, engagement).
- Recommandations stratégiques basées sur l'IA.

### 03. Goal Tracker & Productivité
- Définition d'objectifs et suivi de progression via des tâches.
- Gamification : Attribution de scores et de médailles.

### 04. Portfolio & Orientation
- Accompagnement dans la construction du parcours professionnel.
- Gestion des projets et des compétences acquises.

### 05. Psychologie & Révision IA
- Personnalisation selon le style d'apprentissage et l'humeur.
- Résumés de cours automatiques et révisions adaptées.

### 06. Feedback & Amélioration
- Système de retour utilisateur pour améliorer continuellement l'agent intelligent.

---

## 🏗 Architecture du Projet (MVC)
Le projet suit une architecture **Modèle-Vue-Contrôleur** pour assurer une séparation claire des responsabilités :

### 🔹 Modèle (Model)
*   **Java (JavaFX) :** Situé dans `edu.connection3a36.entities`. Définit les structures de données (User, Projet, Parcours, etc.).
*   **PHP (Symfony) :** Situé dans `src/Entity`. Gère la persistance des données avec Doctrine ORM.

### 🔹 Vue (View)
*   **Java (JavaFX) :** Fichiers `.fxml` situés dans `src/main/resources`. Définit l'interface utilisateur graphique.
*   **PHP (Symfony) :** Templates `.html.twig` situés dans `templates/`. Gère le rendu HTML/CSS pour le web.

### 🔹 Contrôleur (Controller)
*   **Java (JavaFX) :** Situé dans `edu.connection3a36.controllers`. Gère les interactions utilisateur et fait le lien entre la vue et les services.
*   **PHP (Symfony) :** Situé dans `src/Controller`. Gère les requêtes HTTP et retourne les vues Twig.

### 🔹 Services & Outils (Extra Layers)
*   **Services :** `edu.connection3a36.services` (Java) et `src/Service` (PHP). Contient la logique métier complexe (IA, transcription, API).
*   **Tools/Utils :** Utilitaires pour la gestion de session, la connexion BD et le formatage.

---

## 💻 Technologies Utilisées

### Backend & Logiciel
- **Java / JavaFX** : Interface logicielle robuste pour la gestion interne.
- **PHP / Symfony** : Plateforme web pour l'accès étudiant.
- **Twig** : Moteur de template pour le rendu web dynamique.

### Intelligence Artificielle
- **Groq API (Llama 3.3)** : Moteur d'IA pour les recommandations et les discussions.
- **Whisper (via Groq)** : Transcription vocale (Speech-to-Text) pour les entretiens.
- **MediaPipe** : Suivi des mains (Hand Tracking) pour le contrôle gestuel dans les jeux.

---

## 🚀 Installation et Utilisation

### Prérequis
- Java JDK 17+
- PHP 8.1+ & Composer
- Serveur MySQL/MariaDB

### Lancement de la partie Web (Symfony)
1. Clonez le projet.
2. Installez les dépendances : `composer install`
3. Configurez le fichier `.env` avec vos accès base de données.
4. Lancez le serveur : `php -S localhost:8000 -t public`

### Lancement de la partie Logiciel (JavaFX)
1. Ouvrez le projet dans votre IDE (IntelliJ ou Eclipse).
2. Configurez le fichier `config.properties` avec votre `GROQ_API_KEY`.
3. Lancez la classe `App.java`.

---

## 👥 L'Équipe
Ce projet a été réalisé par :
- **Hajer Hmaied**
- **Mariem Amdouni**
- **Imen Azouzi**
- **Arlsen Amira**
- **Eya Tlili**
- **Amal Mokdad**

---
© 2026 MentorAI - Se former autrement.