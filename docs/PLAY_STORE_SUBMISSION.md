# ExamRadar — Google Play Store Submission Package & Checklist

> Complete reference guide for publishing ExamRadar on the Google Play Console.

---

## 1. App Store Listing Metadata

### App Details
* **App Name:** `ExamRadar — Govt Exam Alerts & Deadlines` (42 / 50 chars)
* **Package Name:** `in.examradar.app`
* **Default Language:** English (India) — `en-IN`

### Short Description (Max 80 characters)
```text
Official notifications, application deadlines & results for UPSC, SSC & Banking.
```
*(79 / 80 characters)*

### Full Description (Markdown for Reference, Plaintext for Play Console)
```text
Never miss a government exam deadline again. ExamRadar monitors official commission portals directly—delivering verified notifications, application deadlines, admit card releases, and results straight to your phone.

WHY ASPIRANTS TRUST EXAMRADAR:

✓ 100% Official & Gazette Sources
We ingest directly from commission portals including UPSC, SSC, and IBPS. No clickbait, no unverified rumours.

✓ Zero Missed Deadlines
Get targeted alerts 7 days, 3 days, and 1 day before application windows close. Stay ahead of time for fee payment and correction windows.

✓ Personalized Radar Dashboard
Select your target exams (Civil Services, CGL, CHSL, PO, Clerk, Regulatory) and receive alerts curated specifically to your goals and graduation status.

✓ Application Preparation Checklist
Keep track of critical documents (OBC/EWS/SC/ST certificates, photograph specifications, fee receipts) with offline interactive checklists for each exam.

✓ Verified Direct Links & PDF View
Open official notifications and commission portals directly with verified HTTPS links.

✓ Privacy & Security First
No invasive ad tracking. Your preferences and data are securely stored and encrypted on your device.

EXAMS CURRENTLY COVERED:
• UPSC: Civil Services (CSE), Engineering Services (ESE), NDA, CDS
• Staff Selection Commission: SSC CGL, SSC CHSL, SSC CPO, SSC MTS, GD Constable
• Banking: IBPS PO, IBPS Clerk, SBI PO, SBI Clerk, RRB Officer & Assistant
• Regulatory & State Commissions: RBI Grade B, SEBI, NABARD, and upcoming state PCS boards.

Download ExamRadar today and give your preparation the punctuality it deserves.
```

---

## 2. Graphic Assets Specifications

| Asset | Dimensions | Format | Requirements |
|---|---|---|---|
| **App Icon** | 512 × 512 px | 32-bit PNG | Up to 1 MB, no transparent background, flat edges (Play applies squircle) |
| **Feature Graphic** | 1024 × 500 px | JPEG or 24-bit PNG | Up to 1 MB, center-aligned logo + tagline, no alpha |
| **Phone Screenshots** | Min 1080 × 1920 px (or 1080 × 2400) | PNG/JPEG | Min 2, max 8 screenshots (Home Dashboard, Notice Detail, Checklist, Discovery) |

---

## 3. Play Store Policy & Data Safety Declarations

### Privacy Policy URL
`https://app.examradar.in/privacy-policy` *(or hosted on GitHub Pages: `https://krishbahukhandi.github.io/ExamRadar/privacy`)*

### Data Safety Form (Google Play Console)
1. **Does your app collect or share user data?** Yes
2. **Is all user data encrypted in transit?** Yes (HTTPS / TLS 1.3 enforced)
3. **Can users request data deletion?** Yes (Account screen provides single-tap complete data reset)
4. **Data Types Collected:**
   * **Personal info:** Email address & Full Name (optional sign-in for cross-device sync)
   * **App info and performance:** Crash logs (if analytics configured)
   * **Device or other IDs:** Device notification token (FCM for push alerts)
5. **Data Collection Purpose:** App functionality & account management only. **No data is shared with third parties or sold for advertising.**

### Content Rating Questionnaire
* **Category:** Reference / News & Education
* **Violence, Sexual Content, Controlled Substances:** No
* **User-to-User Communication:** No (broadcast alerts only)
* **Shares Physical Location:** No
* **Allows purchases of digital goods:** No
* **Expected Rating:** PEGI 3 / Everyone

### Target Audience & Content
* **Target Age Groups:** 18 and older
* **Is the app designed for children?** No

---

## 4. Keystore Generation & Release Build

### Generate Release Keystore
```bash
keytool -genkeypair -v \
  -keystore examradar-release.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias examradar \
  -storetype PKCS12
```

### Configure `apps/android/local.properties`
```properties
RELEASE_STORE_FILE=/path/to/examradar-release.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=examradar
RELEASE_KEY_PASSWORD=your_key_password
```

### Build Production Signed AAB
```bash
cd apps/android
./gradlew bundleRelease
```
The output bundle will be located at:
`apps/android/app/build/outputs/bundle/release/app-release.aab`

---

## 5. Google Play App Signing & Digital Asset Links Verification

1. In the **Google Play Console**, go to **Release > Setup > App Integrity**.
2. Copy the **SHA-256 certificate fingerprint** of the **App signing key**.
3. Update `services/backend/src/main/resources/static/.well-known/assetlinks.json` with this fingerprint:
   ```json
   [
     {
       "relation": ["delegate_permission/common.handle_all_urls"],
       "target": {
         "namespace": "android_app",
         "package_name": "in.examradar.app",
         "sha256_cert_fingerprints": [
           "YOUR_PLAY_STORE_SHA256_FINGERPRINT"
         ]
       }
     }
   ]
   ```
4. Verify using the Google Digital Asset Links Statement List tool:
   ```bash
   curl https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://app.examradar.in&relation=delegate_permission/common.handle_all_urls
   ```
