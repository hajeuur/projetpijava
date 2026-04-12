package edu.connection3a36.enums;

public enum Criticite {
    FAIBLE("Faible"),
    MOYEN("Moyen"),
    ELEVE("Élevé");

    private final String label;

    Criticite(String label) {
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
