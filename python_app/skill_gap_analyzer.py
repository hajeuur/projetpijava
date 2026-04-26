import sys
import json
import os

def analyze_gap(user_skills, target_job):
    # Dynamic path handling
    script_dir = os.path.dirname(os.path.abspath(__file__))
    json_path = os.path.join(script_dir, 'job_requirements.json')
    
    try:
        with open(json_path, 'r', encoding='utf-8') as f:
            job_data = json.load(f)
    except FileNotFoundError:
        return {"error": "Base de données de métiers introuvable."}
    
    if target_job not in job_data:
        return {"error": f"Le métier '{target_job}' n'est pas répertorié."}
    
    required_skills = job_data[target_job]
    user_skills_set = set([s.lower().strip() for s in user_skills])
    
    mastered = []
    missing = []
    
    for skill in required_skills:
        if skill.lower().strip() in user_skills_set:
            mastered.append(skill)
        else:
            missing.append(skill)
            
    score = (len(mastered) / len(required_skills)) * 100
    
    # Suggestion logic
    if missing:
        suggestion = f"Pour devenir un expert {target_job}, tu devrais ajouter un projet utilisant {', '.join(missing[:2])} à ton portfolio."
    else:
        suggestion = f"Félicitations ! Ton bagage technique correspond parfaitement aux attentes pour un poste de {target_job}."

    return {
        "job": target_job,
        "score": round(score, 2),
        "mastered": mastered,
        "missing": missing,
        "suggestion": suggestion,
        "radar_data": {
            "labels": required_skills,
            "values": [100 if s.lower() in user_skills_set else 20 for s in required_skills]
        }
    }

if __name__ == "__main__":
    # Reading from stdin instead of argv to avoid Windows shell quoting issues
    try:
        input_data = sys.stdin.read()
        if not input_data:
            print(json.dumps({"error": "Aucune donnée reçue sur stdin."}))
            sys.exit(1)
            
        data = json.loads(input_data)
        user_skills = data.get("skills", [])
        target_job = data.get("job", "")
        
        print(json.dumps(analyze_gap(user_skills, target_job)))
    except Exception as e:
        print(json.dumps({"error": f"Erreur de parsing JSON: {str(e)}"}))
