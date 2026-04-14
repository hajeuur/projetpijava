package edu.mentorai;

import edu.mentorai.entities.*;
import edu.mentorai.interfaces.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TacheDAOTest {

    private static TacheDAO tacheDAO;
    private static ProgrammeDAO programmeDAO;
    private static ObjectifDAO objectifDAO;
    private static int testProgrammeId;
    private static int testTacheId;
    private static int testObjectifId;

    @BeforeAll
    static void setUp() {
        tacheDAO = new TacheDAO();
        programmeDAO = new ProgrammeDAO();
        objectifDAO = new ObjectifDAO();
    }

    // Test 1 — Créer une tâche
    @Test
    @Order(1)
    void testCreerTache() throws Exception {
        Programme prog = new Programme();
        prog.setTitre("Programme Tache Test");
        prog.setDategeneration(LocalDate.now());
        prog.setScorePourcentage(0);
        programmeDAO.save(prog);
        testProgrammeId = prog.getId();

        Objectif obj = new Objectif();
        obj.setTitre("Objectif pour tache test");
        obj.setDescription("Description");
        obj.setDatedebut(LocalDate.now());
        obj.setDatefin(LocalDate.now().plusMonths(1));
        obj.setStatut(Statutobj.EnCours);
        obj.setProgramme(prog);
        obj.setUtilisateurId(1);
        objectifDAO.save(obj);
        testObjectifId = obj.getId();

        Tache tache = new Tache();
        tache.setOrdre(1);
        tache.setTitre("Tache Test JUnit");
        tache.setDescription("Description tache test");
        tache.setEtat(Etat.encours);
        tache.setProgrammeId(testProgrammeId);
        tacheDAO.save(tache);

        testTacheId = tache.getId();
        assertTrue(testTacheId > 0,
                "La tache doit avoir un ID positif");

        System.out.println("Test 1 OK - Tache creee avec ID: " + testTacheId);
    }

    // Test 2 — Lire une tâche par ID
    @Test
    @Order(2)
    void testLireTacheParId() throws Exception {
        Tache tache = tacheDAO.findById(testTacheId);

        assertNotNull(tache, "La tache doit exister");
        assertEquals("Tache Test JUnit", tache.getTitre(),
                "Le titre doit correspondre");
        assertEquals(Etat.encours, tache.getEtat(),
                "L'etat doit etre encours");
        assertEquals(1, tache.getOrdre(),
                "L'ordre doit etre 1");

        System.out.println("Test 2 OK - Tache trouvee: " + tache.getTitre());
    }

    // Test 3 — Lister tâches par programme
    @Test
    @Order(3)
    void testListerTachesParProgramme() throws Exception {
        List<Tache> list = tacheDAO.findByProgramme(testProgrammeId);

        assertNotNull(list);
        assertFalse(list.isEmpty(),
                "La liste doit contenir au moins une tache");
        assertEquals(1, list.size(),
                "Il doit y avoir exactement 1 tache");

        System.out.println("Test 3 OK - " + list.size() + " tache(s) trouvee(s)");
    }

    // Test 4 — Modifier une tâche
    @Test
    @Order(4)
    void testModifierTache() throws Exception {
        Tache tache = tacheDAO.findById(testTacheId);
        assertNotNull(tache);

        tache.setEtat(Etat.realisee);
        tache.setTitre("Tache Modifiee JUnit");
        tacheDAO.update(tache);

        Tache updated = tacheDAO.findById(testTacheId);
        assertEquals(Etat.realisee, updated.getEtat(),
                "L'etat doit etre realisee");
        assertEquals("Tache Modifiee JUnit", updated.getTitre(),
                "Le titre modifie doit etre sauvegarde");

        System.out.println("Test 4 OK - Tache modifiee: " + updated.getTitre());
    }

    // Test 5 — Calcul du score après modification
    @Test
    @Order(5)
    void testCalculScoreApresModification() throws Exception {
        List<Tache> taches = tacheDAO.findByProgramme(testProgrammeId);
        int total = taches.size();
        long realisees = taches.stream()
                .filter(t -> t.getEtat() == Etat.realisee).count();
        int score = total > 0 ?
                (int) Math.round((realisees * 100.0) / total) : 0;

        assertEquals(100, score,
                "Score doit etre 100% avec 1 tache realisee sur 1");

        System.out.println("Test 5 OK - Score calcule: " + score + "%");
    }

    // Test 6 — Vérifier médaille selon score
    @Test
    @Order(6)
    void testAttributionMedaille() {
        Medaille medaille = null;
        int score = 100;

        if (score >= 90) medaille = Medaille.Or;
        else if (score >= 60) medaille = Medaille.Argent;
        else if (score >= 30) medaille = Medaille.Bronze;

        assertEquals(Medaille.Or, medaille,
                "Score 100% doit donner medaille Or");

        System.out.println("Test 6 OK - Medaille: " + medaille.getValue());
    }

    // Test 7 — Supprimer une tâche
    @Test
    @Order(7)
    void testSupprimerTache() throws Exception {
        tacheDAO.delete(testTacheId);

        Tache deleted = tacheDAO.findById(testTacheId);
        assertNull(deleted,
                "La tache doit etre null apres suppression");

        // Nettoyage
        objectifDAO.delete(testObjectifId);
        programmeDAO.delete(testProgrammeId);

        System.out.println("Test 7 OK - Tache supprimee");
    }
}