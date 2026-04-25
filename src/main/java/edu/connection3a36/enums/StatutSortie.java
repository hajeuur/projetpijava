package edu.connection3a36.enums;

public enum StatutSortie {
    NOUVEAU("Nouveau"),
    PLANIFIE("Planifié"),
    TRAITEE("Traitée"),
    IGNORE("Ignoré");

    private final String label;

    StatutSortie(String label) {
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
