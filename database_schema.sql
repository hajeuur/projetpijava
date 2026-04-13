CREATE DATABASE IF NOT EXISTS `3a36`;
USE `3a36`;

CREATE TABLE IF NOT EXISTS utilisateur (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    mdp VARCHAR(255) NOT NULL,
    role VARCHAR(255) DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS parcours (
    id INT AUTO_INCREMENT PRIMARY KEY,
    type_parcours VARCHAR(255) NOT NULL,
    titre VARCHAR(255) NOT NULL,
    date_debut DATE,
    date_fin DATE,
    description TEXT,
    etablissement VARCHAR(255),
    diplome VARCHAR(255),
    specialite VARCHAR(255),
    entreprise VARCHAR(255),
    poste VARCHAR(255),
    type_contrat VARCHAR(255),
    date_creation DATE,
    date_modification DATE
);

CREATE TABLE IF NOT EXISTS projet (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    description TEXT,
    technologies VARCHAR(255),
    date_debut DATE,
    date_fin DATE,
    date_creation DATE,
    date_modification DATE,
    parcours_id INT,
    FOREIGN KEY (parcours_id) REFERENCES parcours(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ressource (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    url_ressource VARCHAR(255),
    description TEXT,
    type_ressource VARCHAR(255),
    date_creation DATE,
    date_modification DATE,
    projet_id INT,
    FOREIGN KEY (projet_id) REFERENCES projet(id) ON DELETE CASCADE
);
