package edu.connection3a36.services;

import edu.connection3a36.entities.Parcours;
import edu.connection3a36.entities.Projet;
import edu.connection3a36.entities.Ressource;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RessourceServiceTest {

    private static final ParcoursService parcoursService = new ParcoursService();
    private static final ProjetService projetService = new ProjetService();
    private static final RessourceService ressourceService = new RessourceService();

    private static int generatedParcoursId = -1;
    private static int generatedProjetId = -1;
    private static int generatedRessourceId = -1;

    private static final String P_TITRE = "Ressource Test Parcours";
    private static final String PR_TITRE = "Ressource Test Projet";
    private static final String R_NOM = "Test Ressource 101";

    @BeforeAll
    static void setup() throws SQLException {
        // Parcours
        Parcours parcours = new Parcours();
        parcours.setTitre(P_TITRE);
        parcours.setTypeParcours("Test");
        parcours.setDescription("Dummy description");
        parcours.setEtablissement("Test Etab");
        parcours.setDiplome("Test Diplome");
        if (!parcoursService.existsByTitreAndType(P_TITRE, "Test")) {
            parcoursService.addEntity(parcours);
        }
        generatedParcoursId = parcoursService.searchByTitre(P_TITRE).get(0).getId();

        // Projet
        Projet projet = new Projet();
        projet.setTitre(PR_TITRE);
        projet.setType("Autre");
        projet.setDescription("Dummy description");
        projet.setTechnologies("Test");
        projet.setParcoursId(generatedParcoursId);
        if (!projetService.existsByTitreAndParcours(PR_TITRE, generatedParcoursId)) {
            projetService.addEntity(projet);
        }
        generatedProjetId = projetService.getByParcoursId(generatedParcoursId)
                .stream().filter(p -> p.getTitre().equals(PR_TITRE)).findFirst().get().getId();
    }

    @Test
    @Order(1)
    @DisplayName("Tester l'ajout d'une ressource")
    void testAddEntity() {
        Ressource res = new Ressource();
        res.setNom(R_NOM);
        res.setTypeRessource("PDF");
        res.setUrlRessource("http://test.com/doc.pdf");
        res.setDescription("Test add resource");
        res.setProjetId(generatedProjetId);

        assertDoesNotThrow(() -> ressourceService.addEntity(res));
    }

    @Test
    @Order(2)
    @DisplayName("Tester l'ajout en doublon (doit échouer)")
    void testAddDuplicate() {
        Ressource res = new Ressource();
        res.setNom(R_NOM);
        res.setDescription("Doublon");
        res.setProjetId(generatedProjetId);

        SQLException exception = assertThrows(SQLException.class, () -> ressourceService.addEntity(res));
        assertTrue(exception.getMessage().contains("existe déjà"));
    }

    @Test
    @Order(3)
    @DisplayName("Tester la lecture (getData/getByProjetId)")
    void testGetByProjet() throws SQLException {
        List<Ressource> list = ressourceService.getByProjetId(generatedProjetId);
        assertFalse(list.isEmpty());

        Ressource found = list.stream().filter(r -> r.getNom().equals(R_NOM)).findFirst().orElse(null);
        assertNotNull(found);
        assertTrue(found.getId() > 0);

        generatedRessourceId = found.getId();
    }

    @Test
    @Order(4)
    @DisplayName("Tester la modification d'une ressource")
    void testUpdateEntity() {
        Ressource updated = new Ressource();
        updated.setNom(R_NOM + " Modifiée");
        updated.setTypeRessource("VIDEO");
        updated.setUrlRessource("http://test.com/vid.mp4");
        updated.setDescription("Desc modifiée");
        updated.setProjetId(generatedProjetId);

        assertDoesNotThrow(() -> ressourceService.updateEntity(generatedRessourceId, updated));
    }

    @Test
    @Order(5)
    @DisplayName("Nettoyage global (Suppression Cascade/Manuelle)")
    void testCleanup() {
        // En vrai l'application a un delete en cascade géré dans ProjetService.
        // Faisons le delete manuellement de la ressource juste pour tester sa méthode
        // deleteEntity
        Ressource toDeleteR = new Ressource();
        toDeleteR.setId(generatedRessourceId);
        assertDoesNotThrow(() -> ressourceService.deleteEntity(toDeleteR));

        // Supprimer Projet
        Projet toDeleteP = new Projet();
        toDeleteP.setId(generatedProjetId);
        assertDoesNotThrow(() -> projetService.deleteEntity(toDeleteP));

        // Supprimer Parcours
        Parcours toDeletePa = new Parcours();
        toDeletePa.setId(generatedParcoursId);
        assertDoesNotThrow(() -> parcoursService.deleteEntity(toDeletePa));
    }
}
