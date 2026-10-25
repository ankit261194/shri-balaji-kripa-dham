import asyncio
import edge_tts
import os

text = """
॥ श्री वीर हनुमंत साबर रक्षा मंत्र ॥

ॐ नमो वीर बली हनुमंत जी, मुग्दर दाहिने हाथ।
मार मार पछाड़िया, पर्वत बाएं हाथ॥

धूं धूं धूं धूंकार चले, काल भैरों किलकिलाय।
हनुमंत वीर रण में धावे, दुष्टन को मार भगावे॥

भूत प्रेत पिशाच डाकिनी, शाकिनी बेताल कांपत थर थर।
जो कोई सतावे बालाजी के भक्त को, ताहि मुग्दर से चूर चूर कर॥

बजरंग बाण की आन, माता अंजनी की आन,
श्री रामचन्द्र जी की दुहाई।
शब्द सांचा, पिण्ड कांचा, फुरो मन्त्र ईश्वरो वाचा॥
"""

async def generate():
    communicate = edge_tts.Communicate(text, "hi-IN-MadhurNeural", rate="-5%", pitch="+0Hz")
    await communicate.save("scratch/test_shabar.mp3")
    print("Generated! Size:", os.path.getsize("scratch/test_shabar.mp3"))

asyncio.run(generate())
