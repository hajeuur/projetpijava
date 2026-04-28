package edu.connection3a36.services;

/**
 * Fournit des données simulées (mockées) pour enrichir le contexte de l'IA
 * concernant les étudiants et scénarios pédagogiques.
 */
public class MockDataService {

    /**
     * Retourne un contexte détaillé sur 10 étudiants types de la classe 3A36.
     * Inclut des profils variés : excellents, à risque, difficultés spécifiques.
     */
    public static String getEtudiants3A36Context() {
        return """
        
        DONNÉES ACTUELLES DE LA CLASSE 3A36 (À utiliser comme contexte si on te pose des questions sur les étudiants) :
        
        1. Amine Ben Ali (ID: 101) - Statut : Excellent. Moyenne : 18.5. Absences : 0. Profil : Très engagé, participe activement. Aide souvent ses camarades.
        2. Sarah Trabelsi (ID: 102) - Statut : À risque (Décrochage). Moyenne : 09.2. Absences : 5. Profil : Baisse brutale de notes depuis 1 mois. Semble isolée.
        3. Youssef Gharbi (ID: 103) - Statut : Difficultés d'attention. Moyenne : 11.5. Absences : 1. Profil : Perturbateur en classe mais montre de bonnes capacités pratiques.
        4. Mariem Jlassi (ID: 104) - Statut : Dyslexique. Moyenne : 13.0. Absences : 2. Profil : Temps supplémentaire nécessaire aux examens. Très créative.
        5. Ahmed Khelil (ID: 105) - Statut : Moyen. Moyenne : 12.5. Absences : 0. Profil : Discret, régulier. A besoin d'encouragement pour participer.
        6. Nour Hentati (ID: 106) - Statut : Excellente (Technique). Moyenne : 16.0. Absences : 1. Profil : Brillante en programmation, difficultés mineures en communication/soft skills.
        7. Rayen Khemiri (ID: 107) - Statut : À risque (Absentéisme). Moyenne : 08.5. Absences : 8. Profil : Ne vient souvent pas aux TD du matin. Doit être convoqué.
        8. Eya Rekik (ID: 108) - Statut : Performante. Moyenne : 15.2. Absences : 0. Profil : Déléguée de classe, très responsable.
        9. Mehdi Bouaziz (ID: 109) - Statut : Irrégulier. Moyenne : 10.8. Absences : 3. Profil : De très bonnes notes suivies de très mauvaises. Manque de méthode de travail.
        10. Farah Mansour (ID: 110) - Statut : En progression. Moyenne : 14.1. Absences : 0. Profil : Était en difficulté en début d'année, forte amélioration suite au dernier plan d'action.
        
        SCÉNARIO TYPE : Si on te demande d'analyser ou de créer un plan pour Sarah (102) ou Rayen (107), 
        suggère un plan d'action de remédiation immédiat impliquant l'enseignant, la psychologue de l'école et les parents.
        """;
    }
}
