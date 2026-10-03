import os
import time
import json
import urllib.request
import urllib.error

API_KEYS = [
    "sk_13bf5df7b1cf804ee9b3b03590b2f64d90463109bddffbfe",
    "sk_61cb841ba07dec2d666c7b8d9cc516655565ba6bdb7b806d",
    "sk_a5c124d02a3c357ae95bfac2afb7947a2c8448504aff9dc6",
    "sk_fd84cbff22e602f6a663c34b2e2427fddb9cc2d14f349031",
    "sk_6468884e17cc4dbeba98e4ec00a94297d7e70b524d645017"
]

MODEL_ID = "eleven_multilingual_v2"

VOICE_MALE_ID = "nPczCjzI2devNBz1zQrb"   # Brian
VOICE_FEMALE_ID = "EXAVITQu4vr4xnSDxMaL" # Sarah

OUT_DIR = "app/src/main/assets/audio/discipline"
os.makedirs(OUT_DIR, exist_ok=True)

PROMPTS = {
    "warn_talk_outside": "कृपया ध्यान दें: दरबार में पूर्ण शांति बनाए रखें। जिनको भी आवश्यक बातचीत करनी है, वे कृपया दरबार परिसर से बाहर जाकर बात करें।",
    "warn_sit_back": "कृपया ध्यान दें: सभी भक्तगण मर्यादा का पालन करते हुए पीछे व्यवस्थित होकर बैठें ताकि सभी को सुगमता से दर्शन प्राप्त हो सकें।"
}

def synthesize_clip(text, voice_id, output_path):
    url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
    payload = {
        "text": text,
        "model_id": MODEL_ID,
        "voice_settings": {
            "stability": 0.72,
            "similarity_boost": 0.85
        }
    }

    for key in API_KEYS:
        try:
            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode("utf-8"),
                headers={
                    "xi-api-key": key,
                    "Content-Type": "application/json"
                }
            )
            with urllib.request.urlopen(req, timeout=30) as res:
                audio = res.read()
                with open(output_path, "wb") as f:
                    f.write(audio)
                print(f"SUCCESS: Generated {output_path} ({len(audio)} bytes) with key ...{key[-6:]}")
                return True
        except urllib.error.HTTPError as e:
            err = e.read().decode('utf-8', errors='ignore')
            print(f"Key ...{key[-6:]} returned HTTP {e.code}: {err[:80]}")
            continue
        except Exception as e:
            print(f"Key ...{key[-6:]} error: {e}")
            continue
    return False

def main():
    print("=== Generating Polite, Dignified Ashram Discipline Audio ===")
    
    # 1. Talk Outside Male
    synthesize_clip(PROMPTS["warn_talk_outside"], VOICE_MALE_ID, os.path.join(OUT_DIR, "warn_talk_outside_male.mp3"))
    time.sleep(1)
    
    # 2. Talk Outside Female
    synthesize_clip(PROMPTS["warn_talk_outside"], VOICE_FEMALE_ID, os.path.join(OUT_DIR, "warn_talk_outside_female.mp3"))
    time.sleep(1)
    
    # 3. Sit Back Male
    synthesize_clip(PROMPTS["warn_sit_back"], VOICE_MALE_ID, os.path.join(OUT_DIR, "warn_sit_back_male.mp3"))
    time.sleep(1)
    
    # 4. Sit Back Female
    synthesize_clip(PROMPTS["warn_sit_back"], VOICE_FEMALE_ID, os.path.join(OUT_DIR, "warn_sit_back_female.mp3"))
    
    print("=== All Discipline Audio clips refreshed successfully! ===")

if __name__ == "__main__":
    main()
