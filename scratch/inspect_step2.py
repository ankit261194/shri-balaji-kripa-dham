import json
with open(r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript_full.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        obj = json.loads(line)
        if obj.get('step_index') in [69532, 69533]:
            print(f"Step {obj.get('step_index')}: {repr(obj.get('content'))}")
