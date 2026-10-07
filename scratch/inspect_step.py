import json

with open(r'C:\Users\hp\.gemini\antigravity\brain\4a9563ac-0aeb-420d-87e1-1333dc38f287\.system_generated\logs\transcript.jsonl', 'r', encoding='utf-8') as f:
    for line in f:
        data = json.loads(line)
        idx = data.get('step_index', 0)
        if idx in [70359, 70375]:
            tc = data.get('tool_calls', [])
            for call in tc:
                print(f"Step {idx}: {call.get('args', {}).get('CommandLine')}")
