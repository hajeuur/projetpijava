package edu.connection3a36.enums;

public enum CategorieSortie {
    PEDAGOGIQUE("Pédagogique"),
    STRATEGIQUE("Stratégique"),
    ADMINISTRATIVE("Administrative");

    private final String label;

    CategorieSortie(String label) {
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
