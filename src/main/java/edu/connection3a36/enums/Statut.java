package edu.connection3a36.enums;

public enum Statut {
    EN_ATTENTE("En attente"),
    EN_COURS("En cours"),
    FINI("Fini"),
    REJETE("Rejeté");

    private final String label;

    Statut(String label) {
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
