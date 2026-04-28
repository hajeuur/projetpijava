package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Tache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ResumeService.
 * Ollama non requis — on teste le fallback et la structure du resultat.
 */
class ResumeServiceTest {

    private ResumeService service;

    @BeforeEach
    void setUp() {
        service = new ResumeService();
    }

    @Test
    void testResultatNonNull() {
        ResumeService.ResumeResultat r = new ResumeService.ResumeResultat("resume", "point", "conseil");
        assertNotNull(r.resume);
        assertNotNull(r.pointPositif);
        assertNotNull(r.conseil);
    }

    @Test
    void testGenererResumeListeVide() {
        ResumeService.ResumeResultat r = service.genererResume("Objectif test", 0, new ArrayList<>());
        assertNotNull(r);
        assertFalse(r.resume.isBlank());
    }

    @Test
    void testGenererResumeSansException() {
        List<Tache> taches = new ArrayList<>();
        Tache t = new Tache();
        t.setTitre("Tache test");
        t.setEtat(Etat.realisee);
        taches.add(t);
        ResumeService.ResumeResultat r = service.genererResume("Apprendre Java", 50, taches);
        assertNotNull(r, "Le resultat ne doit pas etre null meme si Ollama est indisponible");
        assertNotNull(r.resume);
        assertNotNull(r.pointPositif);
        assertNotNull(r.conseil);
    }

    @Test
    void testGenererResumeScore100() {
        List<Tache> taches = List.of(tache(Etat.realisee), tache(Etat.realisee));
        ResumeService.ResumeResultat r = service.genererResume("Objectif complet", 100, taches);
        assertNotNull(r);
        assertFalse(r.conseil.isBlank());
    }

    private Tache tache(Etat etat) {
        Tache t = new Tache();
        t.setTitre("Tache");
        t.setEtat(etat);
        return t;
    }
}
