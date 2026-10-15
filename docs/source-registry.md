# ExamRadar India — Official Source Registry & Access Policies

## 1. Source Catalogue & Scope (Initial Release)

The initial release focuses on high-impact central recruitment bodies with publicly accessible official portals. State-level recruitment boards will be onboarded incrementally via the source adapter registry.

| Source ID | Organisation | Examination Category | Official Base URL | Notice Listing Path | Permitted Ingestion Method | Verification Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `src-ibps` | Institute of Banking Personnel Selection (IBPS) | Bank PO, Clerk, SO, RRB | `https://www.ibps.in` | `/index.php/wp-json/wp/v2/pages?search=notification` | Official WP REST API + JSON parsing + PDF circulars | **LIVE VERIFIED** (HTTP 200, 10 notices extracted) |
| `src-ssc` | Staff Selection Commission (SSC) | CGL, CHSL, CPO, MTS, JE | `https://ssc.gov.in` | `/api/general-website/portal/notice-boards` | Official Notice Board REST API + PDF attachment downloads | **LIVE VERIFIED** (HTTP 200, 10 notices extracted) |
| `src-upsc` | Union Public Service Commission (UPSC) | Civil Services, Engineering, Defence | `https://upsc.gov.in` | `/examinations/active-examinations` | HTML Scrape | **HEURISTIC QUARANTINE** (Akamai 403; non-negotiables respected) |
| `src-sbi` | State Bank of India (SBI) | Banking / Specialist Officer | `https://sbi.co.in` | `/web/careers/current-openings` | HTML Scrape + Document Link | Planned (Stage 8 Expansion) |
| `src-rbi` | Reserve Bank of India (RBI) | Regulatory / Grade B | `https://opportunities.rbi.org.in` | `/scripts/vacancies.aspx` | HTML Scrape + Gazette Link | Planned (Stage 8 Expansion) |

---

## 2. Source Registry Entity Definition

Each source registered in the system maintains strict metadata tracked in the `sources` database table:

```sql
CREATE TABLE sources (
    id VARCHAR(64) PRIMARY KEY,
    organization VARCHAR(128) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    official_base_url VARCHAR(512) NOT NULL,
    source_type VARCHAR(32) NOT NULL,      -- HTML_SCRAPE, RSS_FEED, JSON_API, PDF_DIRECTORY
    fetch_method VARCHAR(32) NOT NULL,     -- HTTP_GET, CONDITIONAL_GET
    polling_interval_minutes INT DEFAULT 60,
    is_enabled BOOLEAN DEFAULT TRUE,
    parser_version VARCHAR(32) NOT NULL,
    last_attempted_fetch TIMESTAMPTZ,
    last_successful_fetch TIMESTAMPTZ,
    last_successful_content_hash VARCHAR(64),
    consecutive_failures INT DEFAULT 0,
    last_error_message TEXT,
    access_policy_notes TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## 3. Strict Source Access Policies & Compliance Rules

ExamRadar adheres to the highest standards of respectful web crawling:

1. **Explicit Robots & Access Verification:** Before enabling any source adapter, the site's `robots.txt` and terms of use are checked. Scraping is restricted to publicly indexed informational notice boards.
2. **Never Bypass Barriers:** The collector strictly avoids bypassing CAPTCHAs, paywalls, login gates, or IP restrictions. If an official website introduces access barriers, the adapter is disabled and flagged for manual review.
3. **Descriptive User-Agent:** All HTTP requests include a clear User-Agent header:
   ```
   User-Agent: ExamRadarBot/1.0 (+https://examradar.in/bot; bot@examradar.in)
   ```
4. **Request Throttling & Concurrency:**
   - Maximum 1 concurrent request per target domain.
   - Fixed polite delay between requests (minimum 2,000 ms).
   - Random jitter (200-500 ms) to avoid burst patterns.
5. **Conditional HTTP Requests:** Employs `If-None-Match` (ETag) and `If-Modified-Since` headers to eliminate redundant payloads where supported by government servers.
6. **Timeouts & Connection Limits:** Strict 15-second connect timeout and 30-second read timeout.

---

## 4. Source Health & Failure Handling

The ingestion pipeline records health metrics in `source_fetch_runs` and `source_health_snapshots`:

- **Consecutive Failure Threshold:** If a source fails 5 consecutive times, its status transitions to `DEGRADED`. At 10 consecutive failures, the source is automatically `PAUSED` and an alert is sent to administrators.
- **Stale Data Warning:** If a source has not had a successful fetch within 48 hours, client responses and mobile UI elements display a clear **"Data Freshness Warning — Last Checked [Timestamp]"**.
- **No False Cancellations:** An unreachable source or network timeout is **never** interpreted as an exam cancellation or deadline modification. Cached data is preserved without altering confirmed dates.

---

## 5. Empirical Portal Architecture & Adapter Findings

### 5.1 Institute of Banking Personnel Selection (`src-ibps`)
- **Portal Stack:** Nginx 1.14.1 with WordPress/Elementor.
- **REST Endpoint:** `https://www.ibps.in/index.php/wp-json/wp/v2/pages?search=notification&per_page=10`
- **TLS/SSL Quirk:** The server only sends the leaf certificate (`*.ibps.in`) without the intermediate CA certificate (`GlobalSign RSA OV SSL CA 2018`). Standard Java truststore handshakes fail with `sslv3 alert unexpected message` or PKIX path building exceptions. `PoliteHttpClient` resolves this with a resilient TLS context.
- **Payload Quirk:** The WordPress API prepends Elementor layout HTML (`<div data-elementor-type="wp-page"...`) before the JSON payload array. The adapter extracts the substring between the first `[` and last `]`.

### 5.2 Staff Selection Commission (`src-ssc`)
- **Portal Stack:** Angular CLI SPA with Express Node.js backend.
- **REST Endpoint:** `https://ssc.gov.in/api/general-website/portal/notice-boards?page=1&limit=15&contentType=notice-boards&key=createdAt&order=DESC&isAttachment=true&language=english&attributes=id,headline,examId,contentType,redirectUrl,startDate,endDate,language,createdAt`
- **Payload Structure:** Clean JSON with ISO-8601 UTC timestamps, exam codes, and attachment metadata.
- **Attachment Base URL:** `https://ssc.gov.in/api/attachment/` + normalized attachment path (forward slashes).

### 5.3 Union Public Service Commission (`src-upsc`)
- **Portal Protection:** Akamai EdgeSuite WAF returning HTTP 403 Forbidden to automated non-browser clients.
- **Compliance Action:** Per non-negotiable engineering principles, we do not attempt to bypass CAPTCHAs, Akamai bot protections, or IP rotation. The UPSC adapter remains quarantined until a permitted feed is authorized or browser automation is verified.
