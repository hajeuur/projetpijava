package edu.connection3a36.services;

import edu.connection3a36.entities.ReferenceArticle;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

public class ReferenceArticleServiceTest {

    private ReferenceArticleService service;

    @BeforeEach
    public void setup() {
        service = new ReferenceArticleService();
    }

    @Test
    public void testValidationEchoueSiTitreVide() {
        ReferenceArticle article = new ReferenceArticle();
        article.setTitre("");
        article.setContenu("Test content valide");
        article.setCategorieId(1);

        try {
            List<String> erreurs = service.validate(article);
            Assertions.assertFalse(erreurs.isEmpty(), "La validation doit échouer si le titre est vide.");
            Assertions.assertTrue(erreurs.contains("Le titre est obligatoire"));
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testValidationEchoueSiContenuTropCourt() {
        ReferenceArticle article = new ReferenceArticle();
        article.setTitre("Titre Valide");
        article.setContenu("Court"); // Moins de 10 caractères
        article.setCategorieId(1);

        try {
            List<String> erreurs = service.validate(article);
            Assertions.assertFalse(erreurs.isEmpty());
            Assertions.assertTrue(erreurs.contains("Le contenu est trop court"));
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }

    @Test
    public void testValidationReussie() {
        ReferenceArticle article = new ReferenceArticle();
        article.setTitre("Titre Test Valide");
        article.setContenu("Ceci est un contenu très valide de plus de 10 caractères.");
        article.setCategorieId(1);

        try {
            List<String> erreurs = service.validate(article);
            Assertions.assertTrue(erreurs.isEmpty(), "La validation ne doit renvoyer aucune erreur.");
        } catch (SQLException e) {
            Assertions.fail(e);
        }
    }
}
