package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour TacheService — validation métier.
 */
class TacheServiceTest {

    private TacheService service;

    @BeforeEach
    void setUp() {
        service = new TacheService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAS VALIDES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTacheValide() {
        Tache t = new Tache(1, "Lire la documentation", "Lire les docs officielles", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertTrue(erreurs.isEmpty(), "Une tache valide ne doit pas avoir d erreurs");
    }

    @Test
    void testTacheSansDescription() {
        Tache t = new Tache(1, "Titre valide", "", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertTrue(erreurs.isEmpty(), "La description est optionnelle");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TITRE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTitreVide() {
        Tache t = new Tache(1, "", "Description", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("titre")));
    }

    @Test
    void testTitreNull() {
        Tache t = new Tache();
        t.setTitre(null);
        t.setOrdre(1);
        t.setProgrammeId(5);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("titre")));
    }

    @Test
    void testTitreTropCourt() {
        Tache t = new Tache(1, "A", "Description", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("2 caractères")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ORDRE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testOrdreZero() {
        Tache t = new Tache(0, "Titre valide", "Description", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("ordre")));
    }

    @Test
    void testOrdreNegatif() {
        Tache t = new Tache(-1, "Titre valide", "Description", Etat.Abandonner, 5);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("ordre")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRAMME
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testProgrammeIdInvalide() {
        Tache t = new Tache(1, "Titre valide", "Description", Etat.Abandonner, 0);
        List<String> erreurs = service.validate(t);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("programme")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ERREURS MULTIPLES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testErreursMultiples() {
        Tache t = new Tache(0, "", "Description", Etat.Abandonner, 0);
        List<String> erreurs = service.validate(t);
        assertTrue(erreurs.size() >= 3, "Doit avoir au moins 3 erreurs");
    }
}
