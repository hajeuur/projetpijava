package edu.connection3a36.services;

import edu.connection3a36.entities.CategorieArticle;
import edu.connection3a36.interfaces.IService;
import edu.connection3a36.tools.MyConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour CategorieArticle.
 * Fonctionnalités : CRUD complet + recherche + contrôle d'unicité + contrôle d'intégrité.
 */
public class CategorieArticleService implements IService<CategorieArticle> {

    private final Connection cnx;

    public CategorieArticleService() {
        cnx = MyConnection.getInstance().getCnx();
    }

    // ======================== CRUD ========================

    @Override
    public void addEntity(CategorieArticle categorie) throws SQLException {
        // Contrôle d'unicité du nom
        if (existsByNom(categorie.getNomCategorie())) {
            throw new SQLException("Cette catégorie existe déjà : " + categorie.getNomCategorie());
        }

        String sql = "INSERT INTO categorie_article (nom_categorie, description, auteur_id, created_at) VALUES (?, ?, ?, ?)";
        PreparedStatement pst = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, categorie.getNomCategorie());
        pst.setString(2, categorie.getDescription());
        pst.setInt(3, categorie.getAuteurId());
        pst.setTimestamp(4, Timestamp.valueOf(categorie.getCreatedAt()));
        pst.executeUpdate();

        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) {
            categorie.setId(rs.getInt(1));
        }
        System.out.println("✅ Catégorie ajoutée : " + categorie.getNomCategorie());
    }

    @Override
    public void updateEntity(int id, CategorieArticle categorie) throws SQLException {
        // Contrôle d'unicité (exclure l'enregistrement courant)
        if (existsByNomExcluding(categorie.getNomCategorie(), id)) {
            throw new SQLException("Une autre catégorie porte déjà ce nom : " + categorie.getNomCategorie());
        }

        String sql = "UPDATE categorie_article SET nom_categorie = ?, description = ?, updated_at = ? WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, categorie.getNomCategorie());
        pst.setString(2, categorie.getDescription());
        pst.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
        pst.setInt(4, id);
        pst.executeUpdate();
        System.out.println("✅ Catégorie modifiée : " + categorie.getNomCategorie());
    }

    @Override
    public void deleteEntity(CategorieArticle categorie) throws SQLException {
        // Contrôle d'intégrité : vérifier si des articles sont liés
        int articleCount = countArticlesByCategorie(categorie.getId());
        if (articleCount > 0) {
            throw new SQLException("Impossible de supprimer cette catégorie car elle contient "
                    + articleCount + " article(s) !");
        }

        String sql = "DELETE FROM categorie_article WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, categorie.getId());
        pst.executeUpdate();
        System.out.println("✅ Catégorie supprimée : " + categorie.getNomCategorie());
    }

    @Override
    public List<CategorieArticle> getData() throws SQLException {
        List<CategorieArticle> data = new ArrayList<>();
        String sql = "SELECT * FROM categorie_article ORDER BY created_at DESC";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            data.add(mapResultSet(rs));
        }
        return data;
    }

    // ======================== RECHERCHE & FILTRAGE ========================

    /**
     * Recherche par nom ou description
     */
    public List<CategorieArticle> search(String keyword) throws SQLException {
        List<CategorieArticle> data = new ArrayList<>();
        String sql = "SELECT * FROM categorie_article WHERE nom_categorie LIKE ? OR description LIKE ? ORDER BY created_at DESC";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            data.add(mapResultSet(rs));
        }
        return data;
    }

    /**
     * Récupérer une catégorie par ID
     */
    public CategorieArticle getById(int id) throws SQLException {
        String sql = "SELECT * FROM categorie_article WHERE id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            return mapResultSet(rs);
        }
        return null;
    }

    // ======================== CONTRÔLES ========================

    /**
     * Vérifie si une catégorie existe déjà avec ce nom
     */
    public boolean existsByNom(String nomCategorie) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie_article WHERE LOWER(nom_categorie) = LOWER(?)";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, nomCategorie);
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    /**
     * Vérifie l'unicité en excluant un ID (pour la modification)
     */
    public boolean existsByNomExcluding(String nomCategorie, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie_article WHERE LOWER(nom_categorie) = LOWER(?) AND id != ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setString(1, nomCategorie);
        pst.setInt(2, excludeId);
        ResultSet rs = pst.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    /**
     * Compte les articles liés à une catégorie (contrôle d'intégrité)
     */
    public int countArticlesByCategorie(int categorieId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reference_article WHERE categorie_id = ?";
        PreparedStatement pst = cnx.prepareStatement(sql);
        pst.setInt(1, categorieId);
        ResultSet rs = pst.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ======================== STATISTIQUES ========================

    /**
     * Nombre total de catégories
     */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM categorie_article";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getInt(1) : 0;
    }

    // ======================== MAPPING ========================

    private CategorieArticle mapResultSet(ResultSet rs) throws SQLException {
        CategorieArticle cat = new CategorieArticle();
        cat.setId(rs.getInt("id"));
        cat.setNomCategorie(rs.getString("nom_categorie"));
        cat.setDescription(rs.getString("description"));
        cat.setAuteurId(rs.getInt("auteur_id"));

        Timestamp createdTs = rs.getTimestamp("created_at");
        if (createdTs != null) cat.setCreatedAt(createdTs.toLocalDateTime());

        Timestamp updatedTs = rs.getTimestamp("updated_at");
        if (updatedTs != null) cat.setUpdatedAt(updatedTs.toLocalDateTime());

        return cat;
    }
}
