import json
transcript_path = r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl'
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        step = json.loads(line)
        if step.get('type') == 'USER_INPUT':
            idx = step.get('step_index')
            if idx in [65565, 65648, 66927, 67115, 67125, 67585]:
                print(f"=== Step {idx} ===")
                print(step.get('content', '').encode('ascii', errors='replace').decode('ascii'))
