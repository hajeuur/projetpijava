-- ============================================================
-- Module Objectifs Personnels — MentorAI
-- À exécuter dans la base de données `mentorai`
-- ============================================================

-- Table objectif
CREATE TABLE IF NOT EXISTS objectif (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    titre          VARCHAR(255) NOT NULL,
    description    TEXT,
    datedebut      DATE,
    datefin        DATE,
    statut         ENUM('EnCours', 'Atteint', 'Abandonner') NOT NULL DEFAULT 'EnCours',
    utilisateur_id INT NOT NULL,
    CONSTRAINT fk_objectif_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateur(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table programme
CREATE TABLE IF NOT EXISTS programme (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    titre               VARCHAR(255) NOT NULL,
    dategeneration      DATE NOT NULL,
    score_pourcentage   INT NOT NULL DEFAULT 0,
    meilleure_medaille  ENUM('Bronze', 'Argent', 'Or') DEFAULT NULL,
    objectif_id         INT UNIQUE,
    CONSTRAINT fk_programme_objectif FOREIGN KEY (objectif_id) REFERENCES objectif(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table tache
CREATE TABLE IF NOT EXISTS tache (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    ordre        INT NOT NULL DEFAULT 1,
    titre        VARCHAR(255) NOT NULL,
    description  TEXT,
    etat         ENUM('encours', 'realisee', 'Abandonner') NOT NULL DEFAULT 'encours',
    programme_id INT NOT NULL,
    CONSTRAINT fk_tache_programme FOREIGN KEY (programme_id) REFERENCES programme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table motivation
CREATE TABLE IF NOT EXISTS motivation (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    dategeneration   DATE NOT NULL,
    messagemotivant  TEXT NOT NULL,
    programme_id     INT NOT NULL,
    CONSTRAINT fk_motivation_programme FOREIGN KEY (programme_id) REFERENCES programme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
