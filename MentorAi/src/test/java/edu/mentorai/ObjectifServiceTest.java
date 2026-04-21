package edu.mentorai;

import edu.mentorai.entities.Objectif;
import edu.mentorai.entities.Programme;
import edu.mentorai.entities.Statutobj;
import edu.mentorai.services.ObjectifService;
import edu.mentorai.services.ProgrammeService;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ObjectifServiceTest {

    private static ObjectifService objectifService;
    private static ProgrammeService programmeService;
    private static int savedId;
    private static int programmeId;

    @BeforeAll
    static void setup() throws SQLException {
        objectifService = new ObjectifService();
        programmeService = new ProgrammeService();

        Programme programme = new Programme();
        programme.setTitre("Programme Test");
        programme.setDategeneration(LocalDate.now());
        programme.setScorePourcentage(0);
        Programme saved = programmeService.save(programme);
        programmeId = saved.getId();
        System.out.println("✅ SETUP — Programme créé avec ID=" + programmeId);
    }

    @AfterAll
    static void teardown() throws SQLException {
        programmeService.delete(programmeId);
        System.out.println("✅ TEARDOWN — Programme supprimé ID=" + programmeId);
    }

    // ✅ Helper : crée un objectif avec un programme_id donné
    private Objectif buildObjectif(String titre, String description, int progId) {
        Objectif obj = new Objectif();
        obj.setTitre(titre);
        obj.setDescription(description);
        obj.setDatedebut(LocalDate.now());
        obj.setDatefin(LocalDate.now().plusMonths(2));
        obj.setStatut(Statutobj.EnCours);
        obj.setUtilisateurId(1);
        Programme p = new Programme();
        p.setId(progId);
        obj.setProgramme(p);
        return obj;
    }

    // ✅ Helper : ignore le test si savedId invalide
    private void assumePositiveId() {
        Assumptions.assumeTrue(savedId > 0, "savedId invalide, testSave() a échoué");
    }

    // ✅ TEST 1 : Création d'un objectif valide
    @Test
    @Order(1)
    void testSave() throws SQLException {
        Objectif obj = buildObjectif("Apprendre Java", "Maitriser JavaFX et JDBC", programmeId);
        Objectif saved = objectifService.save(obj);
        savedId = saved.getId();

        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Apprendre Java", saved.getTitre());
        System.out.println("✅ TEST 1 PASSED — save() : ID=" + savedId);
    }

    // ✅ TEST 2 : Unicité — doublon doit lever une exception
    @Test
    @Order(2)
    void testSaveDoublonLeverException() throws SQLException {
        // ✅ Crée un programme temporaire juste pour tester le doublon titre+desc
        Programme progTemp = new Programme();
        progTemp.setTitre("Programme Temp Doublon");
        progTemp.setDategeneration(LocalDate.now());
        progTemp.setScorePourcentage(0);
        Programme savedProg = programmeService.save(progTemp);

        try {
            Objectif doublon = buildObjectif(
                    "Apprendre Java",
                    "Maitriser JavaFX et JDBC",
                    savedProg.getId()
            );
            SQLException ex = assertThrows(SQLException.class,
                    () -> objectifService.save(doublon));
            assertTrue(ex.getMessage().contains("existe déjà"));
            System.out.println("✅ TEST 2 PASSED — unicité save() : " + ex.getMessage());
        } finally {
            programmeService.delete(savedProg.getId());
        }
    }

    // ✅ TEST 3 : findById
    @Test
    @Order(3)
    void testFindById() throws SQLException {
        assumePositiveId();
        Objectif found = objectifService.findById(savedId);
        assertNotNull(found);
        assertEquals(savedId, found.getId());
        assertEquals("Apprendre Java", found.getTitre());
        System.out.println("✅ TEST 3 PASSED — findById() : " + found.getTitre());
    }

    // ✅ TEST 4 : findByUtilisateur
    @Test
    @Order(4)
    void testFindByUtilisateur() throws SQLException {
        List<Objectif> list = objectifService.findByUtilisateur(1);
        assertNotNull(list);
        assertFalse(list.isEmpty());
        System.out.println("✅ TEST 4 PASSED — findByUtilisateur() : " + list.size() + " objectifs");
    }

    // ✅ TEST 5 : update valide
    @Test
    @Order(5)
    void testUpdate() throws SQLException {
        assumePositiveId();
        Objectif obj = objectifService.findById(savedId);
        assertNotNull(obj);
        obj.setTitre("Apprendre Java Avance");
        obj.setDescription("Maitriser JavaFX et JDBC avance");
        objectifService.update(obj);

        Objectif updated = objectifService.findById(savedId);
        assertEquals("Apprendre Java Avance", updated.getTitre());
        System.out.println("✅ TEST 5 PASSED — update() : " + updated.getTitre());
    }

    // ✅ TEST 6 : update doublon doit lever une exception
    @Test
    @Order(6)
    void testUpdateDoublonLeverException() throws SQLException {
        assumePositiveId();

        // ✅ Crée un programme dédié pour le second objectif
        Programme progSecond = new Programme();
        progSecond.setTitre("Programme Second Test");
        progSecond.setDategeneration(LocalDate.now());
        progSecond.setScorePourcentage(0);
        Programme savedProg = programmeService.save(progSecond);

        try {
            // Crée un second objectif avec son propre programme
            Objectif second = buildObjectif(
                    "Apprendre Python",
                    "Maitriser Django et Flask",
                    savedProg.getId()
            );
            objectifService.save(second);

            // Essaie de modifier le premier avec titre+description du second
            Objectif obj = objectifService.findById(savedId);
            assertNotNull(obj);
            obj.setTitre("Apprendre Python");
            obj.setDescription("Maitriser Django et Flask");

            SQLException ex = assertThrows(SQLException.class,
                    () -> objectifService.update(obj));
            assertTrue(ex.getMessage().contains("existe déjà"));
            System.out.println("✅ TEST 6 PASSED — unicité update() : " + ex.getMessage());

            // Nettoyage objectif second
            objectifService.delete(second.getId());
        } finally {
            // Nettoyage programme second
            programmeService.delete(savedProg.getId());
        }
    }

    // ✅ TEST 7 : searchByTitre
    @Test
    @Order(7)
    void testSearchByTitre() throws SQLException {
        assumePositiveId();
        List<Objectif> results = objectifService.searchByTitre("Java");
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream()
                .anyMatch(o -> o.getTitre().toLowerCase().contains("java")));
        System.out.println("✅ TEST 7 PASSED — searchByTitre() : "
                + results.size() + " résultats");
    }

    // ✅ TEST 8 : findById inexistant retourne null
    @Test
    @Order(8)
    void testFindByIdInexistant() throws SQLException {
        Objectif obj = objectifService.findById(999999);
        assertNull(obj);
        System.out.println("✅ TEST 8 PASSED — findById() inexistant retourne null");
    }

    // ✅ TEST 9 : delete
    @Test
    @Order(9)
    void testDelete() throws SQLException {
        assumePositiveId();
        objectifService.delete(savedId);
        Objectif deleted = objectifService.findById(savedId);
        assertNull(deleted);
        System.out.println("✅ TEST 9 PASSED — delete() : ID=" + savedId + " supprimé");
    }
}