package com.example.shribalajikripadham.data.model

data class SevadarProfile(
    val id: Long = 0,
    val name: String,
    val roleTitleHindi: String = "सेवादार",
    val roleTitleEnglish: String = "Sevadar",
    val phoneNumber: String,
    val dutyHindi: String = "",
    val dutyEnglish: String = "",
    val initials: String = "",
    val photoUri: String = "",
    val displayOrder: Int = 0,
    val isActive: Boolean = true
) {
    companion object {
        fun defaultProfiles(): List<SevadarProfile> = AshramDataDefaults.sevadars
    }
}

object AshramDataDefaults {
    val sevadars: List<SevadarProfile> = listOf(
        SevadarProfile(
            id = 1,
            name = "अंकित शर्मा",
            roleTitleHindi = "मुख्य व्यवस्थापक",
            roleTitleEnglish = "Head Manager",
            phoneNumber = "9876543210",
            dutyHindi = "दरबार एवं आश्रम मुख्य व्यवस्था",
            initials = "अं",
            photoUri = ""
        ),
        SevadarProfile(
            id = 2,
            name = "दीपक कुमार",
            roleTitleHindi = "कतार व्यवस्था प्रमुख",
            roleTitleEnglish = "Queue Manager",
            phoneNumber = "9876543211",
            dutyHindi = "टोकन व भक्त कतार नियंत्रण",
            initials = "दी",
            photoUri = ""
        ),
        SevadarProfile(
            id = 3,
            name = "राहुल सिंह",
            roleTitleHindi = "प्रसाद व भंडारा प्रमुख",
            roleTitleEnglish = "Prasad Incharge",
            phoneNumber = "9876543212",
            dutyHindi = "महाप्रसाद वितरण सेवा",
            initials = "रा",
            photoUri = ""
        ),
        SevadarProfile(
            id = 4,
            name = "सोनू तेवतिया",
            roleTitleHindi = "सुरक्षा व अनुशासन प्रमुख",
            roleTitleEnglish = "Security Head",
            phoneNumber = "9876543213",
            dutyHindi = "परिसर सुरक्षा एवं व्यवस्था",
            initials = "सो",
            photoUri = ""
        )
    )
}
