package edu.connection3a36.services;

import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Statutobj;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ObjectifService — validation métier.
 * Aucune connexion BDD requise : on teste uniquement la logique de validation.
 */
class ObjectifServiceTest {

    private ObjectifService service;

    @BeforeEach
    void setUp() {
        service = new ObjectifService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CAS VALIDES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testObjectifValide() {
        Objectif o = new Objectif("Apprendre Java", "Description valide",
                LocalDate.now(), LocalDate.now().plusMonths(1), 1);
        List<String> erreurs = service.validate(o);
        assertTrue(erreurs.isEmpty(), "Un objectif valide ne doit pas avoir d erreurs");
    }

    @Test
    void testObjectifTitreMinimal() {
        Objectif o = new Objectif("ABC", "Description",
                LocalDate.now(), LocalDate.now().plusDays(10), 1);
        List<String> erreurs = service.validate(o);
        assertTrue(erreurs.isEmpty(), "Un titre de 3 caractères est valide");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TITRE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTitreVide() {
        Objectif o = new Objectif("", "Description",
                LocalDate.now(), LocalDate.now().plusMonths(1), 1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("titre")));
    }

    @Test
    void testTitreNull() {
        Objectif o = new Objectif();
        o.setTitre(null);
        o.setDatedebut(LocalDate.now());
        o.setDatefin(LocalDate.now().plusMonths(1));
        o.setUtilisateurId(1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("titre")));
    }

    @Test
    void testTitreTropCourt() {
        Objectif o = new Objectif("AB", "Description",
                LocalDate.now(), LocalDate.now().plusMonths(1), 1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("3 caractères")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DATES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDateDebutNull() {
        Objectif o = new Objectif("Titre valide", "Description",
                null, LocalDate.now().plusMonths(1), 1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("debut")));
    }

    @Test
    void testDateFinNull() {
        Objectif o = new Objectif("Titre valide", "Description",
                LocalDate.now(), null, 1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("fin")));
    }

    @Test
    void testDateFinAvantDateDebut() {
        Objectif o = new Objectif("Titre valide", "Description",
                LocalDate.now().plusMonths(1), LocalDate.now(), 1);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("apres")));
    }

    @Test
    void testDateDebutEgaleADateFin() {
        LocalDate date = LocalDate.now();
        Objectif o = new Objectif("Titre valide", "Description", date, date, 1);
        // Même date : fin n'est pas AVANT début → valide
        List<String> erreurs = service.validate(o);
        assertTrue(erreurs.stream().noneMatch(e -> e.contains("apres")),
                "Debut = Fin est acceptable");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILISATEUR
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testUtilisateurIdInvalide() {
        Objectif o = new Objectif("Titre valide", "Description",
                LocalDate.now(), LocalDate.now().plusMonths(1), 0);
        List<String> erreurs = service.validate(o);
        assertFalse(erreurs.isEmpty());
        assertTrue(erreurs.stream().anyMatch(e -> e.contains("utilisateur")));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ERREURS MULTIPLES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testErreursMultiples() {
        Objectif o = new Objectif();
        o.setTitre("");
        o.setDatedebut(null);
        o.setDatefin(null);
        o.setUtilisateurId(0);
        List<String> erreurs = service.validate(o);
        assertTrue(erreurs.size() >= 3, "Doit avoir au moins 3 erreurs");
    }
}
