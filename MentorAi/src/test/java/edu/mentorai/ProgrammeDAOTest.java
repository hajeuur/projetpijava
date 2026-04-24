package edu.mentorai;

import edu.mentorai.entities.Medaille;
import edu.mentorai.entities.Programme;
import org.junit.jupiter.api.*;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProgrammeDAOTest {

    private static ProgrammeDAO programmeDAO;
    private static int testProgrammeId;

    @BeforeAll
    static void setUp() {
        programmeDAO = new ProgrammeDAO();
    }

    // Test 1 — Créer un programme
    @Test
    @Order(1)
    void testCreerProgramme() throws Exception {
        Programme prog = new Programme();
        prog.setTitre("Programme Test JUnit");
        prog.setDategeneration(LocalDate.now());
        prog.setScorePourcentage(0);
        programmeDAO.save(prog);

        testProgrammeId = prog.getId();
        assertTrue(testProgrammeId > 0,
                "Le programme doit avoir un ID positif");

        System.out.println("Test 1 OK - Programme cree ID: " + testProgrammeId);
    }

    // Test 2 — Lire un programme par ID
    @Test
    @Order(2)
    void testLireProgrammeParId() throws Exception {
        Programme prog = programmeDAO.findById(testProgrammeId);

        assertNotNull(prog, "Le programme doit exister");
        assertEquals("Programme Test JUnit", prog.getTitre());
        assertEquals(0, prog.getScorePourcentage());

        System.out.println("Test 2 OK - Programme trouve: " + prog.getTitre());
    }

    // Test 3 — Mettre à jour le score
    @Test
    @Order(3)
    void testMettreAJourScore() throws Exception {
        programmeDAO.updateScore(testProgrammeId, 75, Medaille.Argent);

        Programme updated = programmeDAO.findById(testProgrammeId);
        assertEquals(75, updated.getScorePourcentage(),
                "Le score doit etre 75");
        assertEquals(Medaille.Argent, updated.getMeilleureMedaille(),
                "La medaille doit etre Argent");

        System.out.println("Test 3 OK - Score: " + updated.getScorePourcentage()
                + " Medaille: " + updated.getMeilleureMedaille().getValue());
    }

    // Test 4 — Score 100% = médaille Or
    @Test
    @Order(4)
    void testScoreCentPourcentMedailleOr() throws Exception {
        programmeDAO.updateScore(testProgrammeId, 100, Medaille.Or);

        Programme updated = programmeDAO.findById(testProgrammeId);
        assertEquals(100, updated.getScorePourcentage());
        assertEquals(Medaille.Or, updated.getMeilleureMedaille());

        System.out.println("Test 4 OK - Score 100% Medaille Or");
    }

    // Test 5 — Supprimer un programme
    @Test
    @Order(5)
    void testSupprimerProgramme() throws Exception {
        programmeDAO.delete(testProgrammeId);

        Programme deleted = programmeDAO.findById(testProgrammeId);
        assertNull(deleted,
                "Le programme doit etre null apres suppression");

        System.out.println("Test 5 OK - Programme supprime");
    }
}