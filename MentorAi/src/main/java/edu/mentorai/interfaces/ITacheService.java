package edu.mentorai.interfaces;

import edu.mentorai.entities.Tache;

import java.sql.SQLException;
import java.util.List;

public interface ITacheService extends IGenericService<Tache> {
    List<Tache> findByProgramme(int programmeId) throws SQLException;
    void deleteByProgramme(int programmeId) throws SQLException;
}