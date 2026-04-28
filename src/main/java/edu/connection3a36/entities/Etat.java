package edu.connection3a36.entities;

public enum Etat {
    realisee("realisee"),
    encours("encours"),
    Abandonner("Abandonner");

    private final String value;

    Etat(String value) { this.value = value; }
    public String getValue() { return value; }

    public static Etat fromValue(String value) {
        for (Etat e : values())
            if (e.value.equals(value)) return e;
        throw new IllegalArgumentException("Etat inconnu: " + value);
    }
}
