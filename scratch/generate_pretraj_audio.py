import asyncio
import edge_tts
import os
import json
import urllib.request

text = """
॥ श्री प्रेतराज सरकार पावन वंदना व आरती ॥

जय प्रेतराज कृपालु मेरी अरज अब सुन लीजिये।
मैं शरण तुम्हारी आ गया, मेरी विपदा को हर लीजिये॥

शीश पर मुकुट विराजे, गल मोतियन की माल।
हाथ में सोटा सोहता, भाल तिलक लाल॥
दुष्ट दलन पावन प्रभू, संकट हरन दयाल।
भूत पिशाच थर-थर कांपें, देख रूप विकराल॥
जय प्रेतराज कृपालु मेरी अरज अब सुन लीजिये...

असुर निकंदन तेज प्रतापी, भक्तन के प्रतिपाला।
श्री बालाजी संग विराजे, महिमा अति विशाला॥
चौकी पर प्रभु रूप सुहावे, हाथ खड्ग सोहे।
दुखियों के सब दुख हरते, त्रिभुवन जन मोहे॥
जय प्रेतराज कृपालु मेरी अरज अब सुन लीजिये...

बालाजी की सेना के, तुम सेनापति वीर।
भक्त जनों के काज संवारे, हरे भक्त की पीर॥
दंड प्रचंड तुम्हारो स्वामी, दुष्टन को संहारे।
जो जन शरण तुम्हारी आए, तिनके काज संवारे॥
जय प्रेतराज कृपालु मेरी अरज अब सुन लीजिये...

कंचन थाल कपूर सुहाई, आरती करत पुकारी।
जय प्रेतराज देव दयाला, विपदा हरो हमारी॥
आरती जो कोई नर-नारी गावे, मनवांछित फल पावे।
कहत दास प्रभु शरण तिहारी, यम का भय न सतावे॥
जय प्रेतराज कृपालु मेरी अरज अब सुन लीजिये।
मैं शरण तुम्हारी आ गया, मेरी विपदा को हर लीजिये॥

॥ श्री प्रेतराज सरकार चालीसा ॥

॥ दोहा ॥
सुमिरि चरण प्रेतराज के, धरम धुरंधर धीर।
हरहु सकल भव आपदा, मेटहु जन की पीर॥

॥ चौपाई ॥
जय जय जय प्रेतराज बलिवाना। दुष्ट दलन पावन भगवाना॥
तुम सुग्रीव राम के दासा। पूरन करहु भक्त अभिलाषा॥
श्री बालाजी के संग विराजे। चौकी पर प्रभु रूप सुहाजे॥
भूत पिशाच डाकिनी भागी। तुम्हरी शरण जो प्राणी लागी॥
रोग दोष सब पल में टारो। कृपा दृष्टि कर भक्त उबारो॥
जो जन ध्यान धरे मन लाई। ताके संकट देहु मिटाई॥
जय प्रेतराज देव दयाला। सदा करहु भक्तन प्रतिपाला॥
असुर निकंदन परम प्रतापी। कांपत थर-थर अधमी पापी॥
भैरव संग तुम्हारी जोड़ी। महिमा कही न जाय निहोरी॥
हाथ में सोटा खड्ग विराजे। दुष्ट दलन रण डंका बाजे॥
जो जन अर्जी धाम लगावे। प्रेतराज ताके कष्ट नशावे॥
परम पुनीत धाम कल्याणा। डूँगरा जाट महिमा जग जाना॥
गुरुदेव तेजवीर की बानी। संकट कटे सुखी हो प्राणी॥
जो यह चालीसा नित ध्यावै। सर्व सुखों का भोग लगावै॥
पुत्र पौत्र धन धान्य बढ़ावै। अन्त काल प्रभु पद को पावै॥

॥ दोहा ॥
प्रेतराज सरकार की, जो नित आरती गाय।
सकल मनोरथ सिद्ध हों, संकट दूर नशाय॥
"""

output_path = "scratch/temp_pretraj.mp3"

async def main():
    communicate = edge_tts.Communicate(text, "hi-IN-MadhurNeural", rate="-6%", pitch="+0Hz")
    await communicate.save(output_path)
    print("Generated Pretraj audio, size:", os.path.getsize(output_path))
    
    # Upload to hostinger
    boundary = '----WebKitFormBoundaryPretraj'
    with open(output_path, 'rb') as f:
        file_bytes = f.read()

    body = bytearray()
    body.extend(f'--{boundary}\r\n'.encode('utf-8'))
    body.extend(b'Content-Disposition: form-data; name="audio"; filename="pretraj_chalisa_authentic.mp3"\r\n')
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
        print('SUCCESS! Uploaded Pretraj Audio URL:', res.get('audio_url'))

asyncio.run(main())
