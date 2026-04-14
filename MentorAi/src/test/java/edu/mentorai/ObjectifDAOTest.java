package edu.mentorai;

import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Programme;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.interfaces.ObjectifDAO;
import edu.mentorai.interfaces.ProgrammeDAO;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ObjectifDAOTest {

    private static ObjectifDAO objectifDAO;
    private static ProgrammeDAO programmeDAO;
    private static int testObjectifId;
    private static int testProgrammeId;

    @BeforeAll
    static void setUp() {
        objectifDAO = new ObjectifDAO();
        programmeDAO = new ProgrammeDAO();
    }

    // Test 1 — Créer un objectif
    @Test
    @Order(1)
    void testCreerObjectif() throws Exception {
        // Créer le programme associé
        Programme prog = new Programme();
        prog.setTitre("Programme Test JUnit");
        prog.setDategeneration(LocalDate.now());
        prog.setScorePourcentage(0);
        programmeDAO.save(prog);
        testProgrammeId = prog.getId();

        assertTrue(testProgrammeId > 0,
                "Le programme doit avoir un ID positif");

        // Créer l'objectif
        Objectif obj = new Objectif();
        obj.setTitre("Objectif Test JUnit");
        obj.setDescription("Description test unitaire");
        obj.setDatedebut(LocalDate.now());
        obj.setDatefin(LocalDate.now().plusMonths(1));
        obj.setStatut(Statutobj.EnCours);
        obj.setProgramme(prog);
        obj.setUtilisateurId(1);
        objectifDAO.save(obj);

        testObjectifId = obj.getId();
        assertTrue(testObjectifId > 0,
                "L'objectif doit avoir un ID positif apres save");

        System.out.println("Test 1 OK - Objectif cree avec ID: " + testObjectifId);
    }

    // Test 2 — Lire un objectif par ID
    @Test
    @Order(2)
    void testLireObjectifParId() throws Exception {
        Objectif obj = objectifDAO.findById(testObjectifId);

        assertNotNull(obj, "L'objectif doit exister en base");
        assertEquals("Objectif Test JUnit", obj.getTitre(),
                "Le titre doit correspondre");
        assertEquals(Statutobj.EnCours, obj.getStatut(),
                "Le statut doit etre EnCours");
        assertEquals(1, obj.getUtilisateurId(),
                "L'utilisateur ID doit etre 1");

        System.out.println("Test 2 OK - Objectif trouve: " + obj.getTitre());
    }

    // Test 3 — Lister les objectifs par utilisateur
    @Test
    @Order(3)
    void testListerObjectifsParUtilisateur() throws Exception {
        List<Objectif> list = objectifDAO.findByUtilisateur(1);

        assertNotNull(list, "La liste ne doit pas etre null");
        assertFalse(list.isEmpty(),
                "La liste doit contenir au moins un objectif");
        assertTrue(list.stream()
                        .anyMatch(o -> o.getId() == testObjectifId),
                "La liste doit contenir notre objectif de test");

        System.out.println("Test 3 OK - " + list.size() + " objectifs trouves");
    }

    // Test 4 — Modifier un objectif
    @Test
    @Order(4)
    void testModifierObjectif() throws Exception {
        Objectif obj = objectifDAO.findById(testObjectifId);
        assertNotNull(obj);

        obj.setTitre("Objectif Modifie JUnit");
        obj.setStatut(Statutobj.Atteint);
        objectifDAO.update(obj);

        Objectif updated = objectifDAO.findById(testObjectifId);
        assertEquals("Objectif Modifie JUnit", updated.getTitre(),
                "Le titre modifie doit etre sauvegarde");
        assertEquals(Statutobj.Atteint, updated.getStatut(),
                "Le statut modifie doit etre sauvegarde");

        System.out.println("Test 4 OK - Objectif modifie: " + updated.getTitre());
    }

    // Test 5 — Rechercher par titre
    @Test
    @Order(5)
    void testRechercherParTitre() throws Exception {
        List<Objectif> results = objectifDAO.searchByTitre("Modifie");

        assertNotNull(results);
        assertFalse(results.isEmpty(),
                "La recherche doit retourner des resultats");
        assertTrue(results.stream()
                        .anyMatch(o -> o.getTitre().contains("Modifie")),
                "Les resultats doivent contenir le mot recherche");

        System.out.println("Test 5 OK - " + results.size() + " resultats trouves");
    }

    // Test 6 — Vérifier statut EnCours
    @Test
    @Order(6)
    void testStatutEnCours() throws Exception {
        Objectif obj = objectifDAO.findById(testObjectifId);
        assertNotNull(obj);

        obj.setStatut(Statutobj.EnCours);
        objectifDAO.update(obj);

        Objectif updated = objectifDAO.findById(testObjectifId);
        assertEquals(Statutobj.EnCours, updated.getStatut(),
                "Le statut doit etre EnCours");

        System.out.println("Test 6 OK - Statut EnCours verifie");
    }

    // Test 7 — Supprimer un objectif
    @Test
    @Order(7)
    void testSupprimerObjectif() throws Exception {
        objectifDAO.delete(testObjectifId);

        Objectif deleted = objectifDAO.findById(testObjectifId);
        assertNull(deleted,
                "L'objectif doit etre null apres suppression");

        // Nettoyer le programme
        programmeDAO.delete(testProgrammeId);

        System.out.println("Test 7 OK - Objectif supprime");
    }
}