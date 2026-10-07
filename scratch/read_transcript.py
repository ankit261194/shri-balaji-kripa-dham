import json

transcript_path = r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl'
user_steps = []
with open(transcript_path, 'r', encoding='utf-8') as f:
    for line in f:
        step = json.loads(line)
        if step.get('type') == 'USER_INPUT':
            user_steps.append((step.get('step_index'), step.get('content', '')))

print(f"Total user steps: {len(user_steps)}")
# Print the last 30 user steps:
for idx, txt in user_steps[-30:]:
    safe_txt = txt.encode('ascii', errors='replace').decode('ascii')
    print(f"Step {idx}: {safe_txt[:150]}")
