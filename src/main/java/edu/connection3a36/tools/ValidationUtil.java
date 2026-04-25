package edu.connection3a36.tools;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitaire de validation des champs de formulaire.
 * Centralise les contrôles pour éviter la duplication dans les services.
 */
public class ValidationUtil {

    /**
     * Valide qu'une chaîne n'est pas vide
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Valide la longueur minimale
     */
    public static boolean hasMinLength(String value, int min) {
        return value != null && value.trim().length() >= min;
    }

    /**
     * Valide la longueur maximale
     */
    public static boolean hasMaxLength(String value, int max) {
        return value == null || value.trim().length() <= max;
    }

    /**
     * Valide qu'un objet n'est pas null
     */
    public static boolean isNotNull(Object value) {
        return value != null;
    }

    /**
     * Valide un champ texte avec toutes les règles
     * @return message d'erreur ou null si OK
     */
    public static String validateTextField(String value, String fieldName, boolean required, int minLength, int maxLength) {
        if (required && !isNotBlank(value)) {
            return fieldName + " est obligatoire";
        }
        if (isNotBlank(value) && !hasMinLength(value, minLength)) {
            return fieldName + " doit contenir au moins " + minLength + " caractères";
        }
        if (isNotBlank(value) && !hasMaxLength(value, maxLength)) {
            return fieldName + " ne peut pas dépasser " + maxLength + " caractères";
        }
        return null;
    }

    /**
     * Valide un email basique
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
