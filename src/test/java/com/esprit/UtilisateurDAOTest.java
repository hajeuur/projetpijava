package com.esprit;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import org.junit.jupiter.api.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UtilisateurDAOTest {

    static UtilisateurDAO dao;
    static int idUserTest;

    @BeforeAll
    static void setup() {
        dao = new UtilisateurDAO();
        System.out.println("Initialisation du DAO OK");
    }

    @Test
    @Order(1)
    void testAjouter() {
        Utilisateur u = new Utilisateur(
                "TestNom", "TestPrenom",
                "test.unitaire@esprit.tn",
                "testpass123", "etudiant"
        );
        dao.ajouter(u);

        List<Utilisateur> liste = dao.getAll();
        assertFalse(liste.isEmpty());

        boolean trouve = liste.stream()
                .anyMatch(user -> user.getEmail().equals("test.unitaire@esprit.tn"));
        assertTrue(trouve, "L'utilisateur ajouté doit être dans la liste");

        idUserTest = liste.stream()
                .filter(user -> user.getEmail().equals("test.unitaire@esprit.tn"))
                .findFirst()
                .get()
                .getId();
        System.out.println("Test Ajouter OK - ID : " + idUserTest);
    }

    @Test
    @Order(2)
    void testGetAll() {
        List<Utilisateur> liste = dao.getAll();
        assertNotNull(liste, "La liste ne doit pas être null");
        assertFalse(liste.isEmpty(), "La liste ne doit pas être vide");
        System.out.println("Test GetAll OK - " + liste.size() + " utilisateurs");
    }

    @Test
    @Order(3)
    void testGetOne() {
        Utilisateur u = dao.getOne(idUserTest);
        assertNotNull(u, "L'utilisateur doit exister");
        assertEquals("TestNom", u.getNom());
        assertEquals("TestPrenom", u.getPrenom());
        System.out.println("Test GetOne OK - " + u.getNom());
    }

    @Test
    @Order(4)
    void testModifier() {
        Utilisateur u = dao.getOne(idUserTest);
        assertNotNull(u);
        u.setNom("NomModifie");
        u.setPrenom("PrenomModifie");
        dao.modifier(u);

        Utilisateur modifie = dao.getOne(idUserTest);
        assertEquals("NomModifie", modifie.getNom());
        assertEquals("PrenomModifie", modifie.getPrenom());
        System.out.println("Test Modifier OK");
    }

    @Test
    @Order(5)
    void testLoginMauvaisMdp() {
        Utilisateur mauvais = dao.login("test.unitaire@esprit.tn", "mauvaismdp");
        assertNull(mauvais, "Login avec mauvais mdp doit retourner null");
        System.out.println("Test Login mauvais mdp OK");
    }

    @Test
    @Order(6)
    void testSupprimer() {
        dao.supprimer(idUserTest);
        Utilisateur u = dao.getOne(idUserTest);
        assertNull(u, "L'utilisateur supprimé ne doit plus exister");
        System.out.println("Test Supprimer OK");
    }

    @AfterEach
    void afterEach() {
        System.out.println("--- Test terminé ---");
    }

    @AfterAll
    static void cleanup() {
        List<Utilisateur> liste = dao.getAll();
        liste.stream()
                .filter(u -> u.getEmail().equals("test.unitaire@esprit.tn"))
                .forEach(u -> dao.supprimer(u.getId()));
        System.out.println("Nettoyage final OK");
    }
}