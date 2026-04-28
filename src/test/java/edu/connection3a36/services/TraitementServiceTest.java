package edu.connection3a36.services;

import edu.connection3a36.models.Traitement;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TraitementServiceTest {

    static TraitementService service = new TraitementService();

    @Test
    @Order(1)
    void testAjouter() {
        Traitement t = new Traitement(
                "reponse_simple",
                "Traitement de test automatique",
                LocalDate.now(),
                "accepte"
        );
        service.add(t);
        List<Traitement> liste = service.getAll();
        assertFalse(liste.isEmpty());
        System.out.println("testAjouter Traitement OK !");
    }

    @Test
    @Order(2)
    void testAfficher() {
        List<Traitement> liste = service.getAll();
        assertNotNull(liste);
        assertFalse(liste.isEmpty());
        System.out.println("testAfficher Traitement OK ! " + liste.size() + " trouves");
    }

    @Test
    @Order(3)
    void testModifier() {
        List<Traitement> liste = service.getAll();
        Traitement t = liste.get(liste.size() - 1);
        t.setDescription("Description modifiee par test");
        t.setDecision("rejete");
        service.update(t);

        Traitement modifie = service.getById(t.getId());
        assertNotNull(modifie);
        assertEquals("Description modifiee par test", modifie.getDescription());
        assertEquals("rejete", modifie.getDecision());
        System.out.println("testModifier Traitement OK !");
    }

    @Test
    @Order(4)
    void testSupprimer() {
        List<Traitement> avant = service.getAll();
        int id = avant.get(avant.size() - 1).getId();
        service.delete(id);

        Traitement supprime = service.getById(id);
        assertNull(supprime);
        System.out.println("testSupprimer Traitement OK !");
    }
}
