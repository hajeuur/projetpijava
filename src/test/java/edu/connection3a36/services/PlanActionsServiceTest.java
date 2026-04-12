package edu.connection3a36.services;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PlanActionsService — validation métier.
 * Correspondance avec tests/Service/PlanActionsManagerTest.php en Symfony.
 */
class PlanActionsServiceTest {

    private PlanActionsService service;

    @BeforeEach
    void setUp() {
        service = new PlanActionsService();
    }

    @Test
    void testValidPlan() {
        PlanActions plan = new PlanActions();
        plan.setDecision("Réunion de suivi");
        plan.setDescription("Description détaillée du plan d'action pour le mentorat.");
        plan.setStatut(Statut.EN_ATTENTE);
        plan.setCategorie(CategorieSortie.PEDAGOGIQUE);

        List<String> errors = service.validate(plan);
        assertTrue(errors.isEmpty(), "Un plan valide ne doit pas avoir d'erreurs");
    }

    @Test
    void testPlanWithoutDecision() {
        PlanActions plan = new PlanActions();
        plan.setDecision("");
        plan.setDescription("Description valide de plus de 10 caractères.");
        plan.setStatut(Statut.EN_COURS);
        plan.setCategorie(CategorieSortie.STRATEGIQUE);

        List<String> errors = service.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("décision")));
    }

    @Test
    void testPlanWithShortDecision() {
        PlanActions plan = new PlanActions();
        plan.setDecision("AB"); // Trop court (< 5)
        plan.setDescription("Description valide de plus de 10 caractères.");
        plan.setStatut(Statut.EN_ATTENTE);
        plan.setCategorie(CategorieSortie.ADMINISTRATIVE);

        List<String> errors = service.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("5 caractères")));
    }

    @Test
    void testPlanWithShortDescription() {
        PlanActions plan = new PlanActions();
        plan.setDecision("Décision valide");
        plan.setDescription("Court"); // Trop court (< 10)
        plan.setStatut(Statut.FINI);
        plan.setCategorie(CategorieSortie.PEDAGOGIQUE);

        List<String> errors = service.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("10 caractères")));
    }

    @Test
    void testPlanWithoutStatut() {
        PlanActions plan = new PlanActions();
        plan.setDecision("Décision valide");
        plan.setDescription("Description valide de plus de 10 caractères.");
        plan.setStatut(null);
        plan.setCategorie(CategorieSortie.PEDAGOGIQUE);

        List<String> errors = service.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("statut")));
    }

    @Test
    void testPlanWithoutCategorie() {
        PlanActions plan = new PlanActions();
        plan.setDecision("Décision valide");
        plan.setDescription("Description valide de plus de 10 caractères.");
        plan.setStatut(Statut.EN_ATTENTE);
        plan.setCategorie(null);

        List<String> errors = service.validate(plan);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("catégorie")));
    }

    @Test
    void testPlanMultipleErrors() {
        PlanActions plan = new PlanActions();
        plan.setDecision("");       // Vide
        plan.setDescription("");    // Vide
        plan.setStatut(null);       // Null
        plan.setCategorie(null);    // Null

        List<String> errors = service.validate(plan);
        assertEquals(4, errors.size(), "Doit avoir exactement 4 erreurs");
    }
}
