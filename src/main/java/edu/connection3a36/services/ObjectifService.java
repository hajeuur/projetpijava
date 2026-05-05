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

/**
 * ============================================================
 * SERVICE : ObjectifService
 * ============================================================
 * Gère toutes les opérations CRUD sur les objectifs personnels.
 *
 * PARTICULARITÉ IMPORTANTE — CRÉATION EN 2 ÉTAPES :
 * Quand on crée un objectif, on crée AUSSI automatiquement un Programme
 * lié. C'est une règle métier fondamentale : tout objectif a son programme.
 *
 * FLUX DE CRÉATION :
 * 1. Insérer un Programme en BDD → récupérer son ID
 * 2. Insérer l'Objectif en BDD avec le programme_id
 *
 * FLUX DE SUPPRESSION (cascade) :
 * 1. Supprimer les Motivations du programme
 * 2. Supprimer les Tâches du programme
 * 3. Supprimer l'Objectif
 * 4. Supprimer le Programme
 *
 * CONNEXION BDD :
 * Utilise MyConnection (pattern Singleton) pour obtenir une connexion
 * unique partagée dans toute l'application.
 * ============================================================
 */
public class ObjectifService implements IService<Objectif> {

    /**
     * Connexion unique à la base de données MySQL.
     * MyConnection.getInstance() retourne toujours la même instance (Singleton).
     */
    private final Connection cnx = MyConnection.getInstance().getCnx();

