package edu.mentorai.entities;

public enum Medaille {
    Bronze("Bronze"),
    Argent("Argent"),
    Or("Or");

    private final String value;

    Medaille(String value) { this.value = value; }
    public String getValue() { return value; }

    public static Medaille fromValue(String value) {
        if (value == null) return null;
        for (Medaille m : values())
            if (m.value.equals(value)) return m;
        return null;
    }
}