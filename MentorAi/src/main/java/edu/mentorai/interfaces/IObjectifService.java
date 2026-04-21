package edu.mentorai.interfaces;

import edu.mentorai.entities.Objectif;

import java.sql.SQLException;
import java.util.List;

public interface IObjectifService extends IGenericService<Objectif> {
    List<Objectif> findByUtilisateur(int utilisateurId) throws SQLException;
    List<Objectif> searchByTitre(String titre) throws SQLException;
}