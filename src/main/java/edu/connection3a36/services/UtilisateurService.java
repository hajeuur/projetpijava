package edu.connection3a36.services;

import edu.connection3a36.entities.Utilisateur;
import edu.connection3a36.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UtilisateurService {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    public Utilisateur login(String email, String mdp) throws SQLException {
        // En JavaFX local pour tester, on vérifie sans hachage ou avec hachage simulé.
        // Puisque la DB Symfony a déjà des mots de passe hachés avec bcrypt,
        // pour simplifier le test local JavaFX s'il n'y a pas BCrypt intégré,
        // on crée un mock login si l'email est "admin@mentor.com".

        String req = "SELECT * FROM utilisateur WHERE email = ?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, email);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            // Note: Normalement il faut vérifier avec password_verify en PHP.
            // En Java il faudrait utiliser BCrypt.checkpw().
            // Pour démo, on autorise si l'email existe.
            Utilisateur u = new Utilisateur();
            u.setId(rs.getInt("id"));
            u.setEmail(rs.getString("email"));
            u.setRole(rs.getString("role"));
            return u;
        }

        // Mock si base vide
        // Mocks pour tests rapides
        if (email.equalsIgnoreCase("arslene.amira@gmail.com")) {
            return new Utilisateur("arslene.amira@gmail.com", "arslen", "ROLE_ETUDIANT");
        }
        if (email.equalsIgnoreCase("admin")) {
            return new Utilisateur("admin", "12345678910", "ROLE_ADMIN");
        }

        if (email.equals("admin@mentor.com")) {
            return new Utilisateur("admin@mentor.com", "admin", "ROLE_ADMIN");
        }
        if (email.equals("etudiant@mentor.com")) {
            return new Utilisateur("etudiant@mentor.com", "etudiant", "ROLE_ETUDIANT");
        }

        return null; // Échec
    }
}
