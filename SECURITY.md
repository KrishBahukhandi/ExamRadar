# Security Policy

## 🛡️ Supported Versions

ExamRadar India actively maintains and patches the following software versions:

| Component | Version | Supported |
|:---|:---|:---|
| ExamRadar Android App | 1.0.x | :white_check_mark: |
| ExamRadar Backend API | 1.0.x | :white_check_mark: |

---

## 🚨 Reporting a Vulnerability

The ExamRadar India team takes the security and integrity of our candidate services seriously. If you believe you have discovered a vulnerability, security flaw, or sensitive data exposure, please follow responsible disclosure:

1. **Do NOT open a public GitHub issue.**
2. Send an email to **security@examradar.in** (or contact the maintainers via GitHub private security advisories).
3. Include:
   - Type of issue (e.g., authentication bypass, privilege escalation, scraper SSRF, sensitive information disclosure).
   - Step-by-step reproduction instructions or a minimal proof of concept.
   - Affected component (Android client, backend API, database policies).
4. You will receive an acknowledgment within 48 hours, followed by updates on remediation progress.

---

## 🔐 Key Architectural Security Controls

1. **HMAC-SHA256 Signature Verification:**
   - The backend validates all candidate Supabase JWTs with full HMAC-SHA256 signature checks using the configured JWT secret.
   - Fallback to unverified tokens is strictly disabled in production, and administrative roles cannot be forged.
2. **Actuator Endpoint Lockdown:**
   - Only `/actuator/health` and `/actuator/info` are public.
   - All management, configuration, and environment actuator endpoints require validated `ADMIN` authority.
3. **Android Client Hardening:**
   - Auth tokens and refresh tokens are persisted exclusively using hardware-backed AES-256 GCM encryption via `EncryptedSharedPreferences` (Android Keystore).
   - R8 Proguard rules obfuscate data models and network classes in release builds.
   - Cleartext HTTP traffic is disabled in production builds.
4. **Database Row-Level Security (RLS):**
   - Candidate preferences, checklists, and device tokens are gated behind Postgres RLS policies bound to the Supabase authenticated `auth.uid()`.
5. **Polite Scraper Safeguards:**
   - Official portal crawlers run with strict backoff timeouts and only crawl publicly indexed recruitment notices, never attempting authenticated or private administrative sections.
