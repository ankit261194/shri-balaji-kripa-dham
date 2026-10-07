import urllib.request
import json
import sys

sys.stdout.reconfigure(encoding='utf-8')

url = "https://shribalajikripadham.online/api/send_fcm.php"
payload = {
    "phone_number": "9100100251",
    "token_number": 108,
    "patient_name": "परीक्षण भक्त जी",
    "title": "🔔 टोकन बुलावा: टोकन #108",
    "body": "श्री परीक्षण भक्त जी, टोकन #108 का नंबर आ गया है। कृपया गुरुजी के समीप पधारें।"
}

req = urllib.request.Request(
    url,
    data=json.dumps(payload).encode('utf-8'),
    headers={"Content-Type": "application/json"}
)

with urllib.request.urlopen(req) as resp:
    data = json.loads(resp.read().decode('utf-8'))
    print("Multi-Channel Push Result:", json.dumps(data, indent=2, ensure_ascii=False))
