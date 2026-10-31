# ELEVEX PRO - ENTERPRISE OPERATIONS MANUAL & STANDARD OPERATING PROCEDURES (SOP)

**Document Reference:** `ELEVEX-SOP-2026-V5940`  
**Classification:** Confidential / Enterprise Industrial Standard  
**System Version:** Release v5.9.40 (Build 630)  
**Chief Architect & Supreme Administrator:** Ankit Chaudhary (+91-8533955333)  
**Cloud Infrastructure:** Hostinger NVMe SSD Enterprise Cluster (`https://shribalajikripadham.online`)  
**Client Framework:** Flutter Industrial Android Suite (Cross-Flavored MNC Architecture)

---

## 1. Executive Overview & System Architecture

EleveX Pro is a multi-tenant, Fortune 500-grade elevator maintenance, remote diagnostics, and smart fleet dispatch ecosystem. The platform enforces zero-trust security, real-time Haversine geolocation routing, and complete offline capability for safety-critical field operations.

```
┌────────────────────────────────────────────────────────────────────────┐
│               ANKIT ELEVEX PRO - 4-TIER UNIFIED ECOSYSTEM              │
├────────────────────┬────────────────────┬──────────────────────────────┤
│ 🛡️ SUPER ADMIN      │ 📡 SUPERVISOR      │ 🔧 FIELD TECHNICIAN          │
│ • Ghost Mode       │ • Live Fleet Radar │ • 1,012+ Offline Fault Codes │
│ • Company Creation │ • Auto-Dispatch    │ • 150m Geofence Lock         │
│ • Ledger Audit     │ • SLA Tracking     │ • Customer OTP Verification  │
├────────────────────┴────────────────────┴──────────────────────────────┤
│ 🏢 LIFT OWNER / RWA CLIENT PORTAL                                      │
│ • 1-Tap Emergency SOS • Live Technician ETA Radar • 6-Digit OTP Release │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Tier 1: Super Admin Standard Operating Procedure

### 2.1 Authorized Credentials & Authentication
- **Supreme Administrator:** Ankit Chaudhary
- **Primary Registered Mobile:** `8533955333` / `+918533955333`
- **Default Master MPIN:** `1234` (Bcrypt secured)
- **Supreme Session Bypass Token:** `ELEVE_X_SUPREME_MASTER_SESSION_2026`
- **Access Endpoints:**
  * Desktop Corporate Portal: `https://shribalajikripadham.online/portal`
  * Android Release App: `download.php?app=superadmin`

### 2.2 Ghost Mode Operations
Ghost Mode grants silent, supreme oversight across all tenant companies without altering tenant audit ledgers or notifying operational supervisors:
1. Open the Corporate Desktop Portal or Super Admin App.
2. Select **"🛡️ Ghost Switcher"** in the top navigation header.
3. Choose the target tenant (e.g., `tenant_apexelevatorsncr_5779`, `tenant_horizon_vertical_mobility`, `tenant_shree_balaji_lift_eng`).
4. The system triggers `api_auth.php?action=ghost_switch_tenant`, dynamically hot-swapping branding, primary palette, active lifts, breakdown queues, and technician telemetry without page reload.
5. **Silent Ticket Reassignment (`ghost_reassign`):** If a breakdown is mishandled, the Super Admin can reassign it directly via `api_dispatch.php?action=ghost_reassign`. The ticket updates silently with `is_ghost_override = 1`.

### 2.3 Company Provisioning & Onboarding
To provision a new elevator company into the ecosystem:
1. Navigate to **Company Management** -> **Create New Company**.
2. Supply required enterprise metadata:
   - **Legal Company Name:** e.g., *Apex Elevators NCR*
   - **White-Label Brand Display Name:** e.g., *Apex EleveX Pro*
   - **24x7 Emergency Support Hotline:** e.g., `9811001122`
   - **Corporate Office Address & GPS Coordinates:** e.g., *Pari Chowk, Greater Noida* (`28.4744000, 77.5040000`)
   - **Brand Primary Color Hex:** e.g., `#0284C7`
3. The server generates a unique `tenant_id` (e.g., `tenant_apexelevatorsncr_5779`) and provisions an initial company administrator.

### 2.4 Dynamic Feature Flags
Feature flags dictate app capabilities per company:
- `door_drive_specialist`: Enables integrated door drive calibration tools (Panasonic, Fermator, Wittur).
- `offline_diagnostics`: Authorizes offline SQLite caching of 1,012+ OEM fault codes.
- `anti_spoof_gps`: Blocks mock locations and requires genuine hardware coordinates.
- `auto_dispatch`: Activates smart Haversine nearest-technician automated dispatch.
- `kiosk_lock`: Prevents technician terminal minimization during active duty shifts.
- `power_telemetry`: Tracks battery drain and forced device power-off incidents.

