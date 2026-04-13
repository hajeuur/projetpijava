package edu.connection3a36.services;

import edu.connection3a36.entities.PlanActions;
import edu.connection3a36.enums.CategorieSortie;
import edu.connection3a36.enums.Statut;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service CRUD pour PlanActions.
 * Fonctionnalités : CRUD complet + validation métier + recherche/tri/filtre + statistiques.
 */
public class PlanActionsService implements IService<PlanActions> {

    private final Connection cnx;

    public PlanActionsService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ======================== VALIDATION MÉTIER ========================

    public List<String> validate(PlanActions plan) {
        List<String> errors = new ArrayList<>();

        if (plan.getDecision() == null || plan.getDecision().trim().isEmpty()) {
            errors.add("La décision est obligatoire");
        } else if (plan.getDecision().trim().length() < 5) {
            errors.add("La décision doit contenir au moins 5 caractères");
        } else if (plan.getDecision().trim().length() > 200) {
            errors.add("La décision ne peut pas dépasser 200 caractères");
        }

        if (plan.getDescription() == null || plan.getDescription().trim().isEmpty()) {
            errors.add("La description est obligatoire");
        } else if (plan.getDescription().trim().length() < 10) {
            errors.add("La description doit contenir au moins 10 caractères");
        }

        if (plan.getStatut() == null) {
            errors.add("Le statut est obligatoire");
        }

        if (plan.getCategorie() == null) {
            errors.add("La catégorie est obligatoire");
        }

        return errors;
    }

    public boolean existsByDecision(String decision) throws SQLException {
        String sql = "SELECT COUNT(*) FROM plan_actions WHERE LOWER(decision) = LOWER(?)";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, decision.trim());
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean existsByDecisionExcluding(String decision, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM plan_actions WHERE LOWER(decision) = LOWER(?) AND id != ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, decision.trim());
        pst.setInt(2, id);
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    // ======================== CRUD ========================

    @Override
    public void addEntity(PlanActions plan) throws SQLException {
        List<String> errors = validate(plan);
        if (!errors.isEmpty()) {
            throw new SQLException("Validation échouée : " + String.join(", ", errors));
        }
        if (existsByDecision(plan.getDecision())) {
            throw new SQLException("Un Plan d'Action avec cette décision existe déjà.");
        }

        String sql = "INSERT INTO plan_actions (etudiant_id, decision, description, date, statut, "
                   + "categorie, sortie_ai_id, auteur_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setObject(1, plan.getEtudiantId() > 0 ? plan.getEtudiantId() : null);
        pst.setString(2, plan.getDecision().trim());
        pst.setString(3, plan.getDescription().trim());
        pst.setTimestamp(4, Timestamp.valueOf(plan.getDate()));
        pst.setString(5, plan.getStatut().name());
        pst.setString(6, plan.getCategorie() != null ? plan.getCategorie().name() : null);
        int sortieAiId = plan.getSortieAIId();
        if (sortieAiId <= 0) {
            try (Statement st = cnx.createStatement(); ResultSet rs = st.executeQuery("SELECT id FROM sortie_ai LIMIT 1")) {
                if (rs.next()) {
                    sortieAiId = rs.getInt("id");
                } else {
                    throw new SQLException("Impossible de créer un Plan d'Actions : aucune IA décisionnelle n'existe.");
                }
            }
        }
        pst.setObject(7, sortieAiId);
        pst.setObject(8, plan.getAuteurId() > 0 ? plan.getAuteurId() : null);
        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            plan.setId(rs.getInt(1));
        }

