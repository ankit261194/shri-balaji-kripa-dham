import json

with open(r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        step = json.loads(line)
        if step.get('type') == 'USER_INPUT':
            content = step.get('content', '')
            for w in ['हटा', 'hta', 'aarti', 'आरती', 'admin', 'superadmin', 'panel']:
                if w in content.lower():
                    safe = content.encode('ascii', errors='replace').decode('ascii')
                    print(f"Step {step.get('step_index')}: {safe}")
                    break
