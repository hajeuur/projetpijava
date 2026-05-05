import sys
import json
from sentence_transformers import SentenceTransformer, util

# --- Modèle Léger ---
# Nous utilisons all-MiniLM-L6-v2 car il est très rapide et léger (80 Mo)
MODEL_NAME = 'all-MiniLM-L6-v2'

# --- Base de Connaissance Métiers ---
JOB_PROFILES = {
    "Java Backend Developer": {
        "skills": "Java, Spring Boot, Hibernate, MySQL, PostgreSQL, Microservices, Docker, Maven, Git, REST API",
        "salary_range": "45k - 75k €",
        "demand": "High"
    },
    "Fullstack JavaScript Developer": {
        "skills": "JavaScript, React, Node.js, Express, MongoDB, TypeScript, HTML, CSS, Next.js, Redux",
        "salary_range": "42k - 70k €",
        "demand": "Very High"
    },
    "Data Scientist / AI Engineer": {
        "skills": "Python, Machine Learning, Deep Learning, TensorFlow, PyTorch, Scikit-learn, Pandas, NumPy, Data Analysis, SQL",
        "salary_range": "50k - 90k €",
        "demand": "High"
    },
    "DevOps Engineer": {
        "skills": "Docker, Kubernetes, AWS, Azure, Jenkins, CI/CD, Terraform, Linux, Ansible, Monitoring",
        "salary_range": "55k - 85k €",
        "demand": "High"
    },
    "Mobile Developer (Android/iOS)": {
        "skills": "Flutter, React Native, Swift, Kotlin, Dart, Mobile Design, Firebase, API Integration",
        "salary_range": "40k - 65k €",
        "demand": "Medium"
    }
}

def predict_career(user_skills_text):
    try:
        model = SentenceTransformer(MODEL_NAME)
        
        # Encodage des profils métiers
        job_titles = list(JOB_PROFILES.keys())
        job_descriptions = [JOB_PROFILES[j]["skills"] for j in job_titles]
        
        # Encodage de l'utilisateur
        user_embedding = model.encode(user_skills_text, convert_to_tensor=True)
        job_embeddings = model.encode(job_descriptions, convert_to_tensor=True)
        
        # Calcul de la similarité cosinus
        cosine_scores = util.cos_sim(user_embedding, job_embeddings)[0]
        
        results = []
        for i in range(len(job_titles)):
            score = float(cosine_scores[i])
            # Normalisation du score pour l'affichage (0-100)
            percentage = round(max(10, score * 100), 1) 
            
            job_name = job_titles[i]
            results.append({
                "job": job_name,
                "score": percentage,
                "salary": JOB_PROFILES[job_name]["salary_range"],
                "demand": JOB_PROFILES[job_name]["demand"]
            })
            
        # Trier par score décroissant
        results.sort(key=lambda x: x["score"], reverse=True)
        
        return {"success": True, "predictions": results}

    except Exception as e:
        return {"success": False, "error": str(e)}

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"success": False, "error": "No skills provided"}))
        sys.exit(1)
        
    user_skills = sys.argv[1]
    prediction = predict_career(user_skills)
    print(json.dumps(prediction))
