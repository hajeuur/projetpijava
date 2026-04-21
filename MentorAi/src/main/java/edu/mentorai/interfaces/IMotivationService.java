package edu.mentorai.interfaces;

import edu.mentorai.entities.Motivation;

import java.sql.SQLException;
import java.util.List;

public interface IMotivationService extends IGenericService<Motivation> {
    List<Motivation> findByProgramme(int programmeId) throws SQLException;
    Motivation findLatestByProgramme(int programmeId) throws SQLException;
}