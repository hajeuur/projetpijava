package edu.connection3a36.services;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProjetServiceTest {

    private static final ParcoursService parcoursService = new ParcoursService();
    private static final ProjetService projetService = new ProjetService();

    private static int generatedParcoursId = -1;
    private static int generatedProjetId = -1;

    private static final String P_TITRE = "Dummy Parcours For Projet Test";
    private static final String PR_TITRE = "Test Projet Unique 777";

    @BeforeAll
    static void setup() throws SQLException {
        // Préparer un Parcours parent valide car on teste Projet
        Parcours parcours = new Parcours();
        parcours.setTitre(P_TITRE);
        parcours.setTypeParcours("Test");
        parcours.setDescription("Dummy description for testing");
        parcours.setEtablissement("Test Etab");
        parcours.setDiplome("Test Diplome");
        if (!parcoursService.existsByTitreAndType(P_TITRE, "Test")) {
            parcoursService.addEntity(parcours);
        }

        List<Parcours> pts = parcoursService.searchByTitre(P_TITRE);
        if (!pts.isEmpty()) {
            generatedParcoursId = pts.get(0).getId();
        } else {
            fail("Impossible de créer/trouver le parcours parent pour le test");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Tester l'ajout d'un projet lié à un parcours")
    void testAddEntityWithParcours() {
        Projet projet = new Projet();
        projet.setTitre(PR_TITRE);
        projet.setType("application web");
        projet.setDescription("Projet avec parcours");
        projet.setTechnologies("Java, FX");
        projet.setDateDebut(LocalDate.now());
        projet.setParcoursId(generatedParcoursId);

        assertDoesNotThrow(() -> projetService.addEntity(projet), "L'ajout avec parcours devrait fonctionner.");
    }

    @Test
    @Order(2)
    @DisplayName("Tester l'ajout d'un projet global (sans parcours)")
    void testAddGlobalEntity() {
        Projet projet = new Projet();
        projet.setTitre("Global Project Test");
        projet.setType("outil");
        projet.setDescription("Sans parcours");
        projet.setTechnologies("JUnit");
        projet.setDateDebut(LocalDate.now());
        projet.setParcoursId(0); // Global

        assertDoesNotThrow(() -> projetService.addEntity(projet),
                "L'ajout global (parcours_id=0) devrait fonctionner.");
    }

    @Test
    @Order(3)
    @DisplayName("Tester l'ajout d'un projet en doublon (doit échouer)")
    void testAddDuplicateEntity() {
        Projet projet = new Projet();
        projet.setTitre(PR_TITRE);
        projet.setType("application mobile");
        projet.setDescription("Doublon");
        projet.setParcoursId(generatedParcoursId);

        SQLException exception = assertThrows(SQLException.class, () -> {
            projetService.addEntity(projet);
        });

        assertTrue(exception.getMessage().contains("existe déjà"), "Doit renvoyer une erreur d'unicité");
    }

    @Test
    @Order(3)
    @DisplayName("Tester la récupération des données d'un parcours")
    void testGetByParcoursId() throws SQLException {
        List<Projet> list = projetService.getByParcoursId(generatedParcoursId);
        assertNotNull(list);
        assertTrue(list.size() > 0, "Le projet ajouté devrait être présent");

        Projet found = list.stream().filter(p -> p.getTitre().equals(PR_TITRE)).findFirst().orElse(null);
        assertNotNull(found);
        assertTrue(found.getId() > 0);

        generatedProjetId = found.getId();
    }

    @Test
    @Order(4)
    @DisplayName("Tester la modification d'un projet")
    void testUpdateEntity() {
        assertTrue(generatedProjetId > 0);

        Projet updated = new Projet();
        updated.setTitre(PR_TITRE + " Modifié");
        updated.setType("application web");
        updated.setDescription("Description mise à jour");
        updated.setTechnologies("Java, SQL");
        updated.setParcoursId(generatedParcoursId);

        assertDoesNotThrow(() -> projetService.updateEntity(generatedProjetId, updated));
    }

    @Test
    @Order(6)
    @DisplayName("Tester la suppression des données")
    void testDeleteAndCleanup() {
        assertTrue(generatedProjetId > 0);

        try {
            // Nettoyage global
            List<Projet> globals = projetService.getData();
            for (Projet p : globals) {
                if (p.getTitre().equals("Global Project Test") || p.getTitre().equals(PR_TITRE)
                        || p.getTitre().contains("Modifié")) {
                    projetService.deleteEntity(p);
                }
            }

            // Supprimer le parcours parent de test
            Parcours toDeleteP = new Parcours();
            toDeleteP.setId(generatedParcoursId);
            parcoursService.deleteEntity(toDeleteP);
        } catch (SQLException e) {
            fail("Cleanup failed: " + e.getMessage());
        }
    }
}
