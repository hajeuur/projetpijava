package com.mentorai.services;

import com.mentorai.entities.PlanningEtude;
import com.mentorai.utils.MyConnection;

import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Service layer – all SQL lives here.
 * Mirrors every route in PlanningEtudeController.php.
 */
public class PlanningService {

    // ══════════════════════════════════════════════════════════════════════
    // READ
    // ══════════════════════════════════════════════════════════════════════

    /** All activities for the ISO week that contains {@code date}. */
    public List<PlanningEtude> findByWeek(LocalDate date) throws SQLException {
        LocalDate monday = date.with(java.time.DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);
        return findByDateRange(monday, sunday);
    }

    /** Range-inclusive query. */
    public List<PlanningEtude> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        String sql = "SELECT * FROM planning_etude " +
                "WHERE date_seance BETWEEN ? AND ? " +
                "ORDER BY date_seance, heure_debut";
        List<PlanningEtude> list = new ArrayList<>();
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** All activities on a single day. */
    public List<PlanningEtude> findByDate(LocalDate date) throws SQLException {
        String sql = "SELECT * FROM planning_etude " +
                "WHERE date_seance = ? " +
                "ORDER BY heure_debut";
        List<PlanningEtude> list = new ArrayList<>();
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Single activity by PK. */
    public Optional<PlanningEtude> findById(int id) throws SQLException {
        String sql = "SELECT * FROM planning_etude WHERE id = ?";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Distinct type + color pairs, ordered by usage frequency desc.
     * Returns list of String[2]: [type, color].
     */
    public List<String[]> findDistinctTypesWithColor() throws SQLException {
        String sql = "SELECT type_activite, couleur_activite, COUNT(*) as cnt " +
                "FROM planning_etude " +
                "WHERE type_activite IS NOT NULL AND type_activite <> '' " +
                "GROUP BY type_activite, couleur_activite " +
                "ORDER BY cnt DESC";
        List<String[]> list = new ArrayList<>();
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Set<String> seen = new LinkedHashSet<>();
            while (rs.next()) {
                String type = rs.getString("type_activite");
                if (seen.add(type)) {
                    list.add(new String[]{type, rs.getString("couleur_activite")});
                }
            }
        }
        return list;
    }

    /** Color already used for a given type (returns null if none found). */
    public String findColorForType(String type) throws SQLException {
        String sql = "SELECT couleur_activite FROM planning_etude " +
                "WHERE type_activite = ? AND couleur_activite IS NOT NULL " +
                "ORDER BY id DESC LIMIT 1";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("couleur_activite");
            }
        }
        return null;
    }

    /**
     * Suggest time/duration based on the last activity whose title starts
     * with the given prefix (case-insensitive).
     * Returns Optional<PlanningEtude> (only heureDebut + dureePrevue are used).
     */
    public Optional<PlanningEtude> findLastByTitlePrefix(String prefix) throws SQLException {
        String sql = "SELECT * FROM planning_etude " +
                "WHERE LOWER(titre_p) LIKE ? " +
                "ORDER BY id DESC LIMIT 1";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        }
        return Optional.empty();
    }

    // ══════════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Persist a new activity.
     * Returns the generated PK (sets it on the entity too).
     * Throws {@link IllegalArgumentException} if validation fails,
     * containing a newline-separated list of errors.
     */
    public PlanningEtude create(PlanningEtude p) throws SQLException {
        validate(p, -1);

        // Auto-assign color from type if blank
        if (p.getCouleurActivite() == null || p.getCouleurActivite().isBlank()) {
            String c = findColorForType(p.getTypeActivite());
            p.setCouleurActivite(c != null ? c : "#dfe6e9");
        }

        // Overlap check
        String overlap = findOverlapMessage(p.getDateSeance(), p.getHeureDebut(),
                p.getDureePrevue(), -1);
        if (overlap != null) throw new IllegalArgumentException(overlap);

        String sql = "INSERT INTO planning_etude " +
                "(titre_p, date_seance, heure_debut, heure_fin, matiere, type_activite, " +
                "description, notes_pers, duree_prevue, duree_reelle, etat, " +
                "couleur_activite, date_creation, date_modification, utilisateur_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindAll(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════════

    public PlanningEtude update(PlanningEtude p) throws SQLException {
        validate(p, p.getId());

        String overlap = findOverlapMessage(p.getDateSeance(), p.getHeureDebut(),
                p.getDureePrevue(), p.getId());
        if (overlap != null) throw new IllegalArgumentException(overlap);

        String sql = "UPDATE planning_etude SET " +
                "titre_p=?, date_seance=?, heure_debut=?, heure_fin=?, matiere=?, " +
                "type_activite=?, description=?, notes_pers=?, duree_prevue=?, " +
                "duree_reelle=?, etat=?, couleur_activite=?, date_modification=? " +
                "WHERE id=?";

        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getTitreP());
            ps.setDate(2, Date.valueOf(p.getDateSeance()));
            ps.setTime(3, p.getHeureDebut() != null ? Time.valueOf(p.getHeureDebut()) : null);
            ps.setTime(4, p.getHeureFin() != null ? Time.valueOf(p.getHeureFin()) : null);
            ps.setString(5, p.getMatiere());
            ps.setString(6, p.getTypeActivite());
            ps.setString(7, p.getDescription());
            ps.setString(8, p.getNotesPers());
            if (p.getDureePrevue() != null) ps.setInt(9, p.getDureePrevue()); else ps.setNull(9, Types.INTEGER);
            if (p.getDureeReelle() != null) ps.setInt(10, p.getDureeReelle()); else ps.setNull(10, Types.INTEGER);
            ps.setString(11, p.getEtat());
            ps.setString(12, p.getCouleurActivite());
            ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(14, p.getId());
            ps.executeUpdate();
        }
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // MOVE  (drag & drop)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Move activity to a new date + time.
     * Keeps duration unchanged. Validates overlap.
     */
    public PlanningEtude move(int id, LocalDate newDate, LocalTime newStart) throws SQLException {
        PlanningEtude p = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activité introuvable."));

        if (p.getDureePrevue() == null || p.getDureePrevue() <= 0)
            throw new IllegalArgumentException("La durée prévue est invalide.");

        String overlap = findOverlapMessage(newDate, newStart, p.getDureePrevue(), id);
        if (overlap != null) throw new IllegalArgumentException(overlap);

        String sql = "UPDATE planning_etude SET date_seance=?, heure_debut=?, date_modification=? WHERE id=?";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDate));
            ps.setTime(2, Time.valueOf(newStart));
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, id);
            ps.executeUpdate();
        }
        p.setDateSeance(newDate);
        p.setHeureDebut(newStart);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    // DELETE
    // ══════════════════════════════════════════════════════════════════════

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM planning_etude WHERE id=?";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOGGLE  (done ↔ to do)
    // ══════════════════════════════════════════════════════════════════════

    public String toggle(int id) throws SQLException {
        PlanningEtude p = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activité introuvable."));
        String newEtat = "done".equals(p.getEtat()) ? "to do" : "done";
        String sql = "UPDATE planning_etude SET etat=?, date_modification=? WHERE id=?";
        Connection conn = MyConnection.getInstance();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newEtat);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, id);
            ps.executeUpdate();
        }
        return newEtat;
    }

    // ══════════════════════════════════════════════════════════════════════
    // REMINDERS
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Returns reminders for today:
     *   - "upcoming": activity starting in exactly 10 minutes
     *   - "inProgress": 30 minutes remaining while in progress
     *
     * Each entry: String[2] { id, message }
     */
    public Map<String, List<String[]>> getReminders() throws SQLException {
        List<PlanningEtude> activities = findByDate(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();

        List<String[]> upcoming = new ArrayList<>();
        List<String[]> inProgress = new ArrayList<>();

        for (PlanningEtude a : activities) {
            if ("done".equals(a.getEtat()) || "skipped".equals(a.getEtat())) continue;
            if (a.getHeureDebut() == null || a.getDureePrevue() == null) continue;

            LocalDateTime startDt = LocalDate.now().atTime(a.getHeureDebut());
            LocalDateTime endDt = startDt.plusMinutes(a.getDureePrevue());

            long minutesUntilStart = java.time.Duration.between(now, startDt).toMinutes();
            long minutesRemaining = java.time.Duration.between(now, endDt).toMinutes();

            if (minutesUntilStart == 10) {
                upcoming.add(new String[]{
                        String.valueOf(a.getId()),
                        "Vous devriez commencer \"" + a.getTitreP() + "\" dans 10 minutes."
                });
            }
            if (minutesRemaining == 30 && !now.isBefore(startDt)) {
                inProgress.add(new String[]{
                        String.valueOf(a.getId()),
                        "Vous avez planifié " + a.durationLabel() + " – il reste 30 minutes."
                });
            }
        }

        Map<String, List<String[]>> result = new LinkedHashMap<>();
        result.put("upcoming", upcoming);
        result.put("in_progress", inProgress);
        return result;
    }

    // ══════════════════════════════════════════════════════════════════════
    // VALIDATION  (mirrors PHP controller logic exactly)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Validate a PlanningEtude before persist/update.
     * {@code excludeId} = -1 for create, else the current row's PK.
     * Throws {@link IllegalArgumentException} with newline-joined error list.
     */
    public void validate(PlanningEtude p, int excludeId) {
        List<String> errors = new ArrayList<>();

        if (p.getDateSeance() == null)
            errors.add("La date de séance est obligatoire.");

        if (p.getTitreP() == null || p.getTitreP().isBlank())
            errors.add("Le titre est obligatoire.");

        if (p.getHeureDebut() == null)
            errors.add("Heure de début invalide (HH:MM).");

        if (p.getDureePrevue() == null || p.getDureePrevue() <= 0)
            errors.add("La durée prévue est invalide (doit être > 0).");

        if (p.getTypeActivite() == null || p.getTypeActivite().isBlank())
            errors.add("Le type d'activité est obligatoire.");

        String color = p.getCouleurActivite();
        if (color != null && !color.isBlank() && !color.matches("^#[0-9a-fA-F]{6}$"))
            errors.add("La couleur doit être au format hexadécimal (#RRGGBB).");

        if (!errors.isEmpty())
            throw new IllegalArgumentException(String.join("\n", errors));
    }

    // ══════════════════════════════════════════════════════════════════════
    // OVERLAP DETECTION
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Check whether [startTime, startTime+duration) overlaps any existing
     * activity on the same day (excluding {@code excludeId}).
     * Returns a human-readable message, or null if no overlap.
     */
    public String findOverlapMessage(LocalDate date, LocalTime startTime,
                                     int durationMinutes, int excludeId) throws SQLException {
        List<PlanningEtude> existing = findByDate(date);
        int newStart = toMinutes(startTime);
        int newEnd   = newStart + durationMinutes;

        for (PlanningEtude e : existing) {
            if (e.getId() == excludeId) continue;
            if (e.getHeureDebut() == null || e.getDureePrevue() == null) continue;
            int eStart = toMinutes(e.getHeureDebut());
            int eEnd   = eStart + e.getDureePrevue();
            if (newStart < eEnd && eStart < newEnd) {
                String titre = e.getTitreP() != null ? e.getTitreP() : "une activité";
                return "Vous avez déjà \"" + titre + "\" pendant ce créneau.";
            }
        }
        return null;
    }

    // ══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════

    private static int toMinutes(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /** Map a ResultSet row → PlanningEtude. */
    private PlanningEtude map(ResultSet rs) throws SQLException {
        PlanningEtude p = new PlanningEtude();
        p.setId(rs.getInt("id"));
        p.setTitreP(rs.getString("titre_p"));

        Date ds = rs.getDate("date_seance");
        if (ds != null) p.setDateSeance(ds.toLocalDate());

        Time hd = rs.getTime("heure_debut");
        if (hd != null) p.setHeureDebut(hd.toLocalTime());

        Time hf = rs.getTime("heure_fin");
        if (hf != null) p.setHeureFin(hf.toLocalTime());

        p.setMatiere(rs.getString("matiere"));
        p.setTypeActivite(rs.getString("type_activite"));
        p.setDescription(rs.getString("description"));
        p.setNotesPers(rs.getString("notes_pers"));

        int dp = rs.getInt("duree_prevue");
        p.setDureePrevue(rs.wasNull() ? null : dp);

        int dr = rs.getInt("duree_reelle");
        p.setDureeReelle(rs.wasNull() ? null : dr);

        p.setEtat(rs.getString("etat"));
        p.setCouleurActivite(rs.getString("couleur_activite"));

        Timestamp dc = rs.getTimestamp("date_creation");
        if (dc != null) p.setDateCreation(dc.toLocalDateTime());

        Timestamp dm = rs.getTimestamp("date_modification");
        if (dm != null) p.setDateModification(dm.toLocalDateTime());

        int uid = rs.getInt("utilisateur_id");
        p.setUtilisateurId(rs.wasNull() ? null : uid);

        return p;
    }

    /** Bind all columns for INSERT. */
    private void bindAll(PreparedStatement ps, PlanningEtude p) throws SQLException {
        ps.setString(1, p.getTitreP());
        ps.setDate(2, p.getDateSeance() != null ? Date.valueOf(p.getDateSeance()) : null);
        ps.setTime(3, p.getHeureDebut() != null ? Time.valueOf(p.getHeureDebut()) : null);
        ps.setTime(4, p.getHeureFin() != null ? Time.valueOf(p.getHeureFin()) : null);
        ps.setString(5, p.getMatiere());
        ps.setString(6, p.getTypeActivite());
        ps.setString(7, p.getDescription() != null ? p.getDescription() : "");
        ps.setString(8, p.getNotesPers());
        if (p.getDureePrevue() != null) ps.setInt(9, p.getDureePrevue()); else ps.setNull(9, Types.INTEGER);
        if (p.getDureeReelle() != null) ps.setInt(10, p.getDureeReelle()); else ps.setNull(10, Types.INTEGER);
        ps.setString(11, p.getEtat() != null ? p.getEtat() : "to do");
        ps.setString(12, p.getCouleurActivite());
        ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now()));
        ps.setTimestamp(14, null); // date_modification null on create
        if (p.getUtilisateurId() != null) ps.setInt(15, p.getUtilisateurId()); else ps.setNull(15, Types.INTEGER);
    }
}