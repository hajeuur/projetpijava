package edu.connection3a36.services;

import edu.connection3a36.entities.Objectif;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour PlanificateurService.
 * Teste le fallback sans Ollama.
 */
class PlanificateurServiceTest {

    private PlanificateurService service;

    @BeforeEach
    void setUp() {
        service = new PlanificateurService();
    }

    @Test
    void testListeVideRetourneResultatVide() {
        PlanificateurService.PlanResultat r = service.genererPlan(new ArrayList<>());
        assertNotNull(r);
        assertTrue(r.plan.isEmpty());
        assertFalse(r.conseil.isBlank());
    }

    @Test
    void testResultatNonNull() {
        List<Objectif> objectifs = new ArrayList<>();
        Objectif o = new Objectif();
        o.setTitre("Objectif test");
        objectifs.add(o);
        PlanificateurService.PlanResultat r = service.genererPlan(objectifs);
        assertNotNull(r);
        assertNotNull(r.plan);
        assertNotNull(r.conseil);
    }

    @Test
    void testCreneauPlanStructure() {
        PlanificateurService.CreneauPlan c = new PlanificateurService.CreneauPlan("9h-10h", "Tache", "Objectif");
        assertEquals("9h-10h", c.heure);
        assertEquals("Tache", c.tache);
        assertEquals("Objectif", c.objectif);
    }

    @Test
    void testSansException() {
        PlanificateurService.PlanResultat r = service.genererPlan(new ArrayList<>());
        assertNotNull(r, "Ne doit pas lever d exception meme si Ollama indisponible");
    }
}
