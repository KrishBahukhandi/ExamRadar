# ExamRadar India — Deployment & Operations Guide

## 1. Zero-Recurring-Cost Strategy (Mode A)

ExamRadar India is engineered to run completely within free-tier allowances:

| Component | Provider & Tier | Cost | Inactivity / Free-Tier Constraints |
| :--- | :--- | :--- | :--- |
| **Database & Auth** | Supabase Free Tier | ₹0.00 | Pauses after 7 days of inactivity (auto-resumed by traffic). 500MB DB storage. |
| **Push Notifications** | Firebase Cloud Messaging (FCM) | ₹0.00 | Generous free tier (billions of push messages/month). |
| **Ingestion Collector** | GitHub Actions Scheduled Workflow | ₹0.00 | Free 2,000 Linux runner minutes/month. Cron jobs have soft trigger times. |
| **Mobile Distribution** | Google Play Console | $25 (one-time) | One-time developer registration fee. |

---

## 2. Mode A Setup Step-by-Step

### Step 1: Supabase Free Project Setup
1. Create a free account at [supabase.com](https://supabase.com).
2. Create a new project (e.g., `examradar-india`) in the nearest region (e.g., `ap-south-1` Mumbai).
3. Navigate to **Project Settings > API** and copy:
   - Project URL (`https://xyz.supabase.co`)
   - `anon` `public` key
   - `service_role` secret key (for GitHub Actions runner only)
4. Go to **SQL Editor** and run the initial migration scripts from `database/migrations/` (or run Flyway).
5. Enable Email Auth in **Authentication > Providers**.

### Step 2: Firebase Push Notifications Setup
1. Create a free project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name: `in.examradar.app`.
3. Download `google-services.json` and place it in `apps/android/app/`.
4. Go to **Project Settings > Service accounts** and generate a private key JSON.
5. Save the service account JSON securely for use by the backend notification dispatcher.

### Step 3: GitHub Actions Secrets Configuration
In your GitHub repository settings, navigate to **Settings > Secrets and variables > Actions** and add:
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `FIREBASE_SERVICE_ACCOUNT_BASE64`

### Step 4: Scheduled Collector Workflow
The GitHub Actions workflow `.github/workflows/scheduled-collector.yml` runs every 2 hours:
1. Checks out the repository.
2. Compiles the Java Ingestion Engine.
3. Fetches official recruitment pages using polite polling.
4. Identifies new/modified notices, checks hashes, validates dates, and updates PostgreSQL.
5. Dispatches queued deadline and notice notification jobs via FCM.

---

## 3. Mode B Setup (Conventional Spring Boot Production)

When traffic grows and dedicated hosting is required:

1. Build the production Spring Boot JAR:
   ```bash
   cd services/backend
   mvn clean package -DskipTests
   ```
2. Run as a container or Linux systemd service:
   ```bash
   java -jar -Dspring.profiles.active=prod target/examradar-backend-1.0.0.jar
   ```
3. Set environment variables from `.env` or container orchestration secrets.
4. Update the Android app's configuration to point to your API domain (e.g., `https://api.examradar.in`).

---

## 4. Operational Limitations & Recovery

> **Supabase Inactivity Pausing:** Supabase Free projects pause after 7 days without database activity. If the scheduled collector or client activity pauses, an administrator must log in to the Supabase dashboard and click "Restore project".
> 
> **GitHub Actions Cron Delay:** GitHub Actions scheduled events are queued during high-traffic periods and may run 15-45 minutes after their designated cron timestamp. Deadline reminders are scheduled with generous buffer windows to ensure timely delivery.