### 2.5 Immutable Attendance Ledger Auditing
The attendance ledger is write-once and locked by default (`is_locked_immutable = 1`). In cases of legitimate on-site disputes:
1. Super Admin navigates to the **Attendance & Audit Ledger Viewer**.
2. Clicks **"✏️ Audit Seal"** on the disputed technician record.
3. Supplies the legally adjusted punch-in timestamp and a mandatory rationale (minimum 5 characters, e.g., *"SuperAdmin Ankit Chaudhary certified on-site emergency maintenance audit"*).
4. System executes `api_telemetry.php?action=superadmin_edit_attendance`, affixing an immutable audit seal with timestamp and administrator ID.

---

## 3. Tier 2: Company Supervisor Cockpit SOP

### 3.1 Morning Shift Activation & Fleet Rollcall
1. Login with assigned supervisor mobile (e.g., `9811002233` for Apex Elevators, `9812002233` for Horizon, `9813002233` for Balaji) and MPIN `1234`.
2. Inspect the **Live Technician Fleet Radar**:
   - Verify technician status badges:
     * `🟢 Live / Online`: Device heartbeat received within last 10 minutes.
     * `📴 Switched Off by Tech`: Technician manually initiated device shutdown.
     * `🪫 Phone Battery Drained`: Device reached critical battery depletion (<5%).
     * `⚠️ Signal Lost / Offline`: Telemetry gap exceeding 10 minutes.
3. Review OpenStreetMap dynamic landmark addresses for current technician locations.

### 3.2 Smart Haversine Dispatch vs. Manual Override
- **Automated Mode:** When a breakdown is logged with `auto_dispatch = 1`, the server calculates the distance between the reported lift coordinates and all active duty technicians:
  $$d = 2R \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta \varphi}{2}\right) + \cos(\varphi_1)\cos(\varphi_2)\sin^2\left(\frac{\Delta \lambda}{2}\right)}\right)$$
  The closest technician within operational range is automatically assigned, triggering an emergency siren push notification via OneSignal.
- **Manual Mode:** From the Kanban board **Pending** column, select an available technician from the dropdown and click **"डिस्पैच"**.

### 3.3 Ad-Hoc Non-AMC Emergency Dispatch
In life-safety emergencies involving non-contracted lifts:
1. Click **"+ नया ब्रेकडाउन"** -> Check **"Non-AMC Emergency Site"**.
2. Enter the building name, address, nearest landmark, and GPS coordinates.
3. The system generates an immediate ticket (`BD-YYYYMMDD-XXXX`), sets 24-hour temporary validity, and alerts the nearest technician immediately.

---

## 4. Tier 3: Field Technician Terminal SOP

### 4.1 Shift Punch-In Protocol & Anti-Mock GPS Lock
1. Launch **EleveX Technician Terminal** (`com.ankit.elevex.technician`).
2. Login with registered technician phone and MPIN `1234`.
3. Press **"हाज़िरी दर्ज करें (Punch In)"**:
   - The device checks hardware GPS providers.
   - **Anti-Mock Verification:** If mock location providers, developer simulation flags, or emulator environments are detected (`is_mock = true`), the punch-in is immediately rejected with HTTP 403.
   - Genuine punch-in captures latitude, longitude, and reverse-geocoded OpenStreetMap landmark, locking the record immutably.

### 4.2 Tamper Detection & Terminal Freezing
The technician terminal includes native runtime tamper detection:
- Enabling Developer Options, USB Debugging, or executing `su` binary triggers `api_telemetry.php?action=log_tamper_alert`.
- The terminal freezes into **TAMPER_LOCKED** state, flags the attendance ledger, and requires Super Admin intervention to unlock.

### 4.3 Offline Fault Code Diagnostics (1,012+ OEM Codes)
When working in basements, elevator shafts, or areas without network connectivity:
1. Open **"ऑफलाइन फॉल्ट कोड डायरेक्टरी"**.
2. Select controller brand:
   - **Monarch:** NICE3000+, NICE1000+, NICE9000
   - **STEP:** AS380, AS320, STEP-iAStar
   - **Otis:** SKY, Gen2, MCS-220, OVF20
   - **Schindler:** 3300, 5500, Bionic 5, MICONIC 10
   - **KONE:** MonoSpace, MiniSpace, LCECPU40 / LCE230
3. Enter error code (e.g., `Err02`, `E23`, `0100`, `0021`) or describe symptom in Hindi or English (e.g., *"Overcurrent"*, *"अतिप्रवाह"*, *"Door Lock Sensor"*, *"सेफ्टी चेन"*).
4. The local SQLite/in-memory database returns:
   - Root technical cause
   - Step-by-step diagnostic sequence
   - Controller PCB terminal verification pinouts
   - Permanent remedial action

