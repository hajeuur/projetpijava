package edu.connection3a36.services;

import edu.connection3a36.models.Feedback;
import org.junit.jupiter.api.*;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeedbackServiceTest {

    static FeedbackService service = new FeedbackService();

    @Test
    @Order(1)
    void testAjouter() {
        Feedback f = new Feedback(
                "Test automatique unitaire",
                3, LocalDate.now(),
                "suggestion", "en_attente",
                0, 11
        );
        service.add(f);
        List<Feedback> liste = service.getAll();
        assertFalse(liste.isEmpty());
        System.out.println("testAjouter OK !");
    }

    @Test
    @Order(2)
    void testAfficher() {
        List<Feedback> liste = service.getAll();
        assertNotNull(liste);
        assertFalse(liste.isEmpty());
        System.out.println("testAfficher OK ! " + liste.size() + " feedbacks");
    }

    @Test
    @Order(3)
    void testModifier() {
        List<Feedback> liste = service.getAll();
        Feedback f = liste.get(liste.size() - 1);
        f.setContenu("Contenu modifie par test unitaire");
        f.setNote(5);
        service.update(f);

        List<Feedback> apres = service.getAll();
        Feedback modifie = apres.stream()
                .filter(fb -> fb.getId() == f.getId())
                .findFirst().orElse(null);

        assertNotNull(modifie);
        assertEquals("Contenu modifie par test unitaire", modifie.getContenu());
        System.out.println("testModifier OK !");
    }

    @Test
    @Order(4)
    void testSupprimer() {
        List<Feedback> avant = service.getAll();
        int id = avant.get(avant.size() - 1).getId();
        service.delete(id);

        List<Feedback> apres = service.getAll();
        boolean existe = apres.stream().anyMatch(f -> f.getId() == id);
        assertFalse(existe);
        System.out.println("testSupprimer OK !");
    }
}
