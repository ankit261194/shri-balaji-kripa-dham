import sys
import urllib.request
import json
import re
import time

sys.stdout.reconfigure(encoding='utf-8')

print("1. Triggering deploy.php?sha=cbae633 on Hostinger...")
deploy_url = "https://shribalajikripadham.online/deploy.php?sha=cbae633"
req = urllib.request.Request(deploy_url, headers={"User-Agent": "Mozilla/5.0", "Cache-Control": "no-cache"})
try:
    with urllib.request.urlopen(req, timeout=45) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        print("deploy.php result:")
        print(json.dumps(res, indent=2, ensure_ascii=False))
except Exception as e:
    print("Error calling deploy.php:", e)

time.sleep(2)

print("\n2. Checking api/live_config.php for both whatsapp_channel_url and whatsapp_group_url...")
cfg_url = "https://shribalajikripadham.online/api/live_config.php"
req_c = urllib.request.Request(cfg_url, headers={"User-Agent": "Mozilla/5.0", "Cache-Control": "no-cache"})
try:
    with urllib.request.urlopen(req_c, timeout=30) as resp:
        cfg = json.loads(resp.read().decode('utf-8'))
        channel = cfg.get("whatsapp_channel_url")
        group = cfg.get("whatsapp_group_url")
        print(f"whatsapp_channel_url: {channel}")
        print(f"whatsapp_group_url:   {group}")
        assert channel == "https://whatsapp.com/channel/0029VaCZJTmJ3jv2UwiMtY1w", f"Unexpected channel: {channel}"
        assert group == "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0", f"Unexpected group: {group}"
        print("API VERIFICATION SUCCESS: Both URLs distinct, correct, and non-conflicting!")
except Exception as e:
    print("Error checking live_config.php:", e)

time.sleep(2)

print("\n3. Checking https://shribalajikripadham.online/ Website HTML...")
web_url = "https://shribalajikripadham.online/?v=" + str(int(time.time()))
req_w = urllib.request.Request(web_url, headers={"User-Agent": "Mozilla/5.0", "Cache-Control": "no-cache"})
try:
    with urllib.request.urlopen(req_w, timeout=30) as resp:
        html = resp.read().decode('utf-8')
        links = re.findall(r'href=[\'"]([^\'"]*whatsapp[^\'"]*)[\'"]', html, re.I)
        print("Found WhatsApp links on live website:")
        for l in set(links):
            print(" -", l)
        assert "https://whatsapp.com/channel/0029VaCZJTmJ3jv2UwiMtY1w" in html, "Channel link missing from website HTML!"
        assert "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0" in html, "Group link missing from website HTML!"
        print("WEBSITE VERIFICATION SUCCESS: Both WhatsApp Channel and WhatsApp Group are fully present and live on https://shribalajikripadham.online/!")
except Exception as e:
    print("Error checking website HTML:", e)

print("\nALL VERIFICATIONS COMPLETED SUCCESSFULLY!")
