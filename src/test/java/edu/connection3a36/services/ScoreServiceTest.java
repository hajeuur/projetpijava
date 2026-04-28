package edu.connection3a36.services;

import edu.connection3a36.entities.Etat;
import edu.connection3a36.entities.Medaille;
import edu.connection3a36.entities.Statutobj;
import edu.connection3a36.entities.Tache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour ScoreService — calcul du score, médailles et statut objectif.
 * Aucune connexion BDD : on teste la logique pure.
 */
class ScoreServiceTest {

    private ScoreService service;

    @BeforeEach
    void setUp() {
        service = new ScoreService();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALCUL DU SCORE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testScoreListeVide() {
        assertEquals(0, service.calculerScore(new ArrayList<>()),
                "Score doit etre 0 si aucune tache");
    }

    @Test
    void testScoreListeNull() {
        assertEquals(0, service.calculerScore(null),
                "Score doit etre 0 si liste null");
    }

    @Test
    void testScoreAucuneTacheRealisee() {
        List<Tache> taches = List.of(
                tache(Etat.Abandonner),
                tache(Etat.Abandonner),
                tache(Etat.Abandonner)
        );
        assertEquals(0, service.calculerScore(taches),
                "Score doit etre 0 si aucune tache realisee");
    }

    @Test
    void testScoreToutesRealisees() {
        List<Tache> taches = List.of(
                tache(Etat.realisee),
                tache(Etat.realisee),
                tache(Etat.realisee)
        );
        assertEquals(100, service.calculerScore(taches),
                "Score doit etre 100 si toutes les taches sont realisees");
    }

    @Test
    void testScoreMoitieRealisee() {
        List<Tache> taches = List.of(
                tache(Etat.realisee),
                tache(Etat.realisee),
                tache(Etat.Abandonner),
                tache(Etat.Abandonner)
        );
        assertEquals(50, service.calculerScore(taches),
                "Score doit etre 50 si la moitie des taches est realisee");
    }

    @Test
    void testScoreUneTacheRealisee() {
        List<Tache> taches = List.of(
                tache(Etat.realisee),
                tache(Etat.Abandonner),
                tache(Etat.Abandonner),
                tache(Etat.Abandonner)
        );
        assertEquals(25, service.calculerScore(taches),
                "Score doit etre 25 si 1 tache sur 4 est realisee");
    }

    @Test
    void testScoreArrondi() {
        List<Tache> taches = List.of(
                tache(Etat.realisee),
                tache(Etat.Abandonner),
                tache(Etat.Abandonner)
        );
        // 1/3 = 33.33... → arrondi à 33
        assertEquals(33, service.calculerScore(taches));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ATTRIBUTION DES MÉDAILLES
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testMedailleScoreZero() {
        assertNull(service.attribuerMedaille(0),
                "Pas de medaille pour un score de 0");
    }

    @Test
    void testMedailleBronze() {
        assertEquals(Medaille.Bronze, service.attribuerMedaille(1));
        assertEquals(Medaille.Bronze, service.attribuerMedaille(25));
        assertEquals(Medaille.Bronze, service.attribuerMedaille(49));
    }

    @Test
    void testMedailleArgent() {
        assertEquals(Medaille.Argent, service.attribuerMedaille(50));
        assertEquals(Medaille.Argent, service.attribuerMedaille(65));
        assertEquals(Medaille.Argent, service.attribuerMedaille(79));
    }

    @Test
    void testMedailleOr() {
        assertEquals(Medaille.Or, service.attribuerMedaille(80));
        assertEquals(Medaille.Or, service.attribuerMedaille(95));
        assertEquals(Medaille.Or, service.attribuerMedaille(100));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STATUT OBJECTIF SELON AVANCEMENT
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testStatutAucuneTacheRealisee() {
        List<Tache> taches = List.of(tache(Etat.Abandonner), tache(Etat.Abandonner));
        assertEquals(Statutobj.Abandonner, service.calculerStatutObjectif(taches, 0),
                "Statut doit etre Abandonner si score = 0");
    }

    @Test
    void testStatutToutesRealisees() {
        List<Tache> taches = List.of(tache(Etat.realisee), tache(Etat.realisee));
        assertEquals(Statutobj.Atteint, service.calculerStatutObjectif(taches, 100),
                "Statut doit etre Atteint si score = 100");
    }

    @Test
    void testStatutEnCours() {
        List<Tache> taches = List.of(tache(Etat.realisee), tache(Etat.Abandonner));
        assertEquals(Statutobj.EnCours, service.calculerStatutObjectif(taches, 50),
                "Statut doit etre EnCours si score entre 1 et 99");
    }

    @Test
    void testStatutListeVide() {
        assertEquals(Statutobj.EnCours, service.calculerStatutObjectif(new ArrayList<>(), 0),
                "Statut doit etre EnCours si aucune tache");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EMOJI ET COULEUR MÉDAILLE
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testEmojiMedailleNull() {
        assertEquals("—", ScoreService.emojiMedaille(null));
    }

    @Test
    void testEmojiMedailleBronze() {
        assertTrue(ScoreService.emojiMedaille(Medaille.Bronze).contains("Bronze"));
    }

    @Test
    void testEmojiMedailleArgent() {
        assertTrue(ScoreService.emojiMedaille(Medaille.Argent).contains("Argent"));
    }

    @Test
    void testEmojiMedailleOr() {
        assertTrue(ScoreService.emojiMedaille(Medaille.Or).contains("Or"));
    }

    @Test
    void testCouleurMedailleNull() {
        assertNotNull(ScoreService.couleurMedaille(null));
        assertFalse(ScoreService.couleurMedaille(null).isBlank());
    }

    @Test
    void testCouleurMedailleOr() {
        assertEquals("#ffd700", ScoreService.couleurMedaille(Medaille.Or));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private Tache tache(Etat etat) {
        Tache t = new Tache();
        t.setEtat(etat);
        return t;
    }
}
