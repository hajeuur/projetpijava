import sys
import requests
import json

def analyze(api_key, file_path):
    headers = {
        "Authorization": f"Bearer {api_key}",
        "Accept": "application/json"
    }
    ORG_ID = "znQEuaKg"
    log = []
    
    try:
        # 1. Lister les Workspaces
        url_ws = f"https://api.affinda.com/v3/workspaces?organization={ORG_ID}"
        resp_ws = requests.get(url_ws, headers=headers)
        log.append(f"WS Status: {resp_ws.status_code}")
        
        if resp_ws.status_code != 200:
            return {"error": f"Erreur WS ({resp_ws.status_code}): {resp_ws.text}"}
            
        workspaces = resp_ws.json()
        if not isinstance(workspaces, list): workspaces = workspaces.get("results", [])
        
        log.append(f"{len(workspaces)} Workspaces trouvés.")
        collection_id = None
        
        for ws in workspaces:
            ws_id = ws.get("identifier")
            ws_name = ws.get("name")
            log.append(f"Workspace: {ws_name} ({ws_id})")
            
            url_col = f"https://api.affinda.com/v3/collections?workspace={ws_id}"
            resp_col = requests.get(url_col, headers=headers)
            
            if resp_col.status_code == 200:
                cols = resp_col.json()
                if not isinstance(cols, list): cols = cols.get("results", [])
                log.append(f"  {len(cols)} collections.")
                
                for c in cols:
                    c_id = c.get("identifier")
                    c_name = c.get("name")
                    log.append(f"  - {c_name} ({c_id})")
                    if not collection_id: collection_id = c_id
                    if "resume" in c_name.lower() or "cv" in c_name.lower():
                        collection_id = c_id
                        break
            else:
                log.append(f"  Erreur Collection: {resp_col.status_code}")
            
            if collection_id: break

        if not collection_id:
            return {"error": "Aucun dossier trouvé.\nLog : " + " | ".join(log)}

        # 2. Upload
        with open(file_path, 'rb') as f:
            files = {'file': f}
            data_up = {'collection': collection_id}
            response = requests.post("https://api.affinda.com/v3/documents", 
                                   headers=headers, 
                                   files=files, 
                                   data=data_up)
            
            if 200 <= response.status_code < 300:
                return response.json()
            else:
                return {"error": f"Upload failed ({response.status_code}): {response.text}"}
                
    except Exception as e:
        return {"error": str(e) + " | Log: " + " ".join(log)}

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(json.dumps({"error": "Usage: script.py <api_key> <file_path>"}))
    else:
        result = analyze(sys.argv[1], sys.argv[2])
        print(json.dumps(result))