    // ─────────────────────────────────────────────────────────────────────────
    // CRÉATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée un nouvel objectif ET son programme associé en base de données.
     *
     * ÉTAPE 1 : Créer le Programme
     * - Titre : "Programme — [titre de l'objectif]"
     * - Score initial : 0%
     * - Médaille initiale : null
     *
     * ÉTAPE 2 : Créer l'Objectif
     * - Lié au programme créé à l'étape 1 via programme_id
     * - Statut initial : EnCours
     *
     * @param o L'objectif à créer (son ID et programme seront mis à jour)
     * @throws SQLException Si la création du programme ou de l'objectif échoue
     */
    @Override
    public void addEntity(Objectif o) throws SQLException {

        // ════════════════════════════════════════════════════════════════════
        // POURQUOI 2 ÉTAPES ?
        // La BDD exige que le programme existe AVANT l'objectif car la table
        // "objectif" a une colonne "programme_id" qui est une clé étrangère
        // vers la table "programme". On ne peut pas insérer l'objectif sans
        // connaître l'ID du programme. Donc : programme d'abord, objectif ensuite.
        // ════════════════════════════════════════════════════════════════════

        // ── ÉTAPE 1 : Créer le Programme ──────────────────────────────────────
        String reqProg = "INSERT INTO programme (titre, dategeneration, score_pourcentage, meilleure_medaille) VALUES (?,?,?,?)";

        // Statement.RETURN_GENERATED_KEYS : demande à MySQL de nous retourner
        // l'ID auto-incrémenté qui vient d'être généré après le INSERT
        PreparedStatement pstProg = cnx.prepareStatement(reqProg, Statement.RETURN_GENERATED_KEYS);
        pstProg.setString(1, "Programme — " + o.getTitre()); // Titre automatique
        pstProg.setDate(2, Date.valueOf(LocalDate.now()));    // Date du jour
        pstProg.setInt(3, 0);                                 // Score initial = 0%
        pstProg.setNull(4, Types.VARCHAR);                    // Pas encore de médaille
        pstProg.executeUpdate();

        // getGeneratedKeys() récupère l'ID que MySQL vient de générer (AUTO_INCREMENT)
        // rs.next() positionne le curseur sur la première (et unique) ligne de résultat
        ResultSet rsProg = pstProg.getGeneratedKeys();
        if (!rsProg.next()) throw new SQLException("Impossible de créer le programme.");
        int programmeId = rsProg.getInt(1); // colonne 1 = l'ID généré

        // Créer l'objet Programme en mémoire et l'associer à l'objectif
        Programme prog = new Programme("Programme — " + o.getTitre(), LocalDate.now());
        prog.setId(programmeId);
        o.setProgramme(prog); // L'objectif connaît maintenant son programme

        // ── ÉTAPE 2 : Créer l'Objectif ────────────────────────────────────────
        String req = "INSERT INTO objectif (titre, description, datedebut, datefin, statut, programme_id, utilisateur_id) VALUES (?,?,?,?,?,?,?)";
        PreparedStatement pst = cnx.prepareStatement(req, Statement.RETURN_GENERATED_KEYS);
        pst.setString(1, o.getTitre());
        pst.setString(2, o.getDescription() != null ? o.getDescription() : ""); // Éviter null en BDD
        pst.setDate(3, o.getDatedebut() != null ? Date.valueOf(o.getDatedebut()) : Date.valueOf(LocalDate.now()));
        pst.setDate(4, o.getDatefin() != null ? Date.valueOf(o.getDatefin()) : null);
        pst.setString(5, o.getStatut() != null ? o.getStatut().getValue() : Statutobj.EnCours.getValue());
        pst.setInt(6, programmeId);       // Lien vers le programme créé à l'étape 1
        pst.setInt(7, o.getUtilisateurId());
        pst.executeUpdate();

        // Récupérer l'ID de l'objectif créé (même technique que pour le programme)
        ResultSet rs = pst.getGeneratedKeys();
        if (rs.next()) o.setId(rs.getInt(1));

        System.out.println("✅ Objectif id=" + o.getId() + " + Programme id=" + programmeId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPPRESSION (cascade)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Supprime un objectif et TOUT ce qui lui est lié (cascade).
     *
     * ORDRE DE SUPPRESSION (important pour respecter les clés étrangères) :
     * 1. Motivations du programme (dépendent du programme)
     * 2. Tâches du programme (dépendent du programme)
     * 3. L'objectif lui-même
     * 4. Le programme (dépend de l'objectif via programme_id)
     *
     * @param o L'objectif à supprimer
     * @throws SQLException En cas d'erreur SQL
     */
    @Override
    public void deleteEntity(Objectif o) throws SQLException {

        // ════════════════════════════════════════════════════════════════════
        // POURQUOI CET ORDRE PRÉCIS ?
        // MySQL refuse de supprimer un enregistrement si d'autres tables
        // y font référence via une clé étrangère (FK = Foreign Key).
        //
        // Dépendances :
        //   motivation.programme_id → programme.id  (dépend du programme)
        //   tache.programme_id      → programme.id  (dépend du programme)
        //   objectif.programme_id   → programme.id  (dépend du programme)
        //
        // Donc l'ordre OBLIGATOIRE est :
        //   1. Supprimer motivations  (elles dépendent du programme)
        //   2. Supprimer tâches       (elles dépendent du programme)
        //   3. Supprimer l'objectif   (il dépend du programme)
        //   4. Supprimer le programme (plus rien ne le référence)
        //
        // Si on supprime le programme en premier → MySQL lève une erreur FK.
        // ════════════════════════════════════════════════════════════════════

        // Récupérer l'ID du programme AVANT de supprimer l'objectif
        // (après suppression de l'objectif, on ne pourrait plus retrouver le programme_id)
        int programmeId = getProgrammeId(o.getId());

        if (programmeId > 0) {
            // 1. Supprimer les messages de motivation liés au programme
            new MotivationService().deleteByProgramme(programmeId);
            // 2. Supprimer les tâches liées au programme
            new TacheService().deleteByProgramme(programmeId);
        }

        // 3. Supprimer l'objectif
        PreparedStatement pstObj = cnx.prepareStatement("DELETE FROM objectif WHERE id = ?");
        pstObj.setInt(1, o.getId());
        pstObj.executeUpdate();

        // 4. Supprimer le programme (après l'objectif pour éviter les contraintes FK)
        if (programmeId > 0) {
            PreparedStatement pstProg = cnx.prepareStatement("DELETE FROM programme WHERE id = ?");
            pstProg.setInt(1, programmeId);
            pstProg.executeUpdate();
        }

        System.out.println("🗑️ Objectif supprimé : " + o.getTitre());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MODIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Met à jour les informations d'un objectif existant.
     * NOTE : Le programme_id n'est PAS modifié (relation permanente).
     * NOTE : Le statut est mis à jour ici mais sera recalculé automatiquement
     *        par ScoreService à chaque changement de tâche.
     *
     * @param id L'ID de l'objectif à modifier
     * @param o  L'objectif avec les nouvelles valeurs
     * @throws SQLException En cas d'erreur SQL
     */
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

    // ─────────────────────────────────────────────────────────────────────────
    // LECTURE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Récupère TOUS les objectifs de la base de données (tous utilisateurs).
     * Utilisé par l'admin dans DashboardObjectifsAdminController.
     *
     * @return Liste de tous les objectifs, triés par date de début décroissante
     * @throws SQLException En cas d'erreur SQL
     */
    @Override
    public List<Objectif> getData() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM objectif ORDER BY datedebut DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /**
     * Récupère les objectifs d'un utilisateur spécifique.
     * Utilisé dans ObjectifListController pour afficher "Mes Objectifs".
     *
     * @param utilisateurId L'ID de l'utilisateur connecté
     * @return Liste des objectifs de cet utilisateur
     * @throws SQLException En cas d'erreur SQL
     */
    public List<Objectif> getByUtilisateur(int utilisateurId) throws SQLException {
        List<Objectif> list = new ArrayList<>();
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM objectif WHERE utilisateur_id = ? ORDER BY datedebut DESC");
        pst.setInt(1, utilisateurId);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /**
     * Récupère un objectif par son ID.
     * Utilisé pour recharger un objectif après modification.
     *
     * @param id L'ID de l'objectif
     * @return L'objectif trouvé, ou null si inexistant
     * @throws SQLException En cas d'erreur SQL
     */
    public Objectif getById(int id) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement("SELECT * FROM objectif WHERE id = ?");
        pst.setInt(1, id);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    /**
     * Récupère l'ID du programme lié à un objectif.
     * Utilisé avant la suppression pour faire la cascade.
     *
     * @param objectifId L'ID de l'objectif
     * @return L'ID du programme, ou 0 si non trouvé
     * @throws SQLException En cas d'erreur SQL
     */
    public int getProgrammeId(int objectifId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT programme_id FROM objectif WHERE id = ?");
        pst.setInt(1, objectifId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt("programme_id");
        return 0;
    }

    /**
     * Trouve l'objectif lié à un programme (relation inverse).
     * Utilisé par ScoreService pour mettre à jour le statut de l'objectif
     * après recalcul du score.
     *
     * @param programmeId L'ID du programme
     * @return L'objectif lié, ou null si non trouvé
     * @throws SQLException En cas d'erreur SQL
     */
    public Objectif getByProgrammeId(int programmeId) throws SQLException {
        PreparedStatement pst = cnx.prepareStatement(
                "SELECT * FROM objectif WHERE programme_id = ?");
        pst.setInt(1, programmeId);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return map(rs);
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Valide les données d'un objectif avant insertion ou modification.
     * Retourne une liste d'erreurs (vide = données valides).
     *
     * RÈGLES DE VALIDATION :
     * - Titre obligatoire et ≥ 3 caractères
     * - Date de début obligatoire
     * - Date de fin obligatoire
     * - Date de fin doit être après la date de début
     * - Utilisateur obligatoire (ID > 0)
     *
     * @param o L'objectif à valider
     * @return Liste des messages d'erreur (vide si tout est valide)
     */
    public List<String> validate(Objectif o) {
        List<String> erreurs = new ArrayList<>();

        // Validation du titre
        if (o.getTitre() == null || o.getTitre().isBlank())
            erreurs.add("Le titre est obligatoire");
        else if (o.getTitre().trim().length() < 3)
            erreurs.add("Le titre doit contenir au moins 3 caractères");

        // Validation des dates
        if (o.getDatedebut() == null)
            erreurs.add("La date de debut est obligatoire");
        if (o.getDatefin() == null)
            erreurs.add("La date de fin est obligatoire");

        // Cohérence des dates
        if (o.getDatedebut() != null && o.getDatefin() != null
                && o.getDatefin().isBefore(o.getDatedebut()))
            erreurs.add("La date de fin doit etre apres la date de debut");

        // Validation de l'utilisateur
        if (o.getUtilisateurId() <= 0)
            erreurs.add("L utilisateur est obligatoire");

        return erreurs;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPING ResultSet → Objectif
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Convertit une ligne de la base de données (ResultSet) en objet Objectif.
     * Appelé après chaque requête SELECT.
     *
     * NOTE : Le Programme est créé avec seulement son ID (lazy loading).
     * Les détails du programme sont chargés séparément si nécessaire.
     *
     * @param rs Le ResultSet positionné sur la ligne à lire
     * @return Un objet Objectif rempli avec les données de la BDD
     * @throws SQLException En cas d'erreur de lecture
     */
    private Objectif map(ResultSet rs) throws SQLException {
        Objectif o = new Objectif();
        o.setId(rs.getInt("id"));
        o.setTitre(rs.getString("titre"));
        o.setDescription(rs.getString("description"));

        // java.sql.Date (type BDD) ≠ java.time.LocalDate (type Java moderne)
        // On doit convertir avec .toLocalDate() — retourne null si la date est null en BDD
        Date dd = rs.getDate("datedebut");
        if (dd != null) o.setDatedebut(dd.toLocalDate());
        Date df = rs.getDate("datefin");
        if (df != null) o.setDatefin(df.toLocalDate());

        // fromValue() peut lever une exception si la valeur en BDD est corrompue
        // → on attrape l'exception et on met EnCours par défaut pour ne pas planter
        try { o.setStatut(Statutobj.fromValue(rs.getString("statut"))); }
        catch (Exception e) { o.setStatut(Statutobj.EnCours); }

        o.setUtilisateurId(rs.getInt("utilisateur_id"));

        // ════════════════════════════════════════════════════════════════════
        // LAZY LOADING : on crée un Programme "léger" avec seulement l'ID.
        // On ne charge PAS tous les détails du programme ici pour éviter
        // le problème N+1 (si on a 50 objectifs, on ferait 50 requêtes
        // supplémentaires pour charger les programmes).
        // Les détails sont chargés séparément quand nécessaire via
        // programmeService.getById(prog.getId()).
        // ════════════════════════════════════════════════════════════════════
        int progId = rs.getInt("programme_id");
        if (progId > 0) {
            Programme p = new Programme();
            p.setId(progId); // seulement l'ID, pas les autres champs
            o.setProgramme(p);
        }

        return o;
    }
}
