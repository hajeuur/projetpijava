package com.esprit.repositories;

import com.esprit.models.Utilisateur;
import java.util.List;

public interface IUtilisateur {

    void ajouter(Utilisateur u);
    void modifier(Utilisateur u);
    void supprimer(int id);
    Utilisateur getOne(int id);
    List<Utilisateur> getAll();
    Utilisateur login(String email, String mdp);

    // Méthodes supplémentaires
    Utilisateur findByEmail(String email);
    void updateRisk(Utilisateur u);
    void saveAiVerdict(Utilisateur u);
    void incrementLoginAttempts(String email);
    void resetLoginAttempts(String email);
}