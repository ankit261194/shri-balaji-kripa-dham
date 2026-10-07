import json

transcript_path = r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl'
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        step = json.loads(line)
        if step.get('type') == 'USER_INPUT':
            txt = step.get('content', '')
            if any(w in txt.lower() for w in ['photo', 'pic', 'tasveer', 'darshan', 'image']):
                print(f"Step {step.get('step_index')}:")
                for l in txt.splitlines():
                    print("  ", l.encode('ascii', errors='replace').decode('ascii'))
