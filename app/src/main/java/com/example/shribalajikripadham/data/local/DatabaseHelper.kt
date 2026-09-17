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
        const val DATABASE_VERSION = 23

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun hashPassword(password: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val salted = "SBKD_SALT_2026_$password"
            val digest = md.digest(salted.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }

        fun getTodayDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        ensureAllTablesExist(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        ensureAllTablesExist(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
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
                    whatsapp_group_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/invite',
                    whatsapp_number TEXT NOT NULL DEFAULT '+919876543210',
                    youtube_channel_url TEXT NOT NULL DEFAULT 'https://www.youtube.com/@ShriBalajiKripaDham',
                    facebook_page_url TEXT NOT NULL DEFAULT 'https://www.facebook.com/ShriBalajiKripaDham',
                    instagram_url TEXT NOT NULL DEFAULT 'https://www.instagram.com/shribalajikripadham',
                    app_share_url TEXT NOT NULL DEFAULT 'https://shribalajikripadham.org/app',
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
                    allow_admin_reserved_tokens INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
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

        // Ensure missing columns in existing tables
        ensureColumns(db)

        // Seed data if missing
        seedInitialDataIfEmpty(db)

        // Always enforce the latest Super Admin password hash
        try {
            val superAdminHash = hashPassword("9100100251233433")
            db.execSQL("UPDATE admins SET password_hash = ? WHERE role = 'SUPER_ADMIN'", arrayOf(superAdminHash))
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun ensureColumns(db: SQLiteDatabase) {
        val alterStatements = listOf(
            "ALTER TABLE active_device_telemetry ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'",
            "ALTER TABLE ashram_settings ADD COLUMN is_aarti_timings_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN is_guruji_info_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN is_emergency_notice_visible INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN scheduled_token_open_timestamp INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN whatsapp_group_url TEXT NOT NULL DEFAULT 'https://chat.whatsapp.com/invite'",
            "ALTER TABLE ashram_settings ADD COLUMN whatsapp_number TEXT NOT NULL DEFAULT '+919876543210'",
            "ALTER TABLE ashram_settings ADD COLUMN youtube_channel_url TEXT NOT NULL DEFAULT 'https://www.youtube.com/@ShriBalajiKripaDham'",
            "ALTER TABLE ashram_settings ADD COLUMN facebook_page_url TEXT NOT NULL DEFAULT 'https://www.facebook.com/ShriBalajiKripaDham'",
            "ALTER TABLE ashram_settings ADD COLUMN instagram_url TEXT NOT NULL DEFAULT 'https://www.instagram.com/shribalajikripadham'",
            "ALTER TABLE ashram_settings ADD COLUMN app_share_url TEXT NOT NULL DEFAULT 'https://shribalajikripadham.org/app'",
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
            "ALTER TABLE ashram_settings ADD COLUMN is_payment_feature_live INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN can_admin_view_payment_history INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN can_devotee_view_payment_history INTEGER NOT NULL DEFAULT 0",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_upi_id TEXT NOT NULL DEFAULT 'shribalajikripadham@upi'",
            "ALTER TABLE ashram_settings ADD COLUMN ashram_upi_name TEXT NOT NULL DEFAULT 'Shri Balaji Kripa Dham'",
            "ALTER TABLE ashram_settings ADD COLUMN bus_seat_fare_amount INTEGER NOT NULL DEFAULT 1500",
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
            "ALTER TABLE ui_section_configs ADD COLUMN target_audience TEXT NOT NULL DEFAULT 'ALL'",
            "ALTER TABLE ashram_settings ADD COLUMN is_outstation_advance_allowed INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE ashram_settings ADD COLUMN outstation_min_distance_km REAL NOT NULL DEFAULT 30.0",
            "ALTER TABLE ashram_settings ADD COLUMN allow_admin_reserved_tokens INTEGER NOT NULL DEFAULT 0",
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_tokens_darbar_number ON tokens (darbar_date, token_number)",
            "CREATE INDEX IF NOT EXISTS idx_tokens_patient_phone ON tokens (phone_number, darbar_date)"
        )
        for (sql in alterStatements) {
            try {
                db.execSQL(sql)
            } catch (ignored: Exception) {}
        }
        try {
            db.execSQL("UPDATE admins SET can_manage_parchas = 1, can_cancel_tokens = 1, can_delete_tokens = 1, can_custom_token_number = 1, can_export_pdf = 1, can_manage_arzi = 1 WHERE role = 'SUPER_ADMIN'")
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
                val settingsValues = ContentValues().apply {
                    put("id", 1)
                    put("ashram_name", "श्री बालाजी कृपा धाम")
                    put("guruji_name", "परम पूज्य गुरुजी")
                    put("address", "ग्राम डूंगरा जाट, तहसील अनूपशहर, जिला बुलन्दशहर (उ.प्र.)")
                    put("latitude", 28.3972915)
                    put("longitude", 78.1460410)
                    put("allowed_radius_meters", 200.0)
                    put("is_outstation_advance_allowed", 1)
                    put("outstation_min_distance_km", 30.0)
                    put("running_token_number", 1)
                    put("is_darbar_active", 1)
                    put("darbar_date", today)
                    put("darbar_timings", "प्रत्येक रविवार प्रातः 7:00 बजे से (Every Sunday from 7:00 AM)")
                    put("free_disclaimer", "भूत-प्रेत व मानसिक समस्याओं का पूर्णतः निःशुल्क (FREE) इलाज। कोई शुल्क अथवा दक्षिणा नहीं ली जाती।")
                    put("contact_phone", "+91 98765 00000")
                    put("emergency_notice", "जय श्री बालाजी! रविवार दरबार टोकन पंजीकरण आश्रम सीमा में ही मान्य है।")
                    put("is_token_service_enabled", 1)
                    put("is_yatra_service_enabled", 0)
                    put("is_live_counter_visible", 1)
                    put("is_events_visible", 1)
                    put("is_aarti_timings_visible", 1)
                    put("is_guruji_info_visible", 1)
                    put("is_emergency_notice_visible", 1)
                    put("scheduled_token_open_timestamp", 0L)
                    put("is_geofence_enforced", 1)
                    put("latest_version_code", 18)
                    put("latest_version_name", "2.15.0")
                    put("update_notes", "नया अपडेट v2.15.0: सुपर एडमिन आश्रम पर्चा नियंत्रण, सेवादार एक्सेस डेलिगेशन एवं क्लाउड आधारित डायनामिक लोकेशन सिंक।")
                    put("apk_download_url", "https://github.com/ankit261194/shri-balaji-kripa-dham/releases/download/v2.15.0/ShriBalajiKripaDham-release.apk")
                    put("is_force_update", 0)
                    put("whatsapp_group_url", "https://chat.whatsapp.com/invite")
                    put("whatsapp_number", "+919876543210")
                    put("youtube_channel_url", "https://www.youtube.com/@ShriBalajiKripaDham")
                    put("facebook_page_url", "https://www.facebook.com/ShriBalajiKripaDham")
                    put("instagram_url", "https://www.instagram.com/shribalajikripadham")
                    put("app_share_url", "https://shribalajikripadham.org/app")
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
                    put("phone", "+91 98765 00000")
                    put("role", AdminRole.SUPER_ADMIN.name)
                    put("pin_hash", hashPin("0825"))
                    put("password_hash", hashPassword("9100100251233433"))
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
                    put("photo_uri", "")
                    put("is_active", 1)
                    put("created_at", System.currentTimeMillis())
                }
                db.insert("admins", null, superAdmin)
            }

            // Always enforce master PIN 0825 for Super Admin
            try {
                val newPinHash = hashPin("0825")
                val saCv = ContentValues().apply {
                    put("pin_hash", newPinHash)
                }
                db.update("admins", saCv, "role = 'SUPER_ADMIN' OR username = 'admin'", null)
            } catch (e: Exception) {}

            // Automatic restoration from permanent vault
            try {
                AppPermanentVault.restoreVault(context, db)
            } catch (e: Exception) {}
        } catch (e: Exception) { e.printStackTrace() }

        // 3. Seed Canonical Sacred Parchas if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM sacred_parchas", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                val canonicals = com.example.shribalajikripadham.ai.SacredParchaEngine.getCanonicalParchas()
                for (parcha in canonicals) {
                    val cv = ContentValues().apply {
                        put("parcha_id", parcha.parchaId)
                        put("title", parcha.title)
                        put("category", parcha.category.name)
                        put("subtitle", parcha.subtitle)
                        put("samagri_list", parcha.samagriListToJson())
                        put("vidhi_text", parcha.vidhiStepsToJson())
                        put("precautions", parcha.precautionsToJson())
                        put("mantra_text", parcha.mantraText)
                        put("image_uri", parcha.imageUri)
                        put("is_published", if (parcha.isPublished) 1 else 0)
                        put("is_hidden", if (parcha.isHidden) 1 else 0)
                        put("view_count", parcha.viewCount)
                        put("download_count", parcha.downloadCount)
                        put("created_by", parcha.createdBy)
                        put("created_at", parcha.createdAt)
                        put("updated_at", System.currentTimeMillis())
                    }
                    db.insertWithOnConflict("sacred_parchas", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                }
            }
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
                                put("fare_amount", 1500)
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

        // 5. Seed Events if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ashram_events", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                val event1 = ContentValues().apply {
                    put("title_hindi", "वार्षिक महोत्सव: श्री गुरु पूर्णिमा")
                    put("title_english", "Annual Mahotsav: Guru Purnima")
                    put("date_desc_hindi", "आषाढ़ पूर्णिमा (जुलाई) - प्रातः 6 बजे से")
                    put("date_desc_english", "Ashadha Purnima (July) - 6:00 AM Onwards")
                    put("details_hindi", "विशाल भंडारा, गुरु पूजा, अखंड संकीर्तन एवं विशेष आशीर्वाद दरबार। हजारों भक्तों हेतु प्रसादम की व्यवस्था।")
                    put("details_english", "Grand Bhandara, Guru Puja, Akhand Sankirtan & Special Darbar with prasad distribution for thousands of devotees.")
                    put("is_active", 1)
                    put("created_at", System.currentTimeMillis())
                }
                db.insert("ashram_events", null, event1)

                val event2 = ContentValues().apply {
                    put("title_hindi", "महा जन्मोत्सव: श्री हनुमान जयंती")
                    put("title_english", "Maha Janmotsav: Hanuman Jayanti")
                    put("date_desc_hindi", "चैत्र पूर्णिमा (अप्रैल) - दिनभर")
                    put("date_desc_english", "Chaitra Purnima (April) - Full Day")
                    put("details_hindi", "श्री सुंदरकांड पाठ, सिंदूर अर्पण, महाआरती, चोला अर्पण एवं सामूहिक महाप्रसादम।")
                    put("details_english", "Sundarkand Path, Sindoor Arpan, Maha Aarti, Chola offering and community Mahaprasadam.")
                    put("is_active", 1)
                    put("created_at", System.currentTimeMillis())
                }
                db.insert("ashram_events", null, event2)
            }
        } catch (e: Exception) { e.printStackTrace() }

        // 6. Seed Initial Notification if not exists
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM app_notifications", null)
            var count = 0
            if (cursor.moveToFirst()) count = cursor.getInt(0)
            cursor.close()

            if (count == 0) {
                val initialNotif = ContentValues().apply {
                    put("title", "जय श्री बालाजी महाराज")
                    put("message", "आगामी रविवार को ग्राम डूँगरा जाट में निःशुल्क दरबार लगेगा। समय: प्रातः 7:00 बजे।")
                    put("priority", "HIGH")
                    put("sent_by", "Super Admin")
                    put("timestamp", System.currentTimeMillis())
                    put("is_read", 0)
                }
                db.insert("app_notifications", null, initialNotif)
            }
        } catch (e: Exception) { e.printStackTrace() }

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
    }
}
