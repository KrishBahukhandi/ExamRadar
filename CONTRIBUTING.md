# Contributing to ExamRadar India

Thank you for your interest in contributing to **ExamRadar India**! Our mission is to provide Indian competitive examination aspirants with an honest, distraction-free, zero-bloat tracking platform that exclusively monitors official government notifications.

---

## 🧭 Core Principles & Scope Boundaries

Before contributing, please review our core architectural rules:

1. **Strictly Non-Commercial:** We do **not** accept PRs that introduce commercial features, paid video course recommendations, mock test upsells, affiliate links, or banner advertising.
2. **Official Public Sources Only:** Every notice must originate directly from an official central or state government portal, gazette, or public sector enterprise (UPSC, SSC, IBPS, State PSCs, RRB, etc.).
3. **Data Integrity & Immutability:** Notices must link to original public PDFs and never modify original text without recording an immutable revision history.
4. **Polite Web Crawling:** Scrapers must identify themselves with an informative `User-Agent`, respect minimum 2000ms rate limits, and use SHA-256 change detection to avoid hammering government servers.

---

## 🛠️ Development Environment Setup

### 1. Prerequisites
- **Java 17+** (OpenJDK or Eclipse Temurin)
- **Maven 3.8+**
- **Android Studio** Ladybug (2024.2.1+) with Android SDK 35
- **Git**

### 2. Fork and Clone
```bash
git clone https://github.com/<your-username>/examradar.git
cd examradar
```

### 3. Verify Local Builds
Run the test suites to ensure your setup is working cleanly:

```bash
# Test Backend (Runs H2 in-memory DB tests - 0 external dependencies)
cd services/backend
mvn clean test

# Test Android App Unit Tests
cd ../../apps/android
./gradlew testDebugUnitTest
```

---

## 🔌 Adding a New Official Source Adapter

To add support for a new government body (e.g., State PSCs like UPPSC, BPSC, MPSC, or Railway Recruitment Boards):

1. **Create the Adapter Class:**
   Implement the adapter under `services/backend/src/main/java/com/examradar/backend/collector/adapter/`:
   ```java
   @Component
   public class UppscSourceAdapter implements SourceAdapter {
       
       @Override
       public String getSourceId() {
           return "src-uppsc";
       }

       @Override
       public FetchResult fetchAndParse(Source source, PoliteHttpClient httpClient) {
           // 1. Fetch official recruitment page with polite delay
           // 2. Compute SHA-256 hash of page content
           // 3. Extract title, notice type, published date, application deadline, and official URL
           // 4. Return FetchResult with parsed CandidateNotice list
       }
   }
   ```

2. **Add a Source Database Entry:**
   Add a migration in `database/migrations/` (or seed script in `database/seed/`) defining the source ID, organization name, portal URL, and fetch interval.

3. **Author a Unit Test with Fixture HTML:**
   Save a representative HTML snapshot of the portal in `services/backend/src/test/resources/fixtures/` and author a unit test verifying parsing of dates, corrigenda, and links.

---

## 🌿 Pull Request Workflow

1. **Create a Feature Branch:**
   ```bash
   git checkout -b feature/adapter-uppsc-recruitment
   ```
2. **Make Focused, Atomic Commits:**
   Follow conventional commit messages:
   - `feat(collector): add UPPSC official source adapter`
   - `fix(android): resolve dark mode date badge contrast`
   - `test(backend): add unit test for IBPS corrigendum parser`
3. **Run All Tests Before Submitting:**
   ```bash
   # Both must succeed
   (cd services/backend && mvn test)
   (cd apps/android && ./gradlew testDebugUnitTest)
   ```
4. **Open a Pull Request:**
   - Reference any related issues.
   - Include a description of the official portal added, sample URLs tested, and verification results.

---

## 🔒 Security & Vulnerability Reporting

If you identify a security issue (especially relating to authentication, token handling, or scraper rate limits), please review our [SECURITY.md](SECURITY.md) for responsible disclosure procedures instead of opening a public issue.
