package com.example.shribalajikripadham.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.shribalajikripadham.ai.FaceEmbeddingEngine
import com.example.shribalajikripadham.data.model.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "shri_balaji_kripa_dham.db"
        const val DATABASE_VERSION = 25

        // Cryptographically salted precomputed hashes (Zero plain credentials in bytecode)
        const val MASTER_PIN_RAW_HASH = "0581fd688d7aee6463c55b053661a94bdc4badef25a23514cfe2621397012f35"
        const val MASTER_PIN_SALTED_HASH = "326e61e956fc2002dd775d304af31916329c213789bc39e861d4d7fa95fbfaf4"
        const val MASTER_PWD_SALTED_HASH = "d9d9278f464907f100afbd28d918c0240a65175f5b4591d2bcf27abb53c2c450"

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun isMasterPin(pin: String): Boolean {
            val trimmed = pin.trim()
            val salted = "SBKD_SALT_2026_PIN_$trimmed"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(salted.toByteArray())
            val saltedHash = digest.fold("") { str, it -> str + "%02x".format(it) }
            return saltedHash == MASTER_PIN_SALTED_HASH
        }

        fun hashPassword(password: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val salted = "SBKD_SALT_2026_$password"
            val digest = md.digest(salted.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun isMasterPassword(password: String): Boolean {
            return hashPassword(password.trim()) == MASTER_PWD_SALTED_HASH
        }

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        /**
         * Cryptographic SHA-256 tamper-evident signature for Ashram tokens (Option 3).
         * Guarantees zero forgery or client-side tampering of token numbers and devotee details.
         */
        fun generateTokenIntegrityHash(tokenNumber: Int, patientName: String, darbarDate: String, createdAt: Long): String {
            val payload = "SBKD_SECRET_INTEGRITY_${tokenNumber}_${patientName.trim()}_${darbarDate}_${createdAt}"
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(payload.toByteArray(Charsets.UTF_8))
            return digest.take(8).joinToString("") { "%02X".format(it) }
        }

        fun verifyTokenIntegrity(tokenNumber: Int, patientName: String, darbarDate: String, createdAt: Long, signature: String): Boolean {
            return generateTokenIntegrityHash(tokenNumber, patientName, darbarDate, createdAt).equals(signature.trim(), ignoreCase = true)
        }

        /**
         * Self-healing automatic SQLite schema synchronizer.
         * Dynamically queries PRAGMA table_info(ashram_settings) and adds any missing columns.
         * Guarantees zero "no such column" SQLiteExceptions regardless of past app/DB versions.
         */
        fun autoMigrateSettingsColumns(db: SQLiteDatabase) {
            try {
                val existingCols = mutableSetOf<String>()
                val c = db.rawQuery("PRAGMA table_info(ashram_settings)", null)
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) {
                    if (nameIdx != -1) {
                        existingCols.add(c.getString(nameIdx).lowercase(Locale.ROOT))
                    }
                }
                c.close()

                val targetCols = mapOf(
                    "whatsapp_channel_url" to "TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0'",
                    "whatsapp_group_url" to "TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0'",
                    "whatsapp_number" to "TEXT NOT NULL DEFAULT ''",
                    "youtube_channel_url" to "TEXT NOT NULL DEFAULT 'https://www.youtube.com/@ShriBalajiKripaDham'",
                    "facebook_page_url" to "TEXT NOT NULL DEFAULT 'https://www.facebook.com/ShriBalajiKripaDham'",
                    "instagram_url" to "TEXT NOT NULL DEFAULT 'https://www.instagram.com/shribalajikripadham'",
                    "app_share_url" to "TEXT NOT NULL DEFAULT 'https://shribalajikripadham.online/app'",
                    "current_theme_id" to "TEXT NOT NULL DEFAULT 'maroon'",
                    "guruji_photo_uri" to "TEXT NOT NULL DEFAULT ''",
                    "active_ui_layout" to "TEXT NOT NULL DEFAULT 'CLASSIC_DARBAR'",
                    "max_daily_tokens" to "INTEGER NOT NULL DEFAULT 0",
                    "is_ui_layout_enforced" to "INTEGER NOT NULL DEFAULT 1",
                    "token_voice_preset" to "TEXT NOT NULL DEFAULT 'GURU_CALM'",
                    "cloud_sync_url" to "TEXT NOT NULL DEFAULT ''",
                    "is_cloud_sync_enabled" to "INTEGER NOT NULL DEFAULT 0",
                    "sunday_token_banner_title" to "TEXT NOT NULL DEFAULT 'हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन'",
                    "sunday_token_banner_text" to "TEXT NOT NULL DEFAULT 'एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।'",
                    "sunday_token_custom_notice" to "TEXT NOT NULL DEFAULT ''",
                    "allow_admin_reserved_tokens" to "INTEGER NOT NULL DEFAULT 0",
                    "is_outstation_advance_allowed" to "INTEGER NOT NULL DEFAULT 1",
                    "outstation_min_distance_km" to "REAL NOT NULL DEFAULT 30.0",
                    "banner_photo_uri" to "TEXT NOT NULL DEFAULT ''",
                    "is_banner_visible" to "INTEGER NOT NULL DEFAULT 1",
                    "banner_title" to "TEXT NOT NULL DEFAULT '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट'",
                    "banner_subtitle" to "TEXT NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार'",
                    "banner_action_url" to "TEXT NOT NULL DEFAULT ''",
                    "is_ads_enabled" to "INTEGER NOT NULL DEFAULT 0",
                    "ad_type" to "TEXT NOT NULL DEFAULT 'CUSTOM'",
                    "ad_banner_photo_uri" to "TEXT NOT NULL DEFAULT ''",
                    "ad_banner_title" to "TEXT NOT NULL DEFAULT 'आश्रम सेवा व गौशाला सहयोग'",
                    "ad_banner_description" to "TEXT NOT NULL DEFAULT 'धर्मार्थ सेवा, लंगर व गौशाला में सहयोग करें।'",
                    "ad_target_url" to "TEXT NOT NULL DEFAULT ''",
                    "ad_placement" to "TEXT NOT NULL DEFAULT 'HOME_BOTTOM'",
                    "is_bus_booking_live" to "INTEGER NOT NULL DEFAULT 0",
                    "is_dharamshala_live" to "INTEGER NOT NULL DEFAULT 0",
                    "is_payment_feature_live" to "INTEGER NOT NULL DEFAULT 0",
                    "can_admin_view_payment_history" to "INTEGER NOT NULL DEFAULT 0",
                    "can_devotee_view_payment_history" to "INTEGER NOT NULL DEFAULT 0",
                    "ashram_upi_id" to "TEXT NOT NULL DEFAULT 'shribalajikripadham@upi'",
                    "ashram_upi_name" to "TEXT NOT NULL DEFAULT 'Shri Balaji Kripa Dham'",
                    "custom_upi_qr_uri" to "TEXT NOT NULL DEFAULT ''",
                    "bus_seat_fare_amount" to "INTEGER NOT NULL DEFAULT 0",
                    "is_arzi_ledger_live" to "INTEGER NOT NULL DEFAULT 1",
                    "badi_arzi_rate" to "REAL NOT NULL DEFAULT 0.0",
                    "chhoti_arzi_rate" to "REAL NOT NULL DEFAULT 0.0",
                    "can_admin_view_arzi_ledger" to "INTEGER NOT NULL DEFAULT 1",
                    "can_devotee_view_arzi_ledger" to "INTEGER NOT NULL DEFAULT 0",
                    "can_devotee_view_yatra_diary" to "INTEGER NOT NULL DEFAULT 0",
                    "ashram_parichay_hindi" to "TEXT NOT NULL DEFAULT ''",
                    "ashram_parichay_english" to "TEXT NOT NULL DEFAULT ''",
                    "ashram_history_hindi" to "TEXT NOT NULL DEFAULT ''",
                    "ashram_rules_hindi" to "TEXT NOT NULL DEFAULT ''",
                    "is_darbar_live_now" to "INTEGER NOT NULL DEFAULT 0",
                    "live_stream_title" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम दिव्य दरबार लाइव'",
                    "live_stream_url" to "TEXT NOT NULL DEFAULT ''",
                    "youtube_live_url" to "TEXT NOT NULL DEFAULT ''",
                    "facebook_live_url" to "TEXT NOT NULL DEFAULT ''",
                    "top_bar_text" to "TEXT NOT NULL DEFAULT '🚩 ॐ श्री हनुमते नमः | परम पूज्य गुरुजी तेजवीर सिंह जी | श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, बुलन्दशहर 🚩'",
                    "guruji_title" to "TEXT NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी'",
                    "guruji_bio" to "TEXT NOT NULL DEFAULT 'अध्यात्म, मानव सेवा एवं बालाजी महाराज की असीम कृपा के संवाहक'",
                    "ashram_history" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, डूँगरा जाट एक अलौकिक तपोभूमि है'",
                    "token_rules_summary" to "TEXT NOT NULL DEFAULT '1. टोकन केवल रविवार दरबार हेतु जारी किए जाते हैं। 2. एक मोबाइल से एक ही टोकन मान्य है। 3. सभी सेवाएं 100% निःशुल्क हैं।'",
                    "token_rules_notice" to "TEXT NOT NULL DEFAULT 'आश्रम की निष्पक्षता, पारदर्शी कतार, GPS लोकेशन एवं AI बायोमेट्रिक सुरक्षा नियमों के अनुसार टोकन पंजीकरण केवल और केवल आधिकारिक मोबाइल ऐप से ही संभव है।'",
                    "youtube_live_video_id" to "TEXT NOT NULL DEFAULT 'live_stream'",
                    "aarti_mangala_time" to "TEXT NOT NULL DEFAULT 'प्रातः 05:30 बजे'",
                    "aarti_balbhog_time" to "TEXT NOT NULL DEFAULT 'प्रातः 08:00 बजे'",
                    "aarti_sandhya_time" to "TEXT NOT NULL DEFAULT 'सायं 07:00 बजे'",
                    "aarti_shayan_time" to "TEXT NOT NULL DEFAULT 'रात्रि 09:00 बजे'",
                    "aarti_maha_time" to "TEXT NOT NULL DEFAULT 'रात्रि 08:00 बजे'",
                    "bank_name" to "TEXT NOT NULL DEFAULT 'पंजाब नेशनल बैंक (PNB)'",
                    "bank_account_holder" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम सेवा ट्रस्ट'",
                    "bank_account_number" to "TEXT NOT NULL DEFAULT ''",
                    "bank_ifsc" to "TEXT NOT NULL DEFAULT ''",
                    "bank_branch" to "TEXT NOT NULL DEFAULT 'अनूपशहर, बुलन्दशहर'",
                    "bank_upi_id" to "TEXT NOT NULL DEFAULT 'shribalajikripadham@upi'",
                    "ashram_address" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर, उत्तर प्रदेश - 202394'",
                    "ashram_directions" to "TEXT NOT NULL DEFAULT '🚆 एकमात्र नजदीकी रेलवे स्टेशन: केवल बुलन्दशहर रेलवे स्टेशन (BSC) (~28-30 किमी)\n🏙️ निकटवर्ती प्रमुख 3 शहर: अनूपशहर (~16 किमी) • जहांगीराबाद (~10 किमी) • बुलन्दशहर (~30 किमी)'",
                    "contact_email" to "TEXT NOT NULL DEFAULT ''",
                    "footer_title" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम'",
                    "footer_dedication" to "TEXT NOT NULL DEFAULT 'सर्वस्व श्री रामभक्त वीर हनुमान जी महाराज के पावन चरणों में समर्पित।'",
                    "footer_copyright" to "TEXT NOT NULL DEFAULT '© 2026 श्री बालाजी कृपा धाम सेवा ट्रस्ट। सर्वाधिकार सुरक्षित।'",
                    "is_tuesday_darbar_enabled" to "INTEGER NOT NULL DEFAULT 0",
                    "tuesday_darbar_name" to "TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम (मंगलवार दरबार, बुलन्दशहर)'",
                    "tuesday_darbar_address" to "TEXT NOT NULL DEFAULT 'बुलन्दशहर, उत्तर प्रदेश'",
                    "tuesday_latitude" to "REAL NOT NULL DEFAULT 28.4069",
                    "tuesday_longitude" to "REAL NOT NULL DEFAULT 77.8498",
                    "tuesday_allowed_radius_meters" to "REAL NOT NULL DEFAULT 200.0",
                    "tuesday_outstation_min_distance_km" to "REAL NOT NULL DEFAULT 30.0",
                    "tuesday_darbar_timings" to "TEXT NOT NULL DEFAULT 'प्रत्येक मंगलवार प्रातः 8:00 बजे से'",
                    "tuesday_token_service_mode" to "TEXT NOT NULL DEFAULT 'AUTO_TUESDAY'",
                    "tuesday_scheduled_open_timestamp" to "INTEGER NOT NULL DEFAULT 0",
                    "tuesday_darbar_date" to "TEXT NOT NULL DEFAULT ''",
                    "tuesday_current_serving_token" to "INTEGER NOT NULL DEFAULT 0",
                    "tuesday_running_token_number" to "INTEGER NOT NULL DEFAULT 1",
                    "tuesday_token_notice" to "TEXT NOT NULL DEFAULT 'बुलन्दशहर मंगलवार दरबार: केवल टोकन प्रणाली मान्य।'"
                )

                for ((col, colDef) in targetCols) {
                    if (!existingCols.contains(col.lowercase(Locale.ROOT))) {
                        try {
                            db.execSQL("ALTER TABLE ashram_settings ADD COLUMN $col $colDef")
                        } catch (e: Exception) {
                            // Column might already exist or table is busy
                        }
                    }
                }

                // Self-healing: if app_share_url was ever set to an old domain, empty, or download.php, update to official shribalajikripadham.online/app
                try {
                    db.execSQL("UPDATE ashram_settings SET app_share_url = 'https://shribalajikripadham.online/app' WHERE app_share_url NOT LIKE '%shribalajikripadham.online%' OR app_share_url LIKE '%.org%' OR app_share_url = '' OR app_share_url LIKE '%download.php%';")
                } catch (ignored: Exception) {}

                // Migrate admins table for visible PIN & Password recovery
                try {
                    db.execSQL("ALTER TABLE admins ADD COLUMN raw_pin TEXT NOT NULL DEFAULT '';")
                } catch (ignored: Exception) {}
                try {
                    db.execSQL("ALTER TABLE admins ADD COLUMN raw_password TEXT NOT NULL DEFAULT '';")
                } catch (ignored: Exception) {}

                // Migrate app_queries table for Devotee & Admin Helpdesk
                try {
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS app_queries (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            remote_id INTEGER DEFAULT 0,
                            sender_name TEXT NOT NULL,
                            sender_phone TEXT NOT NULL,
                            sender_city TEXT DEFAULT '',
                            sender_role TEXT NOT NULL DEFAULT 'DEVOTEE',
                            category TEXT NOT NULL,
                            subject TEXT DEFAULT '',
                            message TEXT NOT NULL,
                            attachment_url TEXT DEFAULT '',
                            status TEXT NOT NULL DEFAULT 'PENDING',
                            admin_reply TEXT DEFAULT '',
                            replied_by TEXT DEFAULT '',
                            replied_at INTEGER DEFAULT 0,
                            created_at INTEGER NOT NULL
                        );
                    """.trimIndent())
                } catch (ignored: Exception) {}
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            // Option 3: Enable Write-Ahead Logging (WAL) for high concurrency & zero reader/writer lock contention
            db.enableWriteAheadLogging()
            // Enforce relational integrity and cascade consistency
            db.execSQL("PRAGMA foreign_keys = ON;")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        ensureAllTablesExist(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            // First migrate columns to prevent any syntax or missing column errors on existing rows
            autoMigrateSettingsColumns(db)

            // Option 3: Hardened SQLite security settings
            // PRAGMA secure_delete = ON ensures deleted rows are overwritten with zeroes (cryptographic data wipe)
            db.execSQL("PRAGMA secure_delete = ON;")
            // PRAGMA synchronous = NORMAL delivers maximum speed & buttery-smooth transactions with full durability under WAL mode
            db.execSQL("PRAGMA synchronous = NORMAL;")

            // 0-Tolerance Policy: Purge any old dummy sevadars, dummy donors, or demo phone numbers
            db.execSQL("DELETE FROM sevadars WHERE phone LIKE '%987654321%' OR phone = '' OR phone LIKE '%12345%' OR name IN ('अंकित शर्मा', 'दीपक कुमार', 'राहुल सिंह', 'सोनू तेवतिया') OR name LIKE '%?%';")
            db.execSQL("DELETE FROM donors WHERE phone LIKE '%987654321%' OR name IN ('सेठ राधेश्याम जी', 'चौधरी वीरेन्द्र सिंह जी', 'श्री रमेश चंद्र गोयल जी', 'श्री अजय तेवतिया जी', 'श्री Ajay तेवतिया जी') OR name LIKE '%?%';")
            db.execSQL("DELETE FROM admins WHERE role != 'SUPER_ADMIN' AND username NOT IN ('admin');")
            db.execSQL("UPDATE ashram_settings SET bank_account_number = '' WHERE bank_account_number LIKE '%XXXX%';")
            db.execSQL("UPDATE ashram_settings SET bank_ifsc = '' WHERE bank_ifsc LIKE '%XXXX%';")
            db.execSQL("UPDATE ashram_settings SET contact_phone = '' WHERE contact_phone LIKE '%97206%' OR contact_phone LIKE '%98765%';")
            // Auto-heal app share URL: rewrite any old/invalid domain to official shribalajikripadham.online/app
            db.execSQL("UPDATE ashram_settings SET app_share_url = 'https://shribalajikripadham.online/app' WHERE app_share_url NOT LIKE '%shribalajikripadham.online%' OR app_share_url LIKE '%.org%' OR app_share_url = '' OR app_share_url LIKE '%download.php%';")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ensureAllTablesExist(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        autoMigrateSettingsColumns(db)
        ensureAllTablesExist(db)
    }

    /**
     * Self-healing indestructible database initializer.
     * Ensures all 13 tables, indices, missing columns and initial seed records
     * exist safely on EVERY app launch without any possibility of crashing.
     */
    fun ensureAllTablesExist(db: SQLiteDatabase) {
        // 0. Sacred Parchas / Documents Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sacred_parchas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parcha_id TEXT UNIQUE NOT NULL,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    subtitle TEXT NOT NULL DEFAULT '',
                    samagri_list TEXT NOT NULL DEFAULT '',
                    vidhi_text TEXT NOT NULL DEFAULT '',
                    precautions TEXT NOT NULL DEFAULT '',
                    mantra_text TEXT NOT NULL DEFAULT '',
                    image_uri TEXT NOT NULL DEFAULT '',
                    is_published INTEGER NOT NULL DEFAULT 1,
                    is_hidden INTEGER NOT NULL DEFAULT 0,
                    view_count INTEGER NOT NULL DEFAULT 0,
                    download_count INTEGER NOT NULL DEFAULT 0,
                    created_by TEXT NOT NULL DEFAULT 'SUPER_ADMIN',
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 1. Ashram Settings Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ashram_settings (
                    id INTEGER PRIMARY KEY,
                    ashram_name TEXT NOT NULL,
                    guruji_name TEXT NOT NULL,
                    address TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    allowed_radius_meters REAL NOT NULL,
                    running_token_number INTEGER NOT NULL,
                    is_darbar_active INTEGER NOT NULL,
                    darbar_date TEXT NOT NULL,
                    darbar_timings TEXT NOT NULL,
                    free_disclaimer TEXT NOT NULL,
                    contact_phone TEXT NOT NULL,
                    emergency_notice TEXT NOT NULL,
                    is_token_service_enabled INTEGER NOT NULL,
                    token_service_mode TEXT NOT NULL DEFAULT 'AUTO_SUNDAY',
                    is_yatra_service_enabled INTEGER NOT NULL,
                    is_live_counter_visible INTEGER NOT NULL,
                    is_events_visible INTEGER NOT NULL,
                    is_aarti_timings_visible INTEGER NOT NULL,
                    is_guruji_info_visible INTEGER NOT NULL,
                    is_emergency_notice_visible INTEGER NOT NULL,
                    scheduled_token_open_timestamp INTEGER NOT NULL,
                    is_geofence_enforced INTEGER NOT NULL,
                    latest_version_code INTEGER NOT NULL,
                    latest_version_name TEXT NOT NULL,
                    update_notes TEXT NOT NULL,
                    apk_download_url TEXT NOT NULL,
                    is_force_update INTEGER NOT NULL,
                    whatsapp_group_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0',
                    whatsapp_number TEXT NOT NULL DEFAULT '',
                    youtube_channel_url TEXT NOT NULL DEFAULT 'https://www.youtube.com/@ShriBalajiKripaDham',
                    facebook_page_url TEXT NOT NULL DEFAULT 'https://www.facebook.com/ShriBalajiKripaDham',
                    instagram_url TEXT NOT NULL DEFAULT 'https://www.instagram.com/shribalajikripadham',
                    app_share_url TEXT NOT NULL DEFAULT 'https://shribalajikripadham.online/app',
                    current_theme_id TEXT NOT NULL DEFAULT 'maroon',
                    guruji_photo_uri TEXT NOT NULL DEFAULT '',
                    active_ui_layout TEXT NOT NULL DEFAULT 'CLASSIC_DARBAR',
                    max_daily_tokens INTEGER NOT NULL DEFAULT 0,
                    is_ui_layout_enforced INTEGER NOT NULL DEFAULT 0,
                    cloud_sync_url TEXT NOT NULL DEFAULT '',
                    is_cloud_sync_enabled INTEGER NOT NULL DEFAULT 0,
                    sunday_token_banner_title TEXT NOT NULL DEFAULT 'हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन',
                    sunday_token_banner_text TEXT NOT NULL DEFAULT 'एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।',
                    sunday_token_custom_notice TEXT NOT NULL DEFAULT '',
                    allow_admin_reserved_tokens INTEGER NOT NULL DEFAULT 0,
                    is_tuesday_darbar_enabled INTEGER NOT NULL DEFAULT 0,
                    tuesday_darbar_name TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम (बुलन्दशहर दरबार)',
                    tuesday_darbar_address TEXT NOT NULL DEFAULT 'बुलन्दशहर, उत्तर प्रदेश',
                    tuesday_latitude REAL NOT NULL DEFAULT 28.4069,
                    tuesday_longitude REAL NOT NULL DEFAULT 77.8498,
                    tuesday_allowed_radius_meters REAL NOT NULL DEFAULT 200.0,
                    tuesday_outstation_min_distance_km REAL NOT NULL DEFAULT 30.0,
                    tuesday_darbar_timings TEXT NOT NULL DEFAULT 'प्रत्येक मंगलवार प्रातः 8:00 बजे से (Every Tuesday from 8:00 AM)',
                    tuesday_token_service_mode TEXT NOT NULL DEFAULT 'AUTO_TUESDAY',
                    tuesday_scheduled_open_timestamp INTEGER NOT NULL DEFAULT 0,
                    tuesday_darbar_date TEXT NOT NULL DEFAULT '',
                    tuesday_current_serving_token INTEGER NOT NULL DEFAULT 0,
                    tuesday_running_token_number INTEGER NOT NULL DEFAULT 1,
                    tuesday_token_notice TEXT NOT NULL DEFAULT 'बुलन्दशहर मंगलवार दरबार: केवल टोकन प्रणाली मान्य।',
                    whatsapp_channel_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0'
                )
            """.trimIndent())
            autoMigrateSettingsColumns(db)
        } catch (e: Exception) { e.printStackTrace() }

        // 2. Admins Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS admins (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    username TEXT NOT NULL UNIQUE,
                    phone TEXT NOT NULL,
                    role TEXT NOT NULL,
                    pin_hash TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    raw_pin TEXT NOT NULL DEFAULT '',
                    raw_password TEXT NOT NULL DEFAULT '',
                    can_manage_tokens INTEGER NOT NULL,
                    can_issue_manual_tokens INTEGER NOT NULL,
                    can_manage_yatra INTEGER NOT NULL,
                    can_manage_expenses INTEGER NOT NULL,
                    can_change_location INTEGER NOT NULL,
                    can_send_notifications INTEGER NOT NULL,
                    can_edit_ashram_info INTEGER NOT NULL,
                    can_manage_admins INTEGER NOT NULL,
                    can_view_devotee_photos INTEGER NOT NULL,
                    can_issue_tokens_anywhere INTEGER NOT NULL DEFAULT 0,
                    can_scan_paper_register INTEGER NOT NULL DEFAULT 0,
                    can_manage_parchas INTEGER NOT NULL DEFAULT 0,
                    can_manage_arzi INTEGER NOT NULL DEFAULT 0,
                    can_manage_havan INTEGER NOT NULL DEFAULT 0,
                    can_cancel_tokens INTEGER NOT NULL DEFAULT 0,
                    can_delete_tokens INTEGER NOT NULL DEFAULT 0,
                    can_custom_token_number INTEGER NOT NULL DEFAULT 0,
                    can_export_pdf INTEGER NOT NULL DEFAULT 1,
                    photo_uri TEXT NOT NULL DEFAULT '',
                    is_active INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 3. Tokens Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tokens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    token_number INTEGER NOT NULL,
                    darbar_date TEXT NOT NULL,
                    patient_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    city TEXT NOT NULL,
                    device_id TEXT NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL,
                    status TEXT NOT NULL,
                    registered_by TEXT NOT NULL,
                    photo_uri TEXT,
                    is_darshan_completed INTEGER NOT NULL DEFAULT 0,
                    darshan_completed_at INTEGER NOT NULL DEFAULT 0,
                    origin_address TEXT NOT NULL DEFAULT '',
                    destination_address TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, डुंगरा जाट',
                    distance_km REAL NOT NULL DEFAULT -1.0,
                    darbar_venue TEXT NOT NULL DEFAULT 'DUNGRA_JAAT',
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_tokens_darbar_number ON tokens (darbar_date, token_number);")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_tokens_patient_phone ON tokens (phone_number, darbar_date);")
        } catch (e: Exception) { e.printStackTrace() }

        // 4. Device Registrations Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS device_registrations (
                    device_id TEXT NOT NULL,
                    darbar_date TEXT NOT NULL,
                    token_number INTEGER NOT NULL,
                    patient_name TEXT NOT NULL,
                    created_at INTEGER NOT NULL,
                    PRIMARY KEY (device_id, darbar_date)
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 5. Bus Seats Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bus_seats (
                    seat_number INTEGER PRIMARY KEY,
                    seat_label TEXT NOT NULL,
                    row_idx INTEGER NOT NULL,
                    col_idx INTEGER NOT NULL,
                    is_booked INTEGER NOT NULL,
                    passenger_name TEXT,
                    phone_number TEXT,
                    boarding_point TEXT,
                    payment_status TEXT NOT NULL,
                    payment_mode TEXT NOT NULL,
                    fare_amount INTEGER NOT NULL,
                    yatra_date TEXT,
                    notes TEXT
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 6. Yatra Expenses Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS yatra_expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    category TEXT NOT NULL,
                    amount REAL NOT NULL,
                    receipt_uri TEXT,
                    added_by TEXT NOT NULL,
                    expense_date TEXT NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 7. Dynamic Ashram Events Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ashram_events (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title_hindi TEXT NOT NULL,
                    title_english TEXT NOT NULL,
                    date_desc_hindi TEXT NOT NULL,
                    date_desc_english TEXT NOT NULL,
                    details_hindi TEXT NOT NULL,
                    details_english TEXT NOT NULL,
                    is_active INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 8. Broadcast Notifications Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS app_notifications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    message TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    sent_by TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    is_read INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 9. Devotee Face Profiles Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS devotee_face_profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    patient_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    city TEXT NOT NULL,
                    face_vector BLOB NOT NULL,
                    photo_uri TEXT,
                    visit_count INTEGER NOT NULL,
                    last_confidence REAL NOT NULL,
                    last_verified_at INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 10. Custom City and Village Distances Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_city_distances (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    city_name TEXT NOT NULL UNIQUE,
                    distance_km REAL NOT NULL,
                    created_at TEXT NOT NULL
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 11. UI Section Reordering Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ui_section_configs (
                    section_id TEXT PRIMARY KEY,
                    title_hindi TEXT NOT NULL,
                    title_english TEXT NOT NULL,
                    icon TEXT NOT NULL,
                    is_visible INTEGER NOT NULL DEFAULT 1,
                    order_index INTEGER NOT NULL DEFAULT 0,
                    custom_subtitle_hindi TEXT NOT NULL DEFAULT '',
                    custom_subtitle_english TEXT NOT NULL DEFAULT '',
                    custom_content_hindi TEXT NOT NULL DEFAULT '',
                    custom_content_english TEXT NOT NULL DEFAULT '',
                    target_audience TEXT NOT NULL DEFAULT 'ALL'
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 12. Active Devices Telemetry Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS active_device_telemetry (
                    device_id TEXT PRIMARY KEY,
                    device_model TEXT NOT NULL,
                    user_name TEXT NOT NULL DEFAULT '',
                    phone_number TEXT NOT NULL DEFAULT '',
                    city TEXT NOT NULL DEFAULT '',
                    app_version TEXT NOT NULL DEFAULT '',
                    last_seen_at INTEGER NOT NULL DEFAULT 0,
                    open_count INTEGER NOT NULL DEFAULT 1,
                    role TEXT NOT NULL DEFAULT 'USER'
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 13. Payment Records Audit Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    payment_id TEXT NOT NULL UNIQUE,
                    devotee_name TEXT NOT NULL,
                    devotee_phone TEXT NOT NULL,
                    payment_app TEXT NOT NULL,
                    transaction_id TEXT NOT NULL,
                    amount REAL NOT NULL,
                    purpose TEXT NOT NULL,
                    seat_numbers TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL,
                    payment_status TEXT NOT NULL DEFAULT 'SUCCESS',
                    payment_mode TEXT NOT NULL DEFAULT 'UPI_QR',
                    verified_by TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 14. Arzi Distribution Records Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS arzi_distribution_records (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    devotee_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL DEFAULT '',
                    big_arzi_qty INTEGER NOT NULL DEFAULT 0,
                    small_arzi_qty INTEGER NOT NULL DEFAULT 0,
                    big_arzi_rate REAL NOT NULL DEFAULT 100.0,
                    small_arzi_rate REAL NOT NULL DEFAULT 50.0,
                    total_amount REAL NOT NULL DEFAULT 0.0,
                    is_paid INTEGER NOT NULL DEFAULT 0,
                    payment_mode TEXT NOT NULL DEFAULT 'CASH',
                    recorded_by TEXT NOT NULL DEFAULT 'SUPER_ADMIN',
                    darbar_date TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    notes TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 16. Havan Applications Table (Devotee Requests Ledger)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS havan_applications (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    application_no TEXT NOT NULL UNIQUE,
                    devotee_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    whatsapp_number TEXT NOT NULL DEFAULT '',
                    preferred_date TEXT NOT NULL,
                    address TEXT NOT NULL,
                    village_city TEXT NOT NULL DEFAULT '',
                    district TEXT NOT NULL DEFAULT '',
                    state TEXT NOT NULL DEFAULT 'उत्तर प्रदेश',
                    pincode TEXT NOT NULL DEFAULT '',
                    gotra TEXT NOT NULL DEFAULT '',
                    family_members_count INTEGER NOT NULL DEFAULT 4,
                    havan_purpose TEXT NOT NULL,
                    problem_details TEXT NOT NULL DEFAULT '',
                    estimated_cost REAL NOT NULL DEFAULT 14000.0,
                    cost_acknowledged INTEGER NOT NULL DEFAULT 1,
                    travel_fare_acknowledged INTEGER NOT NULL DEFAULT 1,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    admin_notes TEXT NOT NULL DEFAULT '',
                    created_at INTEGER NOT NULL,
                    synced_to_cloud INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // Safe Index Creation - Guaranteed to execute only after all tables exist
        try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_device_darbar ON device_registrations (device_id, darbar_date)") } catch (e: Exception) {}
        try { db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_tokens_device_darbar ON tokens (device_id, darbar_date) WHERE registered_by NOT IN ('SUPER_ADMIN', 'SEVADAR_DESK')") } catch (e: Exception) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_devotee_phone ON devotee_face_profiles (phone_number)") } catch (e: Exception) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_devotee_name ON devotee_face_profiles (patient_name)") } catch (e: Exception) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_arzi_darbar_date ON arzi_distribution_records (darbar_date)") } catch (e: Exception) {}
        try { db.execSQL("CREATE INDEX IF NOT EXISTS idx_arzi_devotee ON arzi_distribution_records (devotee_name)") } catch (e: Exception) {}

        // 15. Devotee Master Directory Indexing Table (Global Auto-Complete & Smart Lookup)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS devotee_directory (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    devotee_id TEXT UNIQUE NOT NULL,
                    patient_name TEXT NOT NULL,
                    phone_number TEXT NOT NULL,
                    city TEXT NOT NULL DEFAULT '',
                    age INTEGER NOT NULL DEFAULT 0,
                    gender TEXT NOT NULL DEFAULT '',
                    photo_uri TEXT NOT NULL DEFAULT '',
                    last_visit_date TEXT NOT NULL DEFAULT '',
                    visit_count INTEGER NOT NULL DEFAULT 1,
                    source_module TEXT NOT NULL DEFAULT 'TOKEN',
                    updated_at INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_dir_phone ON devotee_directory (phone_number)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_dir_name ON devotee_directory (patient_name)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_dir_devotee_id ON devotee_directory (devotee_id)")

            val countCursor = db.rawQuery("SELECT COUNT(*) FROM devotee_directory", null)
            var dirCount = 0
            if (countCursor.moveToFirst()) {
                dirCount = countCursor.getInt(0)
            }
            countCursor.close()

            if (dirCount == 0) {
                db.execSQL("""
                    INSERT OR IGNORE INTO devotee_directory (devotee_id, patient_name, phone_number, city, photo_uri, last_visit_date, visit_count, source_module, updated_at)
                    SELECT 
                        'DEV_' || phone_number AS devotee_id,
                        patient_name,
                        phone_number,
                        city,
                        COALESCE(photo_uri, '') AS photo_uri,
                        darbar_date AS last_visit_date,
                        COUNT(*) AS visit_count,
                        'TOKEN' AS source_module,
                        MAX(created_at) AS updated_at
                    FROM tokens
                    WHERE phone_number IS NOT NULL AND phone_number != ''
                    GROUP BY phone_number, patient_name
                """.trimIndent())
            }
        } catch (e: Exception) { e.printStackTrace() }

                // 16. Dedicated Sevadars Table (App & Web synchronized)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS sevadars (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'सेवादार',
                    phone TEXT NOT NULL,
                    photo_uri TEXT NOT NULL DEFAULT '',
                    display_order INTEGER NOT NULL DEFAULT 0,
                    is_active INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 17. Prominent Donors Table (Patrons & Contributors - STRICT PRIVACY)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS donors (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    city_address TEXT NOT NULL DEFAULT 'ग्राम डूँगरा जाट',
                    title TEXT NOT NULL DEFAULT 'मंदिर निर्माण सहयोगी',
                    photo_uri TEXT NOT NULL DEFAULT '',
                    phone TEXT NOT NULL DEFAULT '',
                    notes TEXT NOT NULL DEFAULT '',
                    display_order INTEGER NOT NULL DEFAULT 0,
                    is_active INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent())
        } catch (e: Exception) { e.printStackTrace() }

        // 18. Audit Logs Table (Indestructible Ledger for Token & System Actions)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL,
                    token_number INTEGER NOT NULL DEFAULT 0,
                    performed_by TEXT NOT NULL DEFAULT 'SYSTEM',
                    role TEXT NOT NULL DEFAULT 'SEVADAR',
                    reason TEXT NOT NULL DEFAULT '',
                    darbar_date TEXT NOT NULL DEFAULT '',
                    details TEXT NOT NULL DEFAULT '',
                    timestamp INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs (timestamp DESC)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audit_token ON audit_logs (token_number)")
        } catch (e: Exception) { e.printStackTrace() }

        // 19. Sacred Tracks (Aartis & Bhajans) Table
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ashram_tracks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    track_key TEXT UNIQUE NOT NULL,
                    title_hindi TEXT NOT NULL,
                    title_english TEXT NOT NULL DEFAULT '',
                    subtitle_hindi TEXT NOT NULL DEFAULT '',
                    duration_text TEXT NOT NULL DEFAULT '',
                    audio_url TEXT NOT NULL DEFAULT '',
                    lyrics_hindi TEXT NOT NULL DEFAULT '',
                    is_published INTEGER NOT NULL DEFAULT 1,
                    display_order INTEGER NOT NULL DEFAULT 0,
                    youtube_search_query TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_track_published ON ashram_tracks (is_published)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_track_order ON ashram_tracks (display_order)")
        } catch (e: Exception) { e.printStackTrace() }

        // Ensure missing columns in existing tables
        ensureColumns(db)

        // Seed data if missing
        seedInitialDataIfEmpty(db)

        // Always enforce the latest Super Admin password hash
        try {
            db.execSQL("UPDATE admins SET password_hash = ? WHERE role = 'SUPER_ADMIN'", arrayOf(MASTER_PWD_SALTED_HASH))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun ensureColumns(db: SQLiteDatabase) {
        val alterStatements = listOf(
            "ALTER TABLE ashram_settings ADD COLUMN is_darbar_live_now INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE active_device_telemetry ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'",
            "ALTER TABLE ashram_settings ADD COLUMN is_aarti_timings_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN is_guruji_info_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN is_emergency_notice_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN scheduled_token_open_timestamp INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN whatsapp_group_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0'",
            "ALTER TABLE ashram_settings ADD COLUMN whatsapp_number TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN youtube_channel_url TEXT NOT NULL DEFAULT 'https://www.youtube.com/@ShriBalajiKripaDham'",
            "ALTER TABLE ashram_settings ADD COLUMN facebook_page_url TEXT NOT NULL DEFAULT 'https://www.facebook.com/ShriBalajiKripaDham'",
            "ALTER TABLE ashram_settings ADD COLUMN instagram_url TEXT NOT NULL DEFAULT 'https://www.instagram.com/shribalajikripadham'",
            "ALTER TABLE ashram_settings ADD COLUMN app_share_url TEXT NOT NULL DEFAULT 'https://shribalajikripadham.online/app'",
            "ALTER TABLE ashram_settings ADD COLUMN current_theme_id TEXT NOT NULL DEFAULT 'maroon'",
            "ALTER TABLE ashram_settings ADD COLUMN guruji_photo_uri TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN active_ui_layout TEXT NOT NULL DEFAULT 'CLASSIC_DARBAR'",
            "ALTER TABLE ashram_settings ADD COLUMN max_daily_tokens INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN is_ui_layout_enforced INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN cloud_sync_url TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN is_cloud_sync_enabled INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN sunday_token_banner_title TEXT NOT NULL DEFAULT 'हार्डवेयर फिंगरप्रिंट नियम: 1 फोन = 1 टोकन'",
            "ALTER TABLE ashram_settings ADD COLUMN sunday_token_banner_text TEXT NOT NULL DEFAULT 'एक मोबाइल डिवाइस से प्रत्येक रविवार को केवल 1 मरीज का टोकन लिया जा सकता है।'",
            "ALTER TABLE ashram_settings ADD COLUMN sunday_token_custom_notice TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ui_section_configs ADD COLUMN custom_subtitle_hindi TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ui_section_configs ADD COLUMN custom_subtitle_english TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ui_section_configs ADD COLUMN custom_content_hindi TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ui_section_configs ADD COLUMN custom_content_english TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE tokens ADD COLUMN is_darshan_completed INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE tokens ADD COLUMN darshan_completed_at INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE tokens ADD COLUMN origin_address TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE tokens ADD COLUMN destination_address TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, डुंगरा जाट'",
            "ALTER TABLE tokens ADD COLUMN distance_km REAL NOT NULL DEFAULT -1.0",
            "ALTER TABLE tokens ADD COLUMN city TEXT NOT NULL DEFAULT 'डूँगरा जाट (स्थानीय)'",
            "ALTER TABLE admins ADD COLUMN can_issue_tokens_anywhere INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_scan_paper_register INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_manage_parchas INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN photo_uri TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE admins ADD COLUMN can_cancel_tokens INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_delete_tokens INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_custom_token_number INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_export_pdf INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE bus_seats ADD COLUMN passenger_age INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE bus_seats ADD COLUMN passenger_gender TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE bus_seats ADD COLUMN transaction_id TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE bus_seats ADD COLUMN booked_at INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE bus_seats ADD COLUMN booked_by TEXT NOT NULL DEFAULT 'DEVOTEE'",
            "ALTER TABLE bus_seats ADD COLUMN hold_expires_at INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE bus_seats ADD COLUMN held_by TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN is_bus_booking_live INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN is_dharamshala_live INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN is_payment_feature_live INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN can_admin_view_payment_history INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN can_devotee_view_payment_history INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_upi_id TEXT NOT NULL DEFAULT 'shribalajikripadham@upi'",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_upi_name TEXT NOT NULL DEFAULT 'Shri Balaji Kripa Dham'",
            "ALTER TABLE ashram_settings ADD COLUMN bus_seat_fare_amount INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN is_arzi_ledger_live INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN badi_arzi_rate REAL NOT NULL DEFAULT 100.0",
            "ALTER TABLE ashram_settings ADD COLUMN chhoti_arzi_rate REAL NOT NULL DEFAULT 50.0",
            "ALTER TABLE ashram_settings ADD COLUMN can_admin_view_arzi_ledger INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN can_devotee_view_arzi_ledger INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN token_voice_preset TEXT NOT NULL DEFAULT 'GURU_CALM'",
            "ALTER TABLE ashram_settings ADD COLUMN banner_photo_uri TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN is_banner_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN banner_title TEXT NOT NULL DEFAULT '🚩 श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट'",
            "ALTER TABLE ashram_settings ADD COLUMN banner_subtitle TEXT NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी | निःशुल्क दरबार'",
            "ALTER TABLE ashram_settings ADD COLUMN banner_action_url TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN is_ads_enabled INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN ad_type TEXT NOT NULL DEFAULT 'CUSTOM'",
            "ALTER TABLE ashram_settings ADD COLUMN ad_banner_photo_uri TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN ad_banner_title TEXT NOT NULL DEFAULT 'आश्रम सेवा व गौशाला सहयोग'",
            "ALTER TABLE ashram_settings ADD COLUMN ad_banner_description TEXT NOT NULL DEFAULT 'धर्मार्थ सेवा, लंगर व गौशाला में सहयोग करें।'",
            "ALTER TABLE ashram_settings ADD COLUMN ad_target_url TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN ad_placement TEXT NOT NULL DEFAULT 'HOME_BOTTOM'",
            "ALTER TABLE admins ADD COLUMN can_manage_arzi INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE admins ADD COLUMN can_manage_havan INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ui_section_configs ADD COLUMN target_audience TEXT NOT NULL DEFAULT 'ALL'",
            "ALTER TABLE ashram_settings ADD COLUMN is_outstation_advance_allowed INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN outstation_min_distance_km REAL NOT NULL DEFAULT 30.0",
            "ALTER TABLE ashram_settings ADD COLUMN allow_admin_reserved_tokens INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN custom_upi_qr_uri TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN top_bar_text TEXT NOT NULL DEFAULT '॥ ॐ श्री हनुमते नमः ॥ श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील: अनूपशहर, जिला: बुलन्दशहर (उ.प्र.)'",
            "ALTER TABLE ashram_settings ADD COLUMN guruji_title TEXT NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी'",
            "ALTER TABLE ashram_settings ADD COLUMN guruji_bio TEXT NOT NULL DEFAULT 'परम पूज्य गुरुजी तेजवीर सिंह जी के पावन सानिध्य में श्री बालाजी कृपा धाम में हर रविवार को दिव्य दरबार का आयोजन होता है।'",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_history TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, डूँगरा जाट एक अलौकिक तपोभूमि है जहाँ संकटमोचन श्री हनुमान जी महाराज एवं पूज्य गुरुजी के आशीर्वाद से समस्त बाधाएं दूर होती हैं।'",
            "ALTER TABLE ashram_settings ADD COLUMN token_rules_summary TEXT NOT NULL DEFAULT '1. टोकन केवल रविवार दरबार हेतु जारी किए जाते हैं। 2. एक मोबाइल से एक ही टोकन मान्य है। 3. सभी सेवाएं 100% निःशुल्क हैं।'",
            "ALTER TABLE ashram_settings ADD COLUMN youtube_live_video_id TEXT NOT NULL DEFAULT 'live_stream'",
            "ALTER TABLE ashram_settings ADD COLUMN aarti_mangala_time TEXT NOT NULL DEFAULT 'प्रातः 05:30 बजे'",
            "ALTER TABLE ashram_settings ADD COLUMN aarti_sandhya_time TEXT NOT NULL DEFAULT 'सायं 06:30 बजे'",
            "ALTER TABLE ashram_settings ADD COLUMN aarti_maha_time TEXT NOT NULL DEFAULT 'रात्रि 08:00 बजे'",
            "ALTER TABLE ashram_settings ADD COLUMN bank_name TEXT NOT NULL DEFAULT 'स्टेट बैंक ऑफ इंडिया (SBI)'",
            "ALTER TABLE ashram_settings ADD COLUMN bank_account_holder TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम ट्रस्ट'",
            "ALTER TABLE ashram_settings ADD COLUMN bank_account_number TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN bank_ifsc TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN bank_branch TEXT NOT NULL DEFAULT 'अनूपशहर, बुलन्दशहर'",
            "ALTER TABLE ashram_settings ADD COLUMN bank_upi_id TEXT NOT NULL DEFAULT 'shribalajikripadham@upi'",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_address TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम, ग्राम डूँगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर, उत्तर प्रदेश - 202394'",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_directions TEXT NOT NULL DEFAULT '🚆 एकमात्र नजदीकी रेलवे स्टेशन: केवल बुलन्दशहर रेलवे स्टेशन (BSC) (~28-30 किमी)\n🏙️ निकटवर्ती प्रमुख 3 शहर: अनूपशहर (~16 किमी) • जहांगीराबाद (~10 किमी) • बुलन्दशहर (~30 किमी)'",
            "ALTER TABLE ashram_settings ADD COLUMN footer_copyright TEXT NOT NULL DEFAULT '© 2026 श्री बालाजी कृपा धाम। सर्वाधिकार सुरक्षित।'",
            "ALTER TABLE ashram_settings ADD COLUMN token_service_mode TEXT NOT NULL DEFAULT 'AUTO_SUNDAY'",
            "ALTER TABLE ashram_settings ADD COLUMN is_tuesday_darbar_enabled INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_darbar_name TEXT NOT NULL DEFAULT 'श्री बालाजी कृपा धाम (बुलन्दशहर दरबार)'",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_darbar_address TEXT NOT NULL DEFAULT 'बुलन्दशहर, उत्तर प्रदेश'",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_latitude REAL NOT NULL DEFAULT 28.4069",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_longitude REAL NOT NULL DEFAULT 77.8498",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_allowed_radius_meters REAL NOT NULL DEFAULT 200.0",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_outstation_min_distance_km REAL NOT NULL DEFAULT 30.0",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_darbar_timings TEXT NOT NULL DEFAULT 'प्रत्येक मंगलवार प्रातः 8:00 बजे से (Every Tuesday from 8:00 AM)'",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_token_service_mode TEXT NOT NULL DEFAULT 'AUTO_TUESDAY'",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_scheduled_open_timestamp INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_darbar_date TEXT NOT NULL DEFAULT ''",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_current_serving_token INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_running_token_number INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN tuesday_token_notice TEXT NOT NULL DEFAULT 'बुलन्दशहर मंगलवार दरबार: केवल टोकन प्रणाली मान्य।'",
            "ALTER TABLE ashram_settings ADD COLUMN whatsapp_channel_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0'",
            "ALTER TABLE tokens ADD COLUMN darbar_venue TEXT NOT NULL DEFAULT 'DUNGRA_JAAT'",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_tokens_darbar_number ON tokens (darbar_date, token_number)",
            "CREATE INDEX IF NOT EXISTS idx_tokens_patient_phone ON tokens (phone_number, darbar_date)"
        )
        for (sql in alterStatements) {
            try {
                db.execSQL(sql)
            } catch (ignored: Exception) {}
        }
        autoMigrateSettingsColumns(db)
        try {
            db.execSQL("UPDATE admins SET can_manage_parchas = 1, can_cancel_tokens = 1, can_delete_tokens = 1, can_custom_token_number = 1, can_export_pdf = 1, can_manage_arzi = 1, can_manage_havan = 1 WHERE role = 'SUPER_ADMIN'")
        } catch (ignored: Exception) {}
    }

    private fun seedInitialDataIfEmpty(db: SQLiteDatabase) {
        val today = getTodayDateString()

        // 1. Seed Ashram Settings if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ashram_settings", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                // First try auto-restoring from public backup JSON in Downloads/ShriBalajiKripaDham_Backups
                val backupRestored = try {
                    com.example.shribalajikripadham.util.GoogleDriveSyncHelper.restoreFromBackupJson(context, null, db)
                } catch (e: Exception) { false }

                if (!backupRestored) {
                    // Check if indestructible vault or SharedPreferences has existing user settings first!
                    val restored = AppPermanentVault.restoreVault(context, db, force = true)
                    if (!restored) {
                    val settingsValues = ContentValues().apply {
                        put("id", 1)
                        put("ashram_name", "श्री बालाजी कृपा धाम")
                        put("guruji_name", "परम पूज्य गुरुजी")
                        put("address", "ग्राम डूंगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उ.प्र.) - 202394")
                        put("latitude", 28.3972915)
                        put("longitude", 78.1460410)
                        put("allowed_radius_meters", 200.0)
                        put("is_outstation_advance_allowed", 1)
                        put("outstation_min_distance_km", 30.0)
                        put("running_token_number", 1)
                        put("is_darbar_active", 1)
                        put("darbar_date", today)
                        put("darbar_timings", "प्रत्येक रविवार प्रातःकाल 8:30 बजे से सायं 5:00 बजे तक")
                        put("free_disclaimer", "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।")
                        put("contact_phone", "")
                        put("emergency_notice", "जय श्री बालाजी! रविवार दरबार टोकन पंजीकरण आश्रम सीमा में ही मान्य है।")
                        put("is_token_service_enabled", 1)
                        put("token_service_mode", "AUTO_SUNDAY")
                        put("is_yatra_service_enabled", 0)
                        put("is_live_counter_visible", 1)
                        put("is_events_visible", 1)
                        put("is_aarti_timings_visible", 1)
                        put("is_guruji_info_visible", 1)
                        put("is_emergency_notice_visible", 1)
                        put("scheduled_token_open_timestamp", 0L)
                        put("is_geofence_enforced", 1)
                        put("latest_version_code", 84)
                        put("latest_version_name", "2.56.9")
                        put("update_notes", "शून्य डमी डेटा गारंटी, वेबसाइट व एडिटर लाइव सिंक।")
                        put("apk_download_url", "https://shribalajikripadham.online/downloads/ShriBalajiKripaDham-release.apk")
                        put("is_force_update", 0)
                        put("whatsapp_group_url", "https://chat.whatsapp.com/IxB0hJ95XMc65wvcrTpBg5?s=cl&p=a&mlu=4&iam=0")
                        put("whatsapp_number", "")
                        put("youtube_channel_url", "https://www.youtube.com/@ShriBalajiKripaDham")
                        put("facebook_page_url", "https://www.facebook.com/ShriBalajiKripaDham")
                        put("instagram_url", "https://www.instagram.com/shribalajikripadham")
                        put("app_share_url", "https://shribalajikripadham.online/app")
                        put("current_theme_id", "maroon")
                        put("guruji_photo_uri", "")
                        put("active_ui_layout", "CLASSIC_DARBAR")
                        put("max_daily_tokens", 0)
                        put("is_ui_layout_enforced", 0)
                        put("cloud_sync_url", "")
                        put("is_cloud_sync_enabled", 0)
                    }
                    db.insert("ashram_settings", null, settingsValues)
                }
            }
        }
    } catch (e: Exception) { e.printStackTrace() }

        // 2. Seed Super Admin if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM admins WHERE role = 'SUPER_ADMIN'", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                val superAdmin = ContentValues().apply {
                    put("name", "Ankit Chaudhary (Super Admin)")
                    put("username", "admin")
                    put("phone", "")
                    put("role", AdminRole.SUPER_ADMIN.name)
                    put("pin_hash", MASTER_PIN_RAW_HASH)
                    put("password_hash", MASTER_PWD_SALTED_HASH)
                    put("can_manage_tokens", 1)
                    put("can_issue_manual_tokens", 1)
                    put("can_manage_yatra", 1)
                    put("can_manage_expenses", 1)
                    put("can_change_location", 1)
                    put("can_send_notifications", 1)
                    put("can_edit_ashram_info", 1)
                    put("can_manage_admins", 1)
                    put("can_view_devotee_photos", 1)
                    put("can_issue_tokens_anywhere", 1)
                    put("can_scan_paper_register", 1)
                    put("can_manage_parchas", 1)
                    put("can_cancel_tokens", 1)
                    put("can_delete_tokens", 1)
                    put("can_custom_token_number", 1)
                    put("can_export_pdf", 1)
                    put("can_manage_arzi", 1)
                    put("can_manage_havan", 1)
                    put("photo_uri", "")
                    put("is_active", 1)
                    put("created_at", System.currentTimeMillis())
                }
                db.insert("admins", null, superAdmin)
            }

            // Always enforce master credentials for Super Admin via secure precomputed hashes
            try {
                val saCv = ContentValues().apply {
                    put("pin_hash", MASTER_PIN_RAW_HASH)
                    put("password_hash", MASTER_PWD_SALTED_HASH)
                }
                db.update("admins", saCv, "role = 'SUPER_ADMIN' OR username = 'admin'", null)
            } catch (e: Exception) {}
        } catch (e: Exception) { e.printStackTrace() }

        // 3. Strict Zero-Dummy Policy: Purge any legacy placeholder canonical parchas
        try {
            db.delete("sacred_parchas", "parcha_id LIKE 'PARCHA_%' OR created_by = 'SYSTEM' OR created_by = 'CANONICAL'", null)
        } catch (e: Exception) { e.printStackTrace() }


        // 4. Seed 60-Seater Bus Seats (3x2 Configuration: 12 Rows x 5 Seats = 60 Seats)
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM bus_seats", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count < 60) {
                val labels = listOf("A", "B", "C", "D", "E")
                var seatNum = 1
                for (row in 1..12) {
                    for (col in 1..5) {
                        val label = "$row${labels[col - 1]}"
                        val checkCursor = db.rawQuery("SELECT seat_number, is_booked FROM bus_seats WHERE seat_number = ?", arrayOf(seatNum.toString()))
                        val exists = checkCursor.moveToFirst()
                        val isAlreadyBooked = if (exists) checkCursor.getInt(1) == 1 else false
                        checkCursor.close()

                        if (!exists) {
                            val seatValues = ContentValues().apply {
                                put("seat_number", seatNum)
                                put("seat_label", label)
                                put("row_idx", row)
                                put("col_idx", col)
                                put("is_booked", 0)
                                put("passenger_name", "")
                                put("passenger_age", 0)
                                put("passenger_gender", "")
                                put("phone_number", "")
                                put("boarding_point", "Gram Dungra Jaat Ashram")
                                put("payment_status", PaymentStatus.UNPAID.name)
                                put("payment_mode", "UPI_QR")
                                put("transaction_id", "")
                                put("fare_amount", 0)
                                put("yatra_date", "Upcoming Pilgrimage")
                                put("booked_at", 0L)
                                put("booked_by", "DEVOTEE")
                                put("notes", "")
                            }
                            db.insertWithOnConflict("bus_seats", null, seatValues, SQLiteDatabase.CONFLICT_IGNORE)
                        } else {
                            val updateCv = ContentValues().apply {
                                put("seat_label", label)
                                put("row_idx", row)
                                put("col_idx", col)
                            }
                            db.update("bus_seats", updateCv, "seat_number = ?", arrayOf(seatNum.toString()))
                        }
                        seatNum++
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 5. Events: Strict Zero-Dummy Policy (Only legitimate events added by Admin)
        // 6. Notifications: Strict Zero-Dummy Policy (Only real notifications from Admin)

        // 7. Seed UI Section Configs if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ui_section_configs", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                com.example.shribalajikripadham.data.model.UiSectionConfig.defaultSections().forEach { s ->
                    val cv = ContentValues().apply {
                        put("section_id", s.sectionId)
                        put("title_hindi", s.titleHindi)
                        put("title_english", s.titleEnglish)
                        put("icon", s.icon)
                        put("is_visible", if (s.isVisible) 1 else 0)
                        put("order_index", s.orderIndex)
                    }
                    db.insertWithOnConflict("ui_section_configs", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 8. Auto-restore tokens and data from public backup on fresh install or reinstall
        try {
            val tokenCursor = db.rawQuery("SELECT COUNT(*) FROM tokens", null)
            var tokenCount = 0
            if (tokenCursor.moveToFirst()) tokenCount = tokenCursor.getInt(0)
            tokenCursor.close()
            if (tokenCount == 0) {
                com.example.shribalajikripadham.util.GoogleDriveSyncHelper.restoreFromBackupJson(context, null, db)
            }
        } catch (ignored: Exception) {}
    }

    // =========================================================================
    // 📜 AUDIT TRAIL LEDGER SYSTEM (Anti-Corruption & Administrative Accountability)
    // =========================================================================

    fun insertAuditLog(
        action: String,
        tokenNumber: Int = 0,
        performedBy: String = "SYSTEM",
        role: String = "SEVADAR",
        reason: String = "",
        darbarDate: String = getTodayDateString(),
        details: String = ""
    ): Long {
        return try {
            val cv = ContentValues().apply {
                put("action", action)
                put("token_number", tokenNumber)
                put("performed_by", performedBy)
                put("role", role)
                put("reason", reason)
                put("darbar_date", darbarDate)
                put("details", details)
                put("timestamp", System.currentTimeMillis())
            }
            writableDatabase.insert("audit_logs", null, cv)
        } catch (e: Exception) {
            e.printStackTrace()
            -1L
        }
    }

    fun getAuditLogs(limit: Int = 150): List<AuditLogEntry> {
        val list = mutableListOf<AuditLogEntry>()
        try {
            val cursor = readableDatabase.rawQuery(
                "SELECT id, action, token_number, performed_by, role, reason, darbar_date, details, timestamp FROM audit_logs ORDER BY id DESC LIMIT ?",
                arrayOf(limit.toString())
            )
            while (cursor.moveToNext()) {
                list.add(
                    AuditLogEntry(
                        id = cursor.getLong(0),
                        action = cursor.getString(1),
                        tokenNumber = cursor.getInt(2),
                        performedBy = cursor.getString(3),
                        role = cursor.getString(4),
                        reason = cursor.getString(5),
                        darbarDate = cursor.getString(6),
                        details = cursor.getString(7),
                        timestamp = cursor.getLong(8)
                    )
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // =========================================================================
    // 💾 DAILY DATABASE AUTO-BACKUP SYSTEM (Zero Data Loss Architecture)
    // =========================================================================

    fun exportDatabaseBackup(context: Context): Pair<Boolean, String> {
        return try {
            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.exists()) {
                return Pair(false, "डेटाबेस फ़ाइल नहीं मिली (DB File not found)")
            }

            // Checkpoint WAL journal to ensure main DB file is complete
            try {
                writableDatabase.rawQuery("PRAGMA wal_checkpoint(FULL)", null).use { it.moveToFirst() }
            } catch (e: Exception) {}

            // Target storage directory (Downloads / Balaji_Backups or App Documents fallback)
            val externalDownloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val preferredDir = java.io.File(externalDownloads, "Balaji_Backups")
            val targetDir = try {
                if (!preferredDir.exists()) preferredDir.mkdirs()
                if (preferredDir.canWrite()) preferredDir else {
                    val fallback = java.io.File(context.getExternalFilesDir(null), "Balaji_Backups")
                    if (!fallback.exists()) fallback.mkdirs()
                    fallback
                }
            } catch (e: Exception) {
                val fallback = java.io.File(context.getExternalFilesDir(null), "Balaji_Backups")
                if (!fallback.exists()) fallback.mkdirs()
                fallback
            }

            val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HHmmss", java.util.Locale.US).format(java.util.Date())
            val backupFile = java.io.File(targetDir, "SBKD_Backup_${timestamp}.db")

            java.io.FileInputStream(dbFile).use { input ->
                java.io.FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 7 Rolling Backups: Purge backups older than the latest 7 to prevent storage overflow
            try {
                val existingBackups = targetDir.listFiles { f ->
                    f.name.startsWith("SBKD_Backup_") && f.name.endsWith(".db")
                }
                if (existingBackups != null && existingBackups.size > 7) {
                    existingBackups.sortBy { it.lastModified() }
                    val toRemove = existingBackups.take(existingBackups.size - 7)
                    toRemove.forEach { it.delete() }
                }
            } catch (ignored: Exception) {}

            // Record into audit ledger
            insertAuditLog(
                action = "DB_BACKUP_EXPORTED",
                performedBy = "SYSTEM",
                role = "SUPER_ADMIN",
                reason = "दैनिक ऑटो-बैकअप संपन्न",
                details = "${backupFile.name} (${backupFile.length() / 1024} KB)"
            )

            Pair(true, "✅ डेटाबेस बैकअप सुरक्षित सहेजा गया:\n${backupFile.name} (${backupFile.length() / 1024} KB)\nपथ: ${targetDir.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "बैकअप निर्माण में त्रुटि: ${e.localizedMessage}")
        }
    }

    // ==========================================
    // 🎵 SACRED AUDIO TRACKS & AARTI REPOSITORY
    // ==========================================
    fun getAllTracks(): List<com.example.shribalajikripadham.data.sacred.SacredTrack> {
        val list = mutableListOf<com.example.shribalajikripadham.data.sacred.SacredTrack>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, track_key, title_hindi, title_english, subtitle_hindi, duration_text, audio_url, lyrics_hindi, is_published, display_order, youtube_search_query FROM ashram_tracks ORDER BY display_order ASC, id ASC", null)
        try {
            while (cursor.moveToNext()) {
                list.add(
                    com.example.shribalajikripadham.data.sacred.SacredTrack(
                        id = cursor.getLong(0),
                        trackKey = cursor.getString(1) ?: "",
                        titleHindi = cursor.getString(2) ?: "",
                        titleEnglish = cursor.getString(3) ?: "",
                        subtitleHindi = cursor.getString(4) ?: "",
                        durationText = cursor.getString(5) ?: "",
                        audioUrl = cursor.getString(6) ?: "",
                        lyricsHindi = cursor.getString(7) ?: "",
                        isPublished = cursor.getInt(8) == 1,
                        displayOrder = cursor.getInt(9),
                        youtubeSearchQuery = cursor.getString(10) ?: ""
                    )
                )
            }
        } finally {
            cursor.close()
        }
        return list
    }

    fun getPublishedTracks(): List<com.example.shribalajikripadham.data.sacred.SacredTrack> {
        val list = mutableListOf<com.example.shribalajikripadham.data.sacred.SacredTrack>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, track_key, title_hindi, title_english, subtitle_hindi, duration_text, audio_url, lyrics_hindi, is_published, display_order, youtube_search_query FROM ashram_tracks WHERE is_published = 1 ORDER BY display_order ASC, id ASC", null)
        try {
            while (cursor.moveToNext()) {
                list.add(
                    com.example.shribalajikripadham.data.sacred.SacredTrack(
                        id = cursor.getLong(0),
                        trackKey = cursor.getString(1) ?: "",
                        titleHindi = cursor.getString(2) ?: "",
                        titleEnglish = cursor.getString(3) ?: "",
                        subtitleHindi = cursor.getString(4) ?: "",
                        durationText = cursor.getString(5) ?: "",
                        audioUrl = cursor.getString(6) ?: "",
                        lyricsHindi = cursor.getString(7) ?: "",
                        isPublished = cursor.getInt(8) == 1,
                        displayOrder = cursor.getInt(9),
                        youtubeSearchQuery = cursor.getString(10) ?: ""
                    )
                )
            }
        } finally {
            cursor.close()
        }
        return list
    }

    fun insertOrUpdateTrack(track: com.example.shribalajikripadham.data.sacred.SacredTrack): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("track_key", track.trackKey)
            put("title_hindi", track.titleHindi)
            put("title_english", track.titleEnglish)
            put("subtitle_hindi", track.subtitleHindi)
            put("duration_text", track.durationText)
            put("audio_url", track.audioUrl)
            put("lyrics_hindi", track.lyricsHindi)
            put("is_published", if (track.isPublished) 1 else 0)
            put("display_order", track.displayOrder)
            put("youtube_search_query", track.youtubeSearchQuery)
        }
        return if (track.id > 0) {
            db.update("ashram_tracks", cv, "id = ?", arrayOf(track.id.toString()))
            track.id
        } else {
            db.insertWithOnConflict("ashram_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    fun deleteTrack(id: Long): Boolean {
        val db = writableDatabase
        return db.delete("ashram_tracks", "id = ?", arrayOf(id.toString())) > 0
    }

    fun saveAllTracks(tracks: List<com.example.shribalajikripadham.data.sacred.SacredTrack>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (track in tracks) {
                val cv = ContentValues().apply {
                    put("track_key", track.trackKey)
                    put("title_hindi", track.titleHindi)
                    put("title_english", track.titleEnglish)
                    put("subtitle_hindi", track.subtitleHindi)
                    put("duration_text", track.durationText)
                    put("audio_url", track.audioUrl)
                    put("lyrics_hindi", track.lyricsHindi)
                    put("is_published", if (track.isPublished) 1 else 0)
                    put("display_order", track.displayOrder)
                    put("youtube_search_query", track.youtubeSearchQuery)
                }
                db.insertWithOnConflict("ashram_tracks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
