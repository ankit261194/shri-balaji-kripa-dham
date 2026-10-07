import json

with open(r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        step = json.loads(line)
        idx = step.get('step_index', 0)
        if idx > 65000:
            content = step.get('content', '')
            if 'deploy' in content.lower() or 'upload' in content.lower() or 'hostinger' in content.lower():
                safe = content[:150].encode('ascii', errors='replace').decode('ascii')
                print(f"Step {idx}: {safe}")
