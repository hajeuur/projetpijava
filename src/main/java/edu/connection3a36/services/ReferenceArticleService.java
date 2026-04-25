package edu.connection3a36.services;

import edu.connection3a36.entities.ReferenceArticle;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour ReferenceArticle.
 * Fonctionnalités : CRUD complet + recherche multi-critères + toggle publication + tri + pagination.
 */
public class ReferenceArticleService implements IService<ReferenceArticle> {

    private final Connection cnx;

    public ReferenceArticleService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ======================== VALIDATION ========================
    
    public List<String> validate(ReferenceArticle article) throws SQLException {
        List<String> errors = new ArrayList<>();
        if (article.getTitre() == null || article.getTitre().trim().isEmpty()) {
            errors.add("Le titre est obligatoire");
        } else if (article.getTitre().trim().length() < 3) {
            errors.add("Le titre doit contenir au moins 3 caractères");
        }
        
        if (article.getContenu() == null || article.getContenu().trim().isEmpty()) {
            errors.add("Le contenu est obligatoire");
        } else if (article.getContenu().trim().length() < 10) {
            errors.add("Le contenu est trop court");
        }
        
        if (article.getCategorieId() <= 0) {
            errors.add("Veuillez sélectionner une catégorie valide");
        }
        return errors;
    }

    public boolean existsByTitre(String titre) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article WHERE LOWER(titre) = LOWER(?)";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, titre.trim());
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean existsByTitreExcluding(String titre, int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article WHERE LOWER(titre) = LOWER(?) AND id != ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, titre.trim());
        pst.setInt(2, id);
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    // ======================== CRUD ========================

