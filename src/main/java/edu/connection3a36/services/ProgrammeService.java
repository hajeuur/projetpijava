package edu.connection3a36.services;

import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;
import edu.connection3a36.entities.Medaille;
import edu.connection3a36.entities.Programme;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère toutes les opérations sur les programmes liés aux objectifs.
 * Implémente IService<Programme> pour les opérations CRUD de base.
 *
 * Méthodes supplémentaires :
 *   getById()          → récupère un programme par son ID
 *   getByObjectifId()  → récupère le programme d'un objectif via JOIN
 *   updateScore()      → met à jour le score et la médaille (appelé par ScoreService)
 *   addForObjectif()   → crée un programme et le lie à un objectif existant
 */
public class ProgrammeService implements IService<Programme> {

    /** Connexion unique à la BDD (pattern Singleton) */
    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ── CRUD de base ──────────────────────────────────────────────────────────

    /** Insère un nouveau programme en BDD et récupère son ID généré. */
    @Override
    public void addEntity(Programme p) throws SQLException {
        String req = "INSERT INTO programme (titre, dategeneration, score_pourcentage, meilleure_medaille) VALUES (?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, p.getTitre());
        // Si la date est null, on utilise la date du jour
        pst.setDate(2, Date.valueOf(p.getDategeneration() != null ? p.getDategeneration() : LocalDate.now()));
        pst.setInt(3, p.getScorePourcentage());
        pst.setString(4, p.getMeilleureMedaille() != null ? p.getMeilleureMedaille().getValue() : null);
        pst.executeUpdate();
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) p.setId(rs.getInt(1));
    }

    /**
     * Crée un programme et met à jour le programme_id de l'objectif lié.
     * Utilisé quand un objectif n'a pas encore de programme associé.
     */
    public void addForObjectif(Programme p, int objectifId) throws SQLException {
        addEntity(p); // créer le programme
        if (objectifId > 0 && p.getId() > 0) {
            // Lier le programme à l'objectif
            PreparedStatement upd = cnx.prepareStatement(
                    "UPDATE objectif SET programme_id=? WHERE id=?");
            upd.setInt(1, p.getId());
            upd.setInt(2, objectifId);
            upd.executeUpdate();
        }
    }

    /**
     * Supprime un programme et tout ce qui lui est lié (cascade).
     * Ordre : motivations → tâches → programme
     */
    @Override
    public void deleteEntity(Programme p) throws SQLException {
        new MotivationService().deleteByProgramme(p.getId()); // 1. supprimer les messages IA
        new TacheService().deleteByProgramme(p.getId());      // 2. supprimer les tâches
        PreparedStatement pst = cnx.prepareStatement("DELETE FROM programme WHERE id = ?");
        pst.setInt(1, p.getId());
        pst.executeUpdate();
    }

    /** Met à jour le titre, le score et la médaille d'un programme. */
    @Override
    public void updateEntity(int id, Programme p) throws SQLException {
        String req = "UPDATE programme SET titre=?, score_pourcentage=?, meilleure_medaille=? WHERE id=?";
        PreparedStatement pst = cnx.prepareStatement(req);
        pst.setString(1, p.getTitre());
        pst.setInt(2, p.getScorePourcentage());
        pst.setString(3, p.getMeilleureMedaille() != null ? p.getMeilleureMedaille().getValue() : null);
        pst.setInt(4, id);
        pst.executeUpdate();
    }

    /** Retourne tous les programmes, triés par date de création décroissante. */
    @Override
    public List<Programme> getData() throws SQLException {
        List<Programme> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM programme ORDER BY dategeneration DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    // ── Méthodes spécifiques ──────────────────────────────────────────────────

    /**
     * Récupère un programme par son ID.
     * Utilisé dans les cartes d'objectifs pour afficher le score et la médaille.
     */
    public Programme getById(int id) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT * FROM programme WHERE id = ?");
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null; // programme non trouvé
    }

    /**
     * Récupère le programme d'un objectif via une jointure SQL.
     * Utilisé quand on a l'ID de l'objectif mais pas celui du programme.
     */
    public Programme getByObjectifId(int objectifId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT p.* FROM programme p " +
                "INNER JOIN objectif o ON o.programme_id = p.id WHERE o.id = ?");
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    /**
     * Met à jour uniquement le score et la médaille d'un programme.
     * Appelé par ScoreService.recalculerEtSauvegarder() après chaque changement de tâche.
     */
    public void updateScore(int programmeId, int score, String medaille) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "UPDATE programme SET score_pourcentage=?, meilleure_medaille=? WHERE id=?");
        pst.setInt(1, score);
        pst.setString(2, medaille); // null si aucune médaille
        pst.setInt(3, programmeId);
        pst.executeUpdate();
    }

    // ── Mapping BDD → objet Java ──────────────────────────────────────────────

    /** Convertit une ligne de la BDD (ResultSet) en objet Programme. */
    private Programme map(ResultSet rs) throws SQLException {
        Programme p = new Programme();
        p.setId(rs.getInt("id"));
        p.setTitre(rs.getString("titre"));
        Date dg = rs.getDate("dategeneration");
        if (dg != null) p.setDategeneration(dg.toLocalDate());
        p.setScorePourcentage(rs.getInt("score_pourcentage"));
        // Medaille.fromValue() retourne null si la valeur est null (pas encore de médaille)
        p.setMeilleureMedaille(Medaille.fromValue(rs.getString("meilleure_medaille")));
        return p;
    }
}
