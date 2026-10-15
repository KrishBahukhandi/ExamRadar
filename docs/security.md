# ExamRadar India — Security & Data Protection Specification

## 1. Security Architecture Principles

1. **Defense in Depth:** Dual protection layers with PostgreSQL Row Level Security (RLS) at the database layer and Spring Security token filters at the application layer.
2. **Zero Mobile Secrets:** No privileged credentials, master API keys, Supabase `service_role` keys, or database passwords reside in the Android client APK. The client only receives the public Supabase Anon key or talks to the secured Spring Boot backend.
3. **Strict Data Isolation:** A candidate cannot view, modify, or delete another candidate's preferences, checklists, or device tokens.
4. **Untrusted External Content:** External HTML pages and PDF documents from official websites are treated as untrusted input. Extracted text is sanitized, scripts are stripped, and extracted links are strictly validated against HTTPS and permitted government domain prefixes.

---

## 2. Supabase Row Level Security (RLS) Policies

All tables containing user-specific data have RLS enabled with granular policies:

```sql
-- Enable RLS on all sensitive tables
ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_exam_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_checklists ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_checklist_items ENABLE ROW LEVEL SECURITY;

-- 1. Profiles Policy (Users can only select and update their own profile)
CREATE POLICY "Users can manage own profile" ON profiles
    FOR ALL
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- 2. User Preferences Policy
CREATE POLICY "Users can view and manage own exam preferences" ON user_exam_preferences
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 3. Device Tokens Policy
CREATE POLICY "Users can register and manage own device tokens" ON device_tokens
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 4. Checklists Policy
CREATE POLICY "Users can access own checklists" ON application_checklists
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 5. Public Tables (Read-only for all, write restricted to service_role)
ALTER TABLE exam_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice_dates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read-only access for published notices" ON notices
    FOR SELECT
    USING (status = 'PUBLISHED');
```

---

## 3. Server-Side Authentication & Spring Security

When running under **Mode B**, Spring Boot intercepts every `/api/v1/*` request using a `JwtAuthenticationFilter`:

- Decodes and verifies the Supabase-issued asymmetric JWT using public keys or symmetric HMAC SHA-256 secret.
- Enforces subject claims: `auth.getName()` extracts the verified UUID.
- Endpoints under `/api/v1/me/**` bind queries to the authenticated subject, completely ignoring any conflicting `user_id` passed in request payloads.
- Administrative endpoints under `/api/v1/admin/**` require explicit `ADMIN` role claim or dedicated server-side `X-Admin-Api-Key` header with timing-safe comparison.

---

## 4. Input Sanitization & URL Validation

1. **Official URL Safelist:** External application links and notice URLs extracted by the collector must:
   - Use `https://` protocol.
   - Match permitted Indian official top-level domains (e.g., `.gov.in`, `.nic.in`, `.ac.in`, `.org.in`, `.sbi`, `.rbi.org.in`).
   - Disallow local, private network, or loopback IPs (`127.0.0.1`, `10.0.0.0/8`, `169.254.169.254`, etc.) to mitigate Server-Side Request Forgery (SSRF).
2. **Untrusted PDF Processing:** Apache PDFBox is configured with resource limits (maximum page limit 50, maximum memory allocation 64MB) to prevent denial-of-service via malformed or decompression-bomb PDF documents.
3. **No Dynamic Code Execution:** Extracted text is treated purely as plain data and escaped before database storage or client rendering.