    @Override
    public void addEntity(ReferenceArticle article) throws SQLException {
        List<String> errors = validate(article);
        if (!errors.isEmpty()) throw new SQLException(String.join("\n", errors));
        if (existsByTitre(article.getTitre())) throw new SQLException("Erreur: Un article avec ce titre existe déjà (unicité).");
        
        String sql = "INSERT INTO reference_article (titre, contenu, categorie_id, auteur_id, created_at, published) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, article.getTitre());
        pst.setString(2, article.getContenu());
        pst.setInt(3, article.getCategorieId());
        pst.setInt(4, article.getAuteurId());
        pst.setTimestamp(5, Timestamp.valueOf(article.getCreatedAt()));
        pst.setBoolean(6, article.isPublished());
        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            article.setId(rs.getInt(1));
        }
        System.out.println("✅ Article ajouté : " + article.getTitre());
    }

    @Override
    public void updateEntity(int id, ReferenceArticle article) throws SQLException {
        List<String> errors = validate(article);
        if (!errors.isEmpty()) throw new SQLException(String.join("\n", errors));
        if (existsByTitreExcluding(article.getTitre(), id)) throw new SQLException("Erreur: Un autre article avec ce titre existe déjà.");
        
        String sql = "UPDATE reference_article SET titre = ?, contenu = ?, categorie_id = ?, "
                   + "published = ?, updated_at = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, article.getTitre());
        pst.setString(2, article.getContenu());
        pst.setInt(3, article.getCategorieId());
        pst.setBoolean(4, article.isPublished());
        pst.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(6, id);
        pst.executeUpdate();
        System.out.println("✅ Article modifié : " + article.getTitre());
    }

    @Override
    public void deleteEntity(ReferenceArticle article) throws SQLException {
        // Supprimer d'abord les relations ManyToMany
        String sqlJoin1 = "DELETE FROM plan_actions_articles WHERE reference_article_id = ?";
        PreparedStatement pst1 = cnx.prepareStatement(sqlJoin1);
        pst1.setInt(1, article.getId());
        pst1.executeUpdate();

        String sqlJoin2 = "DELETE FROM sortie_ai_articles WHERE reference_article_id = ?";
        PreparedStatement pst2 = cnx.prepareStatement(sqlJoin2);
        pst2.setInt(1, article.getId());
        pst2.executeUpdate();

        // Puis supprimer l'article
        String sql = "DELETE FROM reference_article WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, article.getId());
        pst.executeUpdate();
        System.out.println("✅ Article supprimé : " + article.getTitre());
    }

    @Override
    public List<ReferenceArticle> getData() throws SQLException {
        List<ReferenceArticle> data = new ArrayList<>();
        String sql = "SELECT r.*, c.nom_categorie FROM reference_article r "
                   + "LEFT JOIN categorie_article c ON r.categorie_id = c.id "
                   + "ORDER BY r.created_at DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            data.add(mapResultSet(rs));
        }
        return data;
    }

    // ======================== RECHERCHE & FILTRAGE ========================

    /**
     * Recherche avancée avec filtres multiples
     */
    public List<ReferenceArticle> searchArticles(String search, Integer categorieId, Boolean published) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT r.*, c.nom_categorie FROM reference_article r "
              + "LEFT JOIN categorie_article c ON r.categorie_id = c.id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isEmpty()) {
            sql.append("AND (r.titre LIKE ? OR r.contenu LIKE ?) ");
            params.add("%" + search + "%");
            params.add("%" + search + "%");
        }

        if (categorieId != null && categorieId > 0) {
            sql.append("AND c.id = ? ");
            params.add(categorieId);
        }

        if (published != null) {
            sql.append("AND r.published = ? ");
            params.add(published);
        }

        sql.append("ORDER BY r.created_at DESC");

        PreparedStatement pst = cnx.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            Object param = params.get(i);
            if (param instanceof String) pst.setString(i + 1, (String) param);
            else if (param instanceof Integer) pst.setInt(i + 1, (Integer) param);
            else if (param instanceof Boolean) pst.setBoolean(i + 1, (Boolean) param);
        }

        List<ReferenceArticle> data = new ArrayList<>();
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            data.add(mapResultSet(rs));
        }
        return data;
    }

    /**
     * Récupérer un article par ID
     */
    public ReferenceArticle getById(int id) throws SQLException {
        String sql = "SELECT r.*, c.nom_categorie FROM reference_article r "
                   + "LEFT JOIN categorie_article c ON r.categorie_id = c.id WHERE r.id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSet(rs);
        }
        return null;
    }

    /**
     * Récupérer uniquement les articles publiés
     */
    public List<ReferenceArticle> getPublished() throws SQLException {
        return searchArticles(null, null, true);
    }

    // ======================== TOGGLE PUBLICATION ========================

    /**
     * Basculer l'état de publication d'un article (publié ↔ brouillon)
     */
    public void togglePublish(int articleId) throws SQLException {
        String sql = "UPDATE reference_article SET published = NOT published, updated_at = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(2, articleId);
        pst.executeUpdate();
        System.out.println("✅ Publication basculée pour article #" + articleId);
    }

    // ======================== STATISTIQUES ========================

    /**
     * Nombre total d'articles
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    /**
     * Nombre d'articles publiés
     */
    public int countPublished() throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article WHERE published = 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    /**
     * Nombre d'articles récents (derniers N jours)
     */
    public int countRecentArticles(int days) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article WHERE published = 1 AND created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, days);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ======================== MAPPING ========================

    private ReferenceArticle mapResultSet(ResultSet rs) throws SQLException {
        ReferenceArticle article = new ReferenceArticle();
        article.setId(rs.getInt("id"));
        article.setTitre(rs.getString("titre"));
        article.setContenu(rs.getString("contenu"));
        article.setCategorieId(rs.getInt("categorie_id"));
        article.setAuteurId(rs.getInt("auteur_id"));
        article.setPublished(rs.getBoolean("published"));

        // Champ transient pour l'affichage
        try {
            article.setCategorieNom(rs.getString("nom_categorie"));
        } catch (SQLException ignored) {
            // Le champ peut ne pas être dans le ResultSet si pas de JOIN
        }

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) article.setCreatedAt(createdTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) article.setUpdatedAt(updatedTs.toLocalDateTime());

        return article;
    }
}
