package edu.mentorai.interfaces;

import edu.mentorai.entities.Medaille;
import edu.mentorai.entities.Programme;

import java.sql.SQLException;

public interface IProgrammeService extends IGenericService<Programme> {
    Programme findByObjectifId(int objectifId) throws SQLException;
    void updateScore(int programmeId, int score, Medaille medaille) throws SQLException;
}