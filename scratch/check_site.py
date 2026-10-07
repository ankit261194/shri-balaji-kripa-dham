import urllib.request
import re
import sys
import json

sys.stdout.reconfigure(encoding="utf-8")

# 1. Check home page HTML
req = urllib.request.Request("https://shribalajikripadham.online/", headers={"User-Agent": "Mozilla/5.0"})
with urllib.request.urlopen(req) as resp:
    html = resp.read().decode("utf-8", errors="ignore")

tels = re.findall(r"tel:([0-9+]+)", html)
was = re.findall(r"wa\.me/([0-9+]+)", html)
print("HOMEPAGE tel links:", set(tels))
print("HOMEPAGE wa.me links:", set(was))

# 2. Check live_config.php
req2 = urllib.request.Request("https://shribalajikripadham.online/api/live_config.php")
with urllib.request.urlopen(req2) as resp:
    cfg = json.loads(resp.read().decode("utf-8"))
c = cfg.get("config", {})
print("live_config.php contact_phone:", c.get("contact_phone"))
print("live_config.php whatsapp_number:", c.get("whatsapp_number"))
print("live_config.php ashram_address:", c.get("ashram_address"))
print("live_config.php current_serving_token:", c.get("current_serving_token"))
print("live_config.php daily_token_limit:", c.get("daily_token_limit"))
print("live_config.php darbar_date:", c.get("darbar_date"))
print("live_config.php is_darbar_active:", c.get("is_darbar_active"))

# 3. Check what tokens exist
req3 = urllib.request.Request("https://shribalajikripadham.online/api/get_queue.php?date=" + str(c.get("darbar_date", "2026-10-06")))
with urllib.request.urlopen(req3) as resp:
    q = json.loads(resp.read().decode("utf-8"))
print("Tokens in queue for darbar_date:", len(q.get("tokens", [])))
print("Serving token in queue:", q.get("serving_token"))
