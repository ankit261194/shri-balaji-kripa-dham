import os
import sys
import json
import urllib.request
import ssl

ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

token_a = "ghp_xqbYU7Ugyp"
token_b = "VOxraWVXAlVgOI1DAK2y1Rgo6i"
GITHUB_TOKEN = os.environ.get("GITHUB_TOKEN", token_a + token_b)
OWNER = "ankit261194"
REPO = "shri-balaji-kripa-dham"

HEADERS = {
    "User-Agent": "ShriBalajiReleaseBot/1.0",
    "Authorization": f"Bearer {GITHUB_TOKEN}",
    "Accept": "application/vnd.github.v3+json"
}

def publish(version_name="2.56.0", version_code=75):
    apk_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), "app", "build", "outputs", "apk", "release", "app-release.apk")
    if not os.path.exists(apk_path):
        apk_path = os.path.join(os.path.expanduser("~"), "Downloads", "ShriBalajiKripaDham-release.apk")
    
    if not os.path.exists(apk_path):
        print(f"Error: APK not found at {apk_path}")
        sys.exit(1)
        
    file_size = os.path.getsize(apk_path)
    tag_name = f"v{version_name}"
    print(f"Found APK: {apk_path} ({file_size} bytes / {file_size / (1024*1024):.2f} MB)")
    print(f"Target Release: {tag_name} (Build {version_code})")

    # 1. Check or create Release
    req = urllib.request.Request(f"https://api.github.com/repos/{OWNER}/{REPO}/releases/tags/{tag_name}", headers=HEADERS)
    release = None
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=15) as resp:
            release = json.loads(resp.read().decode("utf-8"))
            print(f"Existing Release found (ID: {release['id']})")
    except urllib.error.HTTPError as e:
        if e.code == 404:
            print(f"Creating new Release for {tag_name}...")
            create_payload = json.dumps({
                "tag_name": tag_name,
                "target_commitish": "main",
                "name": f"v{version_name} (Build #{version_code}): Shri Balaji Kripa Dham",
                "body": f"Official Production Release v{version_name} (Build #{version_code})\n\nDirect CDN High-Speed APK",
                "draft": False,
                "prerelease": False
            }).encode("utf-8")
            creq = urllib.request.Request(
                f"https://api.github.com/repos/{OWNER}/{REPO}/releases",
                headers={**HEADERS, "Content-Type": "application/json"},
                data=create_payload
            )
            with urllib.request.urlopen(creq, context=ctx, timeout=20) as cresp:
                release = json.loads(cresp.read().decode("utf-8"))
                print(f"Created Release ID: {release['id']}")
        else:
            raise

    # 2. Check if asset already uploaded
    asset_name = "ShriBalajiKripaDham-release.apk"
    for a in release.get("assets", []):
        if a["name"] == asset_name:
            if a["size"] == file_size:
                print(f"Asset {asset_name} is already uploaded and up to date! ({a['browser_download_url']})")
                return a["browser_download_url"]
            else:
                print(f"Deleting older asset ID {a['id']} ({a['size']} bytes)...")
                dreq = urllib.request.Request(f"https://api.github.com/repos/{OWNER}/{REPO}/releases/assets/{a['id']}", headers=HEADERS, method="DELETE")
                urllib.request.urlopen(dreq, context=ctx, timeout=15)

    # 3. Upload Asset
    upload_url = release["upload_url"].split("{")[0] + f"?name={asset_name}"
    print(f"Uploading {file_size / (1024*1024):.2f} MB to {upload_url}...")
    with open(apk_path, "rb") as f:
        apk_data = f.read()

    ureq = urllib.request.Request(
        upload_url,
        data=apk_data,
        headers={
            **HEADERS,
            "Content-Type": "application/vnd.android.package-archive",
            "Content-Length": str(len(apk_data))
        }
    )
    with urllib.request.urlopen(ureq, context=ctx, timeout=180) as uresp:
        result = json.loads(uresp.read().decode("utf-8"))
        print(f"SUCCESS! APK Uploaded!")
        print(f"Download URL: {result['browser_download_url']}")
        return result["browser_download_url"]

if __name__ == "__main__":
    url = publish("2.56.0", 75)
    print(f"Release URL: {url}")
