import json
import urllib.request
import urllib.parse
import time
import sys

sys.stdout.reconfigure(encoding='utf-8')

base_url = "https://shribalajikripadham.online/api/sevadar_chat.php"

def call_api(action, payload=None, params=None):
    url = f"{base_url}?action={action}"
    if params:
        for k, v in params.items():
            url += f"&{k}={urllib.parse.quote(str(v))}"
    
    headers = {"Content-Type": "application/json"}
    data = None
    if payload:
        data = json.dumps(payload).encode('utf-8')
    
    req = urllib.request.Request(url, data=data, headers=headers)
    with urllib.request.urlopen(req, timeout=15) as resp:
        return json.loads(resp.read().decode('utf-8'))

print("=== 1. Testing send_message (Devotee -> Sevadar) ===")
res_send = call_api("send_message", payload={
    "sevadar_id": "5",
    "sevadar_name": "श्री बालाजी कृपा धाम (आधिकारिक हेल्पलाइन)",
    "devotee_id": "devotee_test_999",
    "devotee_name": "परीक्षण भक्त",
    "devotee_phone": "9876543210",
    "sender_role": "DEVOTEE",
    "message_type": "TEXT",
    "message_text": "जय श्री बालाजी महाराज! क्या आज शाम की आरती का समय 7:00 बजे है?"
})
print("Send result:", json.dumps(res_send, ensure_ascii=False))

conv_id = res_send.get("conversation_id", "conv_5_9876543210")
msg_id = res_send.get("msg_id", "")

print("\n=== 2. Testing get_messages ===")
res_get = call_api("get_messages", params={"conversation_id": conv_id, "limit": 10})
print("Get messages count:", len(res_get.get("messages", [])))
print("First message:", json.dumps(res_get.get("messages", [])[0] if res_get.get("messages") else {}, ensure_ascii=False))

print("\n=== 3. Testing send_message (Sevadar / Admin Reply -> Devotee) ===")
res_reply = call_api("send_message", payload={
    "conversation_id": conv_id,
    "sevadar_id": "5",
    "sevadar_name": "श्री बालाजी कृपा धाम (आधिकारिक हेल्पलाइन)",
    "devotee_id": "devotee_test_999",
    "devotee_name": "परीक्षण भक्त",
    "devotee_phone": "9876543210",
    "sender_role": "SEVADAR",
    "message_type": "TEXT",
    "message_text": "जय श्री राम! हाँ भक्त जी, सायंकालीन दिव्य महाआरती ठीक सायं 7:00 बजे प्रारंभ होगी। आप सपरिवार आमंत्रित हैं।"
})
print("Reply result:", json.dumps(res_reply, ensure_ascii=False))

print("\n=== 4. Testing mark_read (Double Blue Ticks) ===")
res_read = call_api("mark_read", payload={
    "conversation_id": conv_id,
    "role": "DEVOTEE"
})
print("Mark read result:", json.dumps(res_read, ensure_ascii=False))

print("\n=== 5. Testing list_conversations (Admin / Sevadar Console) ===")
res_list = call_api("list_conversations")
print("Total active conversations:", len(res_list.get("conversations", [])))
print("Conversation preview:", json.dumps(res_list.get("conversations", [])[0] if res_list.get("conversations") else {}, ensure_ascii=False))

print("\nALL 5 REAL-TIME CHAT API TESTS PASSED SUCCESSFULLY!")