### 4.4 On-Site Breakdown Lifecycle & Customer OTP Geofence Lock
1. **Accept Dispatch:** When a ticket is assigned, tap **"साइट के लिए रवाना (En Route)"**.
2. **Arrive On-Site:** Upon reaching the building, tap **"साइट पर पहुंचे (On-Site)"**.
3. **Execute Diagnostics & Repair:** Follow OEM diagnostic protocols.
4. **Resolution Verification (150m Geofence Lock):**
   - Technician MUST be physically within **150 meters** of the registered lift site coordinates.
   - Request the 6-digit cryptographic completion OTP from the Lift Owner or RWA manager.
   - Enter OTP into the terminal and tap **"कार्य संपन्न (Complete Breakdown)"**.
   - If distance exceeds 150 meters, the server returns HTTP 403: *"जियोफेंस सुरक्षा लॉक: आप लिफ्ट साइट से दूर हैं। कृपया 150 मीटर के दायरे में आकर OTP दर्ज करें।"*
   - Once validated, the ticket status changes to `resolved`, and the customer is prompted for rating.

---

## 5. Tier 4: Lift Owner & RWA Client SOP

### 5.1 Emergency SOS Breakdown Reporting
1. Launch **EleveX Lift Owner App** (`com.ankit.elevex.owner`).
2. Login with registered phone (e.g., `9811006677`).
3. Registered lifts appear with building name, controller brand, and active AMC validity.
4. Tap **"🚨 आपातकालीन ब्रेकडाउन SOS"**.
5. The system issues a unique ticket number, notifies the control room, and dispatches the nearest technician.

### 5.2 Live Technician Radar Tracking & ETA
- The app displays real-time radar tracking showing technician distance in meters/kilometers and live arrival ETA in minutes.
- Live landmark text confirms the technician's current progress toward the site.

### 5.3 6-Digit Completion OTP & Service Rating
- When a ticket is created, the Owner App displays a secure 6-digit code (e.g., `482915`).
- **Do not share this code over the phone.** Only provide this code to the technician in person after physically inspecting and confirming the elevator has been restored to safe operation.
- After completion, submit a 1 to 5 star rating and service feedback, which feeds into the company SLA leaderboard.

---

## 6. Enterprise Master Data Directory

### 6.1 Provisioned Companies
| Tenant ID | Company Name | Brand Display | Hotline | Headquarters | Primary Color |
|---|---|---|---|---|---|
| `tenant_master_elevex` | Ankit EleveX Supreme Command | EleveX Pro International | 8533955333 | Sector Alpha 1, Greater Noida | `#8B5CF6` |
| `tenant_apexelevatorsncr_5779` | Apex Elevators NCR | Apex EleveX Pro | 9811001122 | Commercial Belt, Pari Chowk | `#0284C7` |
| `tenant_horizon_vertical_mobility` | Horizon Vertical Mobility | Horizon Elevators Delhi NCR | 9812001122 | Statesman House, Connaught Place | `#0EA5E9` |
| `tenant_shree_balaji_lift_eng` | Shree Balaji Lift Engineering Services | Shree Balaji Lift Services Meerut | 9813001122 | Delhi Road, Transport Nagar, Meerut | `#F59E0B` |

### 6.2 AMC Lift Sites Inventory
| Unique Code | Building Name | Landmark | GPS Coordinates | Radius | Controller OEM | AMC Type |
|---|---|---|---|---|---|---|
| `APEX-HORIZON-01` | Horizon Grand Tower A | Pari Chowk Metro Pillar 42 | `28.4744000, 77.5040000` | 150m | Monarch NICE3000+ | Comprehensive Platinum |
| `APEX-ALPHA-02` | Alpha Plaza Commercial | Opposite Pari Chowk Roundabout | `28.4735000, 77.5032000` | 150m | STEP AS380 | Comprehensive Gold |
| `HORIZON-CP-01` | Connaught House Tower | Near Rajiv Chowk Metro Gate 7 | `28.6328000, 77.2197000` | 150m | Otis SKY | Comprehensive Diamond |
| `HORIZON-GUR-02` | Cyber Horizon Corporate | Cyber Hub Rapid Metro Station | `28.4906000, 77.0888000` | 150m | Schindler 3300 | Comprehensive Enterprise |
| `BALAJI-GAUR-01` | Gaur City Commercial Mall | Char Murti / Kisan Chowk | `28.6082000, 77.4244000` | 150m | KONE MonoSpace | Comprehensive Gold |
| `BALAJI-MEERUT-02` | Rithani Central Arcade | Near Rithani RRTS Station | `28.9482000, 77.6712000` | 150m | Monarch NICE3000+ | Comprehensive Platinum |

---

## 7. APK Packaging & Distribution Verification

All 5 release APK binaries are packaged, SHA-256 hashed, and served directly via the high-speed CDN download endpoint:

- **SuperAdmin App:** `https://shribalajikripadham.online/download.php?app=superadmin`
- **Supervisor Cockpit:** `https://shribalajikripadham.online/download.php?app=supervisor`
- **Technician Terminal:** `https://shribalajikripadham.online/download.php?app=technician`
- **Lift Owner App:** `https://shribalajikripadham.online/download.php?app=owner`
- **Master Gateway:** `https://shribalajikripadham.online/download.php?app=gateway`

---

*EleveX Pro • Engineered with Zero Compromise for Life-Safety Industrial Vertical Mobility.*
