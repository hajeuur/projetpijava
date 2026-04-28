package edu.connection3a36.entities;

public enum Statutobj {
    EnCours("EnCours"),
    Atteint("Atteint"),
    Abandonner("Abandonner");

    private final String value;

    Statutobj(String value) { this.value = value; }
    public String getValue() { return value; }

    public static Statutobj fromValue(String value) {
        for (Statutobj s : values())
            if (s.value.equals(value)) return s;
        throw new IllegalArgumentException("Statut inconnu: " + value);
    }
}
