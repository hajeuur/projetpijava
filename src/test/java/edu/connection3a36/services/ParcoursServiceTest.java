package edu.connection3a36.services;

import edu.connection3a36.entities.Parcours;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ParcoursServiceTest {

    private static final ParcoursService parcoursService = new ParcoursService();
    private static int generatedTestId = -1;
    private static final String TEST_TITRE = "Test JUnit Parcours Unique Title 999";
    private static final String TEST_TYPE = "Formation Test";

    @BeforeAll
    static void setup() throws SQLException {
        // Nettoyage préventif au cas où un test précédent a crashé
        List<Parcours> list = parcoursService.getData();
        for (Parcours p : list) {
            if (TEST_TITRE.equals(p.getTitre())) {
                parcoursService.deleteEntity(p);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Tester l'ajout d'un parcours")
    void testAddEntity() {
        Parcours parcours = new Parcours();
        parcours.setTitre(TEST_TITRE);
        parcours.setTypeParcours(TEST_TYPE);
        parcours.setDescription("Un parcours créé automatiquement par un test unitaire");
        parcours.setEtablissement("JUnit Academy");
        parcours.setDiplome("Bac+Test");
        parcours.setDateDebut(LocalDate.now());

        assertDoesNotThrow(() -> {
            parcoursService.addEntity(parcours);
        }, "L'ajout ne devrait pas lancer d'exception.");
    }

    @Test
    @Order(2)
    @DisplayName("Tester l'ajout en doublon (doit échouer)")
    void testAddDuplicateEntity() {
        Parcours parcours = new Parcours();
        parcours.setTitre(TEST_TITRE);
        parcours.setTypeParcours(TEST_TYPE);
        parcours.setDescription("Doublon");
        parcours.setEtablissement("Test Etab");
        parcours.setDiplome("Test Diplome");

        SQLException exception = assertThrows(SQLException.class, () -> {
            parcoursService.addEntity(parcours);
        });

        assertTrue(exception.getMessage().contains("existe déjà"),
                "L'exception doit mentionner l'existence du doublon.");
    }

    @Test
    @Order(3)
    @DisplayName("Tester la lecture globale (getData) et l'existence du parcours ajouté")
    void testGetData() throws SQLException {
        List<Parcours> list = parcoursService.getData();
        assertNotNull(list);
        assertTrue(list.size() > 0, "La liste ne doit pas être vide après un ajout.");

        // Trouver l'ID du parcours inséré
        Parcours myTestParcours = list.stream()
                .filter(p -> p.getTitre() != null && p.getTitre().equals(TEST_TITRE))
                .findFirst()
                .orElse(null);

        assertNotNull(myTestParcours, "Le parcours de test doit se trouver dans la base.");
        assertTrue(myTestParcours.getId() > 0);

        generatedTestId = myTestParcours.getId();
    }

    @Test
    @Order(4)
    @DisplayName("Tester la modification d'un parcours")
    void testUpdateEntity() {
        assertTrue(generatedTestId > 0, "L'ID de test doit exister pour cette étape.");

        Parcours updatedParcours = new Parcours();
        updatedParcours.setTitre(TEST_TITRE + " Modifié");
        updatedParcours.setTypeParcours(TEST_TYPE);
        updatedParcours.setDescription("Description modifiée par test");
        updatedParcours.setEtablissement("JUnit Academy Reloaded");
        updatedParcours.setDiplome("Master Test");

        assertDoesNotThrow(() -> {
            parcoursService.updateEntity(generatedTestId, updatedParcours);
        }, "La modification ne devrait pas lancer d'exception.");
    }

    @Test
    @Order(5)
    @DisplayName("Tester la recherche par titre")
    void testSearchByTitre() throws SQLException {
        List<Parcours> list = parcoursService.searchByTitre("Modifié");
        assertNotNull(list);
        boolean found = list.stream().anyMatch(p -> p.getId() == generatedTestId);
        assertTrue(found, "La recherche doit retrouver le parcours modifié.");
    }

    @Test
    @Order(6)
    @DisplayName("Tester la suppression du parcours")
    void testDeleteEntity() {
        assertTrue(generatedTestId > 0, "L'ID de test doit exister pour suppression.");

        Parcours toDelete = new Parcours();
        toDelete.setId(generatedTestId);

        assertDoesNotThrow(() -> {
            parcoursService.deleteEntity(toDelete);
        }, "La suppression ne devrait pas lancer d'exception.");

        // Vérification de la suppression
        assertDoesNotThrow(() -> {
            List<Parcours> list = parcoursService.getData();
            boolean stillExists = list.stream().anyMatch(p -> p.getId() == generatedTestId);
            assertFalse(stillExists, "Le parcours ne devrait plus exister dans la DB.");
        });
    }
}