        if (plan.getSortieAIId() > 0) {
            updateSortieAIStatut(plan.getSortieAIId(), "PLANIFIE");
        }
    }

    @Override
    public void updateEntity(int id, PlanActions plan) throws SQLException {
        List<String> errors = validate(plan);
        if (!errors.isEmpty()) {
            throw new SQLException("Validation échouée : " + String.join(", ", errors));
        }
        if (existsByDecisionExcluding(plan.getDecision(), id)) {
            throw new SQLException("Un autre Plan d'Action avec cette décision existe déjà.");
        }

        String sql = "UPDATE plan_actions SET decision = ?, description = ?, statut = ?, "
                   + "categorie = ?, updated_at = ?, etudiant_id = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, plan.getDecision().trim());
        pst.setString(2, plan.getDescription().trim());
        pst.setString(3, plan.getStatut().name());
        pst.setString(4, plan.getCategorie() != null ? plan.getCategorie().name() : null);
        pst.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
        pst.setObject(6, plan.getEtudiantId() > 0 ? plan.getEtudiantId() : null);
        pst.setInt(7, id);
        pst.executeUpdate();
    }

    @Override
    public void deleteEntity(PlanActions plan) throws SQLException {
        String sqlJoin = "DELETE FROM plan_actions_articles WHERE plan_actions_id = ?";
        PreparedStatement pstJoin = cnx.prepareStatement(sqlJoin);
        pstJoin.setInt(1, plan.getId());
        pstJoin.executeUpdate();

        String sql = "DELETE FROM plan_actions WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, plan.getId());
        pst.executeUpdate();
    }

    @Override
    public List<PlanActions> getData() throws SQLException {
        List<PlanActions> data = new ArrayList<>();
        String sql = "SELECT * FROM plan_actions ORDER BY date DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) data.add(mapResultSet(rs));
        return data;
    }

    public List<PlanActions> searchPlans(String search, String statut, String categorie, String sortBy, String order) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM plan_actions WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isEmpty()) {
            sql.append("AND (decision LIKE ? OR description LIKE ?) ");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (statut != null && !statut.isEmpty()) {
            sql.append("AND statut = ? ");
            params.add(statut);
        }

        if (categorie != null && !categorie.isEmpty()) {
            sql.append("AND categorie = ? ");
            params.add(categorie);
        }

        String[] validSorts = {"id", "decision", "date", "statut"};
        String finalSort = "date";
        for (String s : validSorts) {
            if (s.equals(sortBy)) { finalSort = sortBy; break; }
        }
        String finalOrder = "ASC".equalsIgnoreCase(order) ? "ASC" : "DESC";
        sql.append("ORDER BY ").append(finalSort).append(" ").append(finalOrder);

        PreparedStatement pst = cnx.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            pst.setString(i + 1, (String) params.get(i));
        }

        List<PlanActions> data = new ArrayList<>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) data.add(mapResultSet(rs));
        return data;
    }

    public PlanActions getById(int id) throws SQLException {
        String sql = "SELECT * FROM plan_actions WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return mapResultSet(rs);
        return null;
    }

    // ======================== FEEDBACK ========================

    public void addFeedback(int planId, String feedback, int feedbackAuteurId) throws SQLException {
        String sql = "UPDATE plan_actions SET feedback_enseignant = ?, feedback_date = ?, "
                   + "feedback_auteur_id = ?, updated_at = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, feedback);
        pst.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(3, feedbackAuteurId);
        pst.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(5, planId);
        pst.executeUpdate();
    }

    public List<PlanActions> getRecentFeedbacks() throws SQLException {
        List<PlanActions> data = new ArrayList<>();
        String sql = "SELECT * FROM plan_actions WHERE feedback_enseignant IS NOT NULL AND feedback_enseignant != '' ORDER BY feedback_date DESC LIMIT 10";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) data.add(mapResultSet(rs));
        return data;
    }

    public List<PlanActions> getRecentUserFeedbacks(int auteurId) throws SQLException {
        List<PlanActions> data = new ArrayList<>();
        String sql = "SELECT * FROM plan_actions WHERE feedback_enseignant IS NOT NULL AND feedback_enseignant != '' AND feedback_auteur_id = ? ORDER BY feedback_date DESC LIMIT 10";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, auteurId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) data.add(mapResultSet(rs));
        return data;
    }

    // ======================== RELATIONS ARTICLES ========================

    public void addArticleToPlan(int planId, int articleId) throws SQLException {
        String sql = "INSERT IGNORE INTO plan_actions_articles (plan_actions_id, reference_article_id) VALUES (?, ?)";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, planId);
        pst.setInt(2, articleId);
        pst.executeUpdate();
    }

    public void removeArticleFromPlan(int planId, int articleId) throws SQLException {
        String sql = "DELETE FROM plan_actions_articles WHERE plan_actions_id = ? AND reference_article_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, planId);
        pst.setInt(2, articleId);
        pst.executeUpdate();
    }

    public List<Integer> getArticleIds(int planId) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT reference_article_id FROM plan_actions_articles WHERE plan_actions_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, planId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) ids.add(rs.getInt(1));
        return ids;
    }

    // ======================== STATISTIQUES ========================

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM plan_actions";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    public Map<String, Integer> countByStatut() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT statut, COUNT(*) as cnt FROM plan_actions GROUP BY statut";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) stats.put(rs.getString("statut"), rs.getInt("cnt"));
        return stats;
    }

    public Map<String, Integer> countByCategorie() throws SQLException {
        Map<String, Integer> stats = new HashMap<>();
        String sql = "SELECT categorie, COUNT(*) as cnt FROM plan_actions GROUP BY categorie";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            if (rs.getString("categorie") != null) {
                stats.put(rs.getString("categorie"), rs.getInt("cnt"));
            }
        }
        return stats;
    }

    private void updateSortieAIStatut(int sortieId, String statut) throws SQLException {
        String sql = "UPDATE sortie_ai SET statut = ?, updated_at = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, statut);
        pst.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(3, sortieId);
        pst.executeUpdate();
    }

    // ======================== MAPPING ========================

    private PlanActions mapResultSet(ResultSet rs) throws SQLException {
        PlanActions plan = new PlanActions();
        plan.setId(rs.getInt("id"));
        plan.setEtudiantId(rs.getInt("etudiant_id"));
        plan.setDecision(rs.getString("decision"));
        plan.setDescription(rs.getString("description"));

        Timestamp dateTs = rs.getTimestamp("date");
        if (dateTs != null) plan.setDate(dateTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) plan.setUpdatedAt(updatedTs.toLocalDateTime());

        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            try { plan.setStatut(Statut.valueOf(statutStr)); } catch (IllegalArgumentException ignored) {}
        }

        String catStr = rs.getString("categorie");
        if (catStr != null) {
            try { plan.setCategorie(CategorieSortie.valueOf(catStr)); } catch (IllegalArgumentException ignored) {}
        }

        plan.setSortieAIId(rs.getInt("sortie_ai_id"));
        plan.setFeedbackEnseignant(rs.getString("feedback_enseignant"));

        Timestamp fbTs = rs.getTimestamp("feedback_date");
        if (fbTs != null) plan.setFeedbackDate(fbTs.toLocalDateTime());

        plan.setFeedbackAuteurId(rs.getInt("feedback_auteur_id"));
        
        plan.setAuteurId(rs.getInt("auteur_id"));

        return plan;
    }
}
