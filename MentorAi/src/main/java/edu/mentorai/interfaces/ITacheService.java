package edu.mentorai.interfaces;

import edu.mentorai.entities.Tache;
import java.sql.SQLException;
import java.util.List;

public interface ITacheService extends IGenericService<Tache> {
    List<Tache> findByProgramme(int programmeId) throws SQLException;
    List<Tache> searchByTitre(String titre) throws SQLException;
    List<Tache> findByEtat(String etat) throws SQLException;
    void deleteByProgramme(int programmeId) throws SQLException;
    // save, findById, findAll, update, delete → hérités
}