package com.esprit;

import com.esprit.dao.UtilisateurDAO;
import com.esprit.models.Utilisateur;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        UtilisateurDAO dao = new UtilisateurDAO();

        // TEST 1 : Ajouter un utilisateur
        Utilisateur u = new Utilisateur("Ben Ali", "Ahmed", "ahmed@esprit.tn", "1234", "etudiant");
        dao.ajouter(u);

        // TEST 2 : Afficher tous les utilisateurs
        List<Utilisateur> liste = dao.getAll();
        for (Utilisateur user : liste) {
            System.out.println(user);
        }
    }
}