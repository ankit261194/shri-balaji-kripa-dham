import asyncio
import edge_tts
import os
import json
import urllib.request

tracks = [
    {
        "key": "khatu_shyam_aarti",
        "filename": "khatu_shyam_aarti_authentic.mp3",
        "voice": "hi-IN-MadhurNeural",
        "rate": "-5%",
        "text": """
॥ श्री खाटू श्याम जी की आरती ॥

ॐ जय श्री श्यामा हरे, बाबा जय श्री श्यामा हरे।
खाटू धाम विराजत, अनुपम रूप धरे॥ ॐ जय श्री श्यामा हरे॥

रतन जड़ित सिंहासन, अद्भुत छवि सोहे।
मोदक भोग लगत हैं, सुर मुनि मन मोहे॥ ॐ जय श्री श्यामा हरे॥

गले वैजयंती माला, शीश मुकुट साजे।
कानन कुंडल झलकत, नूपुर पग बाजे॥ ॐ जय श्री श्यामा हरे॥

तीन बाण के धारी, शीश के दानी।
कलयुग में अवतारी, महिमा जग जानी॥ ॐ जय श्री श्यामा हरे॥

हारे के सहारे, लखदातार कहाए।
जो जन शरण तिहारी, संकट मिट जाए॥ ॐ जय श्री श्यामा हरे॥

आरती जो कोई गावे, प्रेम सहित ध्यावे।
कहत श्याम जन सेवक, मनवांछित फल पावे॥
ॐ जय श्री श्यामा हरे, बाबा जय श्री श्यामा हरे।
खाटू धाम विराजत, अनुपम रूप धरे॥
"""
    },
    {
        "key": "satyanarayan_aarti",
        "filename": "satyanarayan_aarti_authentic.mp3",
        "voice": "hi-IN-MadhurNeural",
        "rate": "-5%",
        "text": """
॥ श्री सत्यनारायण जी की आरती ॥

जय लक्ष्मी रमणा, स्वामी जय लक्ष्मी रमणा।
सत्यनारायण स्वामी, जन पातक हरणा॥ ॐ जय लक्ष्मी रमणा॥

रत्न जड़ित सिंहासन, अद्भुत छबि राजै।
नारद करत निराजन, घण्टा धुनि बाजै॥ ॐ जय लक्ष्मी रमणा॥

प्रकट भये कलि कारण, द्विज को दरश दियो।
बूढ़ो ब्राह्मण बनके, कंचन महल कियो॥ ॐ जय लक्ष्मी रमणा॥

दुर्बल भील कठौता, जिन पर कृपा करी।
लीन्यो भक्ति भाव बस, सम्पति अमित भरी॥ ॐ जय लक्ष्मी रमणा॥

सत्यनारायण जी की आरती, जो कोई नर गावै।
कहत शिवानन्द स्वामी, मनवांछित फल पावै॥
जय लक्ष्मी रमणा, स्वामी जय लक्ष्मी रमणा।
सत्यनारायण स्वामी, जन पातक हरणा॥
"""
    },
    {
        "key": "bhajan_meri_naav_chali",
        "filename": "bhajan_meri_naav_chali_authentic.mp3",
        "voice": "hi-IN-MadhurNeural",
        "rate": "-4%",
        "text": """
॥ पावन संकटमोचन भजन: मेरी नाव चली बजरंग बली ॥

मेरी नाव चली बजरंग बली,
जरा बल्ली कृपा की लगा देना।
भवसागर बीच भंवर में फंसी,
पार किनारे लगा देना॥

तूफां है बड़ा, मझधार बड़ी,
कोई और नहीं है सहारा यहाँ।
इक तेरा भरोसा है दाता,
मेरी बिगड़ी को आके बना देना॥
मेरी नाव चली बजरंग बली...

तूने राम जी के सब काज किए,
सीता माता की सुध लाए कपी।
लक्ष्मण जी के प्राण बचाए प्रभु,
मेरा भी कष्ट मिटा देना॥
मेरी नाव चली बजरंग बली...

अहिरावण को पाताल में मारा,
लंका में मचाया था हाहाकार।
मैं दीन हीन दुर्बल बालक,
मुझ दास पर दया बरसा देना॥
मेरी नाव चली बजरंग बली...

डूँगरा धाम में तेरा पहरा,
तेरी महिमा गावे सब नर नारी।
हाथ जोड़ कर विनती करूँ,
मेरी नैया को पार लगा देना॥
मेरी नाव चली बजरंग बली,
जरा बल्ली कृपा की लगा देना॥
"""
    },
    {
        "key": "bhajan_aaj_mangalwar_hai",
        "filename": "bhajan_aaj_mangalwar_hai_authentic.mp3",
        "voice": "hi-IN-MadhurNeural",
        "rate": "-4%",
        "text": """
॥ पावन भजन: आज मंगलवार है महावीर का वार है ॥

आज मंगलवार है, महावीर का वार है।
सच्चे मन से जो भी बोले, उसका बेड़ा पार है॥

लाल लंगोटा हाथ में सोटा, केसरीनंदन प्यारा है।
सूरज को फल समझ के खाया, ऐसा पवन दुलारा है॥
जिनके सीने में सिया राम, वो अंजनी का लाल है।
आज मंगलवार है, महावीर का वार है...

संकट कटे मिटे सब पीरा, जो सुमरे हनुमत बलबीरा।
भूत पिशाच निकट नहिं आवै, जब नाम सुनावे जगधीरा॥
जिस पर कृपा करे बजरंगी, मिट जाता भव का भार है।
आज मंगलवार है, महावीर का वार है...

डूँगरा जाट में दरबार सजा, बालाजी की जय जयकार हुई।
जो भी आया खाली झोली, उसकी झोली भरपूर हुई॥
तेजवीर गुरुदेव की वाणी, सच्चा यह दरबार है।
आज मंगलवार है, महावीर का वार है।
सच्चे मन से जो भी बोले, उसका बेड़ा पार है॥
"""
    }
]

def upload_to_hostinger(file_path, filename):
    boundary = f'----WebKitFormBoundary{os.urandom(8).hex()}'
    with open(file_path, 'rb') as f:
        file_bytes = f.read()

    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(f'Content-Disposition: form-data; name="audio"; filename="{filename}"\r\n'.encode('utf-8'))
    body.extend(b'Content-Type: audio/mpeg\r\n\r\n')
    body.extend(file_bytes)
    body.extend(f'\r\n--{boundary}--\r\n'.encode('utf-8'))

    req_up = urllib.request.Request(
        'https://shribalajikripadham.online/api/upload_audio.php',
        data=bytes(body),
        headers={
            'Content-Type': f'multipart/form-data; boundary={boundary}',
            'X-SBKD-API-KEY': 'SBKD_SECURE_TOKEN_9100100251233433_V243'
        },
        method='POST'
    )

    with urllib.request.urlopen(req_up, timeout=60) as resp:
        res = json.loads(resp.read().decode('utf-8'))
        return res.get('audio_url')

async def main():
    results = {}
    for t in tracks:
        key = t['key']
        out_file = f"scratch/temp_{key}.mp3"
        print(f"Generating {key}...")
        comm = edge_tts.Communicate(t['text'], t['voice'], rate=t['rate'])
        await comm.save(out_file)
        size = os.path.getsize(out_file)
        print(f"  Generated {size} bytes. Uploading...")
        url = upload_to_hostinger(out_file, t['filename'])
        print(f"  UPLOADED {key} -> {url}")
        results[key] = url

    with open("scratch/uploaded_remaining_4.json", "w", encoding="utf-8") as out:
        json.dump(results, out, indent=2)

    print("\nAll 4 generated and uploaded successfully!")

asyncio.run(main())
