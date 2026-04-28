package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Objectif;
import edu.connection3a36.entities.Programme;
import edu.connection3a36.entities.Statutobj;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService implements IService<Objectif> {

    private final Connection cnx = MyConnection.getInstance().getCnx();

    /**
     * Crée le programme d'abord, puis insère l'objectif avec programme_id.
     * Structure BDD : objectif.programme_id → programme.id
     */
    @Override
    public void addEntity(Objectif o) throws SQLException {
        // 1. Créer le programme
        String reqProg = "INSERT INTO programme (titre, dategeneration, score_pourcentage, meilleure_medaille) VALUES (?,?,?,?)";
        PreparedStatement pstProg = cnx.prepareStatement(reqProg, Statement.RETURN_GENERATED_KEYS);
        pstProg.setString(1, "Programme — " + o.getTitre());
        pstProg.setDate(2, Date.valueOf(LocalDate.now()));
        pstProg.setInt(3, 0);
        pstProg.setNull(4, Types.VARCHAR);
        pstProg.executeUpdate();
        ResultSet rsProg = pstProg.getGeneratedKeys();
        if (!rsProg.next()) throw new SQLException("Impossible de créer le programme.");
        int programmeId = rsProg.getInt(1);

        Programme prog = new Programme("Programme — " + o.getTitre(), LocalDate.now());
        prog.setId(programmeId);
        o.setProgramme(prog);

        // 2. Insérer l'objectif avec programme_id
        String req = "INSERT INTO objectif (titre, description, datedebut, datefin, statut, programme_id, utilisateur_id) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, o.getTitre());
        pst.setString(2, o.getDescription() != null ? o.getDescription() : "");
        pst.setDate(3, o.getDatedebut() != null ? Date.valueOf(o.getDatedebut()) : Date.valueOf(LocalDate.now()));
        pst.setDate(4, o.getDatefin() != null ? Date.valueOf(o.getDatefin()) : null);
        pst.setString(5, o.getStatut() != null ? o.getStatut().getValue() : Statutobj.EnCours.getValue());
        pst.setInt(6, programmeId);
        pst.setInt(7, o.getUtilisateurId());
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) o.setId(rs.getInt(1));
        System.out.println("✅ Objectif id=" + o.getId() + " + Programme id=" + programmeId);
    }

    @Override
    public void deleteEntity(Objectif o) throws SQLException {
        int programmeId = getProgrammeId(o.getId());
        if (programmeId > 0) {
            new MotivationService().deleteByProgramme(programmeId);
            new TacheService().deleteByProgramme(programmeId);
        }
        PreparedStatement pstObj = cnx.prepareStatement("DELETE FROM objectif WHERE id = ?");
        pstObj.setInt(1, o.getId());
        pstObj.executeUpdate();
        if (programmeId > 0) {
            PreparedStatement pstProg = cnx.prepareStatement("DELETE FROM programme WHERE id = ?");
            pstProg.setInt(1, programmeId);
            pstProg.executeUpdate();
        }
        System.out.println("🗑️ Objectif supprimé : " + o.getTitre());
    }

    @Override
    public void updateEntity(int id, Objectif o) throws SQLException {
        String req = "UPDATE objectif SET titre=?, description=?, datedebut=?, datefin=?, statut=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, o.getTitre());
        pst.setString(2, o.getDescription() != null ? o.getDescription() : "");
        pst.setDate(3, o.getDatedebut() != null ? Date.valueOf(o.getDatedebut()) : null);
        pst.setDate(4, o.getDatefin() != null ? Date.valueOf(o.getDatefin()) : null);
        pst.setString(5, o.getStatut() != null ? o.getStatut().getValue() : Statutobj.EnCours.getValue());
        pst.setInt(6, id);
        pst.executeUpdate();
    }

    @Override
    public List<Objectif> getData() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM objectif ORDER BY datedebut DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Objectif> getByUtilisateur(int utilisateurId) throws SQLException {
        List<Objectif> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM objectif WHERE utilisateur_id = ? ORDER BY datedebut DESC");
        pst.setInt(1, utilisateurId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public Objectif getById(int id) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT * FROM objectif WHERE id = ?");
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    public int getProgrammeId(int objectifId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT programme_id FROM objectif WHERE id = ?");
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt("programme_id");
        return 0;
    }

    /** Trouve l'objectif lié à un programme via objectif.programme_id */
    public edu.connection3a36.entities.Objectif getByProgrammeId(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT * FROM objectif WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    /**
     * Valide un objectif avant insertion/modification.
     * @return liste des erreurs (vide = valide)
     */
    public List<String> validate(edu.connection3a36.entities.Objectif o) {
        List<String> erreurs = new ArrayList<>();
        if (o.getTitre() == null || o.getTitre().isBlank())
            erreurs.add("Le titre est obligatoire");
        else if (o.getTitre().trim().length() < 3)
            erreurs.add("Le titre doit contenir au moins 3 caractères");
        if (o.getDatedebut() == null)
            erreurs.add("La date de debut est obligatoire");
        if (o.getDatefin() == null)
            erreurs.add("La date de fin est obligatoire");
        if (o.getDatedebut() != null && o.getDatefin() != null
                && o.getDatefin().isBefore(o.getDatedebut()))
            erreurs.add("La date de fin doit etre apres la date de debut");
        if (o.getUtilisateurId() <= 0)
            erreurs.add("L utilisateur est obligatoire");
        return erreurs;
    }

    private Objectif map(ResultSet rs) throws SQLException {
        Objectif o = new Objectif();
        o.setId(rs.getInt("id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));
        Date dd = rs.getDate("datedebut");
        if (dd != null) o.setDatedebut(dd.toLocalDate());
        Date df = rs.getDate("datefin");
        if (df != null) o.setDatefin(df.toLocalDate());
        try { o.setStatut(Statutobj.fromValue(rs.getString("statut"))); }
        catch (Exception e) { o.setStatut(Statutobj.EnCours); }
        o.setUtilisateurId(rs.getInt("utilisateur_id"));
        int progId = rs.getInt("programme_id");
        if (progId > 0) {
            Programme p = new Programme();
            p.setId(progId);
            o.setProgramme(p);
        }
        return o;
    }
}
