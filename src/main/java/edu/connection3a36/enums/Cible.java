package edu.connection3a36.enums;

public enum Cible {
    ETUDIANT("Étudiant"),
    CLASSE("Classe"),
    ENSEIGNANT("Enseignant"),
    ADMINISTRATEUR("Administrateur");

    private final String label;

    Cible(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
