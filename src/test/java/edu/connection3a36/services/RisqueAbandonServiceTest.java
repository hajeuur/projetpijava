package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour RisqueAbandonService.
 * Teste la logique de fallback sans Ollama.
 */
class RisqueAbandonServiceTest {

    private RisqueAbandonService service;

    @BeforeEach
    void setUp() {
        service = new RisqueAbandonService();
    }

    @Test
    void testAucuneTacheAbandonnee() {
        List<Tache> taches = List.of(tache(Etat.realisee), tache(Etat.encours));
        RisqueAbandonService.RisqueResultat r = service.analyserRisque(taches, 60, LocalDate.now().plusDays(10));
        assertNotNull(r);
        assertFalse(r.risque, "Pas de risque si aucune tache abandonnee");
        assertTrue(r.tachesARelancer.isEmpty());
    }

    @Test
    void testListeVide() {
        RisqueAbandonService.RisqueResultat r = service.analyserRisque(new ArrayList<>(), 0, LocalDate.now().plusDays(5));
        assertNotNull(r);
        assertFalse(r.risque);
    }

    @Test
    void testResultatNonNull() {
        List<Tache> taches = List.of(tache(Etat.Abandonner), tache(Etat.Abandonner));
        RisqueAbandonService.RisqueResultat r = service.analyserRisque(taches, 20, LocalDate.now().plusDays(3));
        assertNotNull(r);
        assertNotNull(r.conseil);
        assertNotNull(r.tachesARelancer);
    }

    @Test
    void testSansException() {
        List<Tache> taches = new ArrayList<>();
        taches.add(tache(Etat.Abandonner));
        taches.add(tache(Etat.realisee));
        RisqueAbandonService.RisqueResultat r = service.analyserRisque(taches, 30, LocalDate.now().plusDays(5));
        assertNotNull(r, "Ne doit pas lever d exception meme si Ollama est indisponible");
    }

    private Tache tache(Etat etat) {
        Tache t = new Tache();
        t.setTitre("Tache test");
        t.setEtat(etat);
        return t;
    }
}
