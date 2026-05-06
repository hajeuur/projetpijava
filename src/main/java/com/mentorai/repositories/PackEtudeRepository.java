package com.mentorai.repositories;

import com.mentorai.utils.MyConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class PackEtudeRepository {

    public Map<String, Object> getProfil(int userId) {
        String query = "SELECT * FROM profil_apprentissage WHERE utilisateur_id = ? LIMIT 1";
        Map<String, Object> profil = new HashMap<>();
        
        try (Connection cnx = MyConnection.getInstance();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                profil.put("id", rs.getInt("id"));
                profil.put("niveau_concentration", rs.getString("niveau_concentration"));
                profil.put("temps_moyen_apprentissage", rs.getInt("temps_moyen_apprentissage"));
                profil.put("matieres_fortes", rs.getString("matieres_fortes"));
                profil.put("matieres_faibles", rs.getString("matieres_faibles"));
                profil.put("vitesse_apprentissage", rs.getString("vitesse_apprentissage"));
                profil.put("format_préféré", rs.getString("format_préféré"));
                profil.put("style_motivation", rs.getString("style_motivation"));
                profil.put("type_pers", rs.getString("type_pers"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return profil.isEmpty() ? null : profil;
    }

    public Map<String, Object> getLatestHumeur(int profilId) {
        String query = "SELECT * FROM humeur WHERE profil_apprentissage_id = ? ORDER BY cree_le DESC LIMIT 1";
        Map<String, Object> humeur = new HashMap<>();
        
        try (Connection cnx = MyConnection.getInstance();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, profilId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                humeur.put("valeur_humeur", rs.getInt("valeur_humeur"));
                humeur.put("facteur_principal", rs.getString("facteur_principal"));
                humeur.put("tendance", rs.getString("tendance"));
                humeur.put("niveau_risque", rs.getString("niveau_risque"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return humeur.isEmpty() ? null : humeur;
    }

    public double getCompletionRate(int userId) {
        String query = "SELECT COUNT(*) as total, SUM(CASE WHEN etat='complété' THEN 1 ELSE 0 END) as completed FROM planning_etude WHERE utilisateur_id = ?";
        
        try (Connection cnx = MyConnection.getInstance();
             PreparedStatement pst = cnx.prepareStatement(query)) {
            pst.setInt(1, userId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int completed = rs.getInt("completed");
                if (total > 0) {
                    return (double) completed / total;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }
}
