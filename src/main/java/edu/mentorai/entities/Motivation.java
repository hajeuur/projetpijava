package edu.mentorai.entities;

import java.time.LocalDate;

public class Motivation {
    private int id;
    private LocalDate dategeneration;
    private String messagemotivant;
    private int programmeId;

    public Motivation() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getDategeneration() { return dategeneration; }
    public void setDategeneration(LocalDate dategeneration) { this.dategeneration = dategeneration; }

    public String getMessagemotivant() { return messagemotivant; }
    public void setMessagemotivant(String messagemotivant) { this.messagemotivant = messagemotivant; }

    public int getProgrammeId() { return programmeId; }
    public void setProgrammeId(int programmeId) { this.programmeId = programmeId; }
}
