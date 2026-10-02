import os
import time
import json
import urllib.request
import urllib.error

API_KEY = "sk_13bf5df7b1cf804ee9b3b03590b2f64d90463109bddffbfe"
MODEL_ID = "eleven_multilingual_v2"

VOICES = {
    "male": {
        "id": "nPczCjzI2devNBz1zQrb", # Brian
        "dir": "app/src/main/assets/audio/male"
    },
    "female": {
        "id": "EXAVITQu4vr4xnSDxMaL", # Sarah
        "dir": "app/src/main/assets/audio/female"
    }
}

WORDS_1_TO_100 = [
    "", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस",
    "ग्यारह", "बारह", "तेरह", "चौदह", "पंद्रह", "सोलह", "सत्रह", "अठारह", "उन्नीस", "बीस",
    "इक्कीस", "बाईस", "तेईस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताईस", "अट्ठाईस", "उनतीस", "तीस",
    "इकतीस", "बत्तीस", "तैंतीस", "चौंतीस", "पैंतीस", "छत्तीस", "सैंतीस", "अड़तीस", "उनतालीस", "चालीस",
    "इकतालीस", "बयालीस", "तैंतालीस", "चवालीस", "पैंतालीस", "छियालीस", "सैंतालीस", "अड़तालीस", "उनचास", "पचास",
    "इक्यावन", "बावन", "तिरेपन", "चौवन", "पचपन", "छप्पन", "सत्तावन", "अट्ठावन", "उनसठ", "साठ",
    "इकसठ", "बासठ", "तिरेसठ", "चौंसठ", "पैंसठ", "छियासठ", "सरसठ", "अड़सठ", "उनहत्तर", "सत्तर",
    "इकहत्तर", "बहत्तर", "तिहत्तर", "चौहत्तर", "पचहत्तर", "छिहत्तर", "सतहत्तर", "अठहत्तर", "उन्नासी", "अस्सी",
    "इक्यासी", "बयासी", "तिरासी", "चौरासी", "पचासी", "छियासी", "सत्तासी", "अट्ठासी", "नवासी", "नब्बे",
    "इक्यानवे", "बानवे", "तिरानवे", "चौरानवे", "पंचानवे", "छियानवे", "सत्तानवे", "अट्ठानवे", "निन्यानवे", "एक सौ"
]

def get_hindi_number(n):
    if 1 <= n <= 100:
        return WORDS_1_TO_100[n]
    elif 101 <= n <= 150:
        rem = n % 100
        return f"एक सौ {WORDS_1_TO_100[rem]}"
    return str(n)

PROMPTS = {
    "token_intro": "टोकन नंबर",
    "shri": "श्री",
    "shrimati": "श्रीमती",
    "call_guruji": "जी, आपका नंबर आ गया है, तुरंत गुरुजी के समीप आएं।",
    "call_guruji_direct": "आपका नंबर आ गया है, तुरंत गुरुजी के समीप आएं।",
    "standby_prompt": "जी, अगला नंबर आपका है, कृपया आगे आकर बैठें, और बाकी सब पीछे होके बैठ जाओ।",
    "standby_behind_prompt": "जी, अगला नंबर आपका है, कृपया इनके पीछे आकर बैठें, और बाकी सब पीछे होके बैठ जाओ।",
    "standby_direct": "अगला नंबर आपका है, कृपया आगे आकर बैठें, और बाकी सब पीछे होके बैठ जाओ।"
}

def synthesize_clip(text, voice_id, output_path):
    if os.path.exists(output_path) and os.path.getsize(output_path) > 1000:
        print(f"Skipping (already exists): {output_path}")
        return True

    url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
    payload = {
        "text": text,
        "model_id": MODEL_ID,
        "voice_settings": {
            "stability": 0.65,
            "similarity_boost": 0.8
        }
    }

    retries = 3
    while retries > 0:
        try:
            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode("utf-8"),
                headers={
                    "xi-api-key": API_KEY,
                    "Content-Type": "application/json"
                }
            )
            with urllib.request.urlopen(req) as res:
                audio = res.read()
                with open(output_path, "wb") as f:
                    f.write(audio)
                print(f"Generated: {output_path} ({len(audio)} bytes)")
                return True
        except urllib.error.HTTPError as e:
            if e.code == 429:
                print("Rate limit 429 hit. Sleeping 5 seconds...")
                time.sleep(5)
                retries -= 1
            else:
                err_body = e.read().decode('utf-8', errors='ignore')
                print(f"HTTP Error {e.code} for '{text}': {err_body}")
                return False
        except Exception as e:
            print(f"Error for '{text}': {e}")
            retries -= 1
            time.sleep(2)
    return False

def main():
    print("=== Starting ElevenLabs Audio Assets Generation (Tokens 1 to 150) ===")
    
    for gender, vinfo in VOICES.items():
        v_id = vinfo["id"]
        out_dir = vinfo["dir"]
        os.makedirs(out_dir, exist_ok=True)
        print(f"\n--- Generating for {gender.upper()} voice ({v_id}) in {out_dir} ---")

        # 1. Dialogues
        for name, text in PROMPTS.items():
            path = os.path.join(out_dir, f"{name}.mp3")
            synthesize_clip(text, v_id, path)
            time.sleep(0.4)

        # 2. Token Numbers 1 to 150
        for num in range(1, 151):
            h_word = get_hindi_number(num)
            path = os.path.join(out_dir, f"num_{num}.mp3")
            synthesize_clip(h_word, v_id, path)
            time.sleep(0.4)

    print("\n=== ALL AUDIO CLIPS GENERATED SUCCESSFULLY! ===")

if __name__ == "__main__":
    main()
