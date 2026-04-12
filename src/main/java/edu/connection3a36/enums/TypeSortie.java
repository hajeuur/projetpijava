package edu.connection3a36.enums;

public enum TypeSortie {
    ALERTE("Alerte"),
    PREDICTION("Prédiction"),
    RECOMMANDATION("Recommandation"),
    ANALYSE("Analyse");

    private final String label;

    TypeSortie(String label) {
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
