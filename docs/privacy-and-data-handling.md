# ExamRadar India — Privacy Policy & Data Handling Guidelines

## 1. Data Minimization Principles

ExamRadar India strictly adheres to the principle of data minimization:

1. **No Sensitive PII:** The application **never** asks for, collects, stores, or processes:
   - Aadhaar numbers or government identity cards.
   - Government examination portal passwords or login credentials.
   - Banking information, UPI IDs, or credit/debit card details.
   - Scanned identity proofs, candidate photographs, or signature uploads.
2. **Anonymous / Guest Browsing:** Candidates can browse all official examination notices, deadlines, eligibility summaries, and calendar events without creating an account or logging in.
3. **Optional Profile Data:** Profile fields (such as target exam categories, education level, and graduation status) are strictly optional and are used solely to filter notifications and match eligibility criteria.

---

## 2. Personal Data Collected & Purpose

| Field | Necessity | Purpose |
| :--- | :--- | :--- |
| **Email Address** | Required for registered accounts | Authentication, password reset, and account recovery via Supabase Auth. |
| **Exam Categories Followed** | Optional | Personalizing the home feed and filtering relevant alerts. |
| **Education Level & Year** | Optional | Assessing candidate eligibility against official recruitment rules. |
| **Device Push Token** | Required for alerts | Delivering deadline reminders and critical notification changes via FCM. |
| **Application Checklist Items** | Optional | Private user progress tracker stored exclusively for the candidate. |

---

## 3. Account Deletion & Right to be Forgotten

Candidates can permanently delete their account at any time directly within the app:

1. Navigate to **Account Settings > Delete Account**.
2. Upon confirmation, the application executes a cascade deletion:
   - User profile record is deleted.
   - User examination preferences are removed.
   - Notification preferences and registered device tokens are deleted.
   - Private application checklists are purged.
   - Authentication record in Supabase Auth is revoked.
3. No personal identifiable information is retained in operational backups post-deletion.

---

## 4. Official Notice Citations & Independence

ExamRadar India is an independent informational tool:
- Every recruitment card displays the official publishing authority (e.g., UPSC, SSC, IBPS).
- Every card includes a direct link to the official original public PDF or announcement portal.
- Information freshness indicators display the exact timestamp when the official board was last inspected.
