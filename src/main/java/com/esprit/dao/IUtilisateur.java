package com.esprit.dao;

import com.esprit.models.Utilisateur;
import java.util.List;

public interface IUtilisateur {

    // Ajouter un utilisateur
    void ajouter(Utilisateur u);

    // Modifier un utilisateur
    void modifier(Utilisateur u);

    // Supprimer un utilisateur
    void supprimer(int id);

    // Récupérer un utilisateur par son id
    Utilisateur getOne(int id);

    // Récupérer tous les utilisateurs
    List<Utilisateur> getAll();

    // Récupérer un utilisateur par email et mot de passe (pour login)
    Utilisateur login(String email, String mdp);

}