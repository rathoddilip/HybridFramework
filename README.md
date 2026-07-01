# Hybrid API and Web Automation Framework

Java TestNG automation framework for API and browser-based web tests.

Repository: [github.com/rathoddilip/HybridFramework](https://github.com/rathoddilip/HybridFramework)

## Tech Stack

| Component | Version |
|-----------|---------|
| Java | 17 |
| Maven | 3.x |
| TestNG | 7.9 |
| Selenium | 4.29 |
| REST Assured | 5.4 |
| Allure | 2.26 |

## Prerequisites

- **JDK 17** (required — matches `pom.xml`)
- **Maven 3.x**
- **Browsers** for web tests: Chrome, Firefox, Microsoft Edge
- Network access for API calls and first-time Edge WebDriver download

## Project Structure

```text
src/main/java/com/framework
  api/          API service clients
  config/       YAML and system-property configuration loader
  core/         Base API, test, page, and driver utilities
  listeners/    TestNG and Allure listeners
  models/       API request and response models
  pages/        Selenium page objects

src/main/resources/config
  dev.yaml                 Safe placeholder config (committed)
  dev.local.example.yaml   Template for local secrets (committed)
  dev.local.yaml           Your real values (gitignored — create locally)
  staging.yaml             Staging placeholders

src/test/java/com/tests
  api/          API tests (FixerraAuthAPITest)
  web/          Web UI tests (LoginTest, SmokeTest)

testng.xml                 Default suite — API + web tests
testng-crossbrowser.xml    Parallel cross-browser web suite (LoginTest)
pom.xml                    Maven build and profiles
Jenkinsfile                Jenkins pipeline
automation-ci.yml          GitHub Actions workflow
docker-compose.yml         Optional Selenium Grid
```

## Local Setup

### 1. Clone and open the project

After cloning [HybridFramework](https://github.com/rathoddilip/HybridFramework), open the folder that contains `pom.xml` (repository root).

> If your workspace has a parent folder (e.g. `AIAutomationTest/files`), always run Maven from the directory that contains `pom.xml`.

### 2. Create local config

**Windows (PowerShell):**

```powershell
Copy-Item src\main\resources\config\dev.local.example.yaml src\main\resources\config\dev.local.yaml
```

**macOS / Linux:**

```bash
cp src/main/resources/config/dev.local.example.yaml src/main/resources/config/dev.local.yaml
```

### 3. Add your real values

Edit `src/main/resources/config/dev.local.yaml` (see **Configuration** below).

Do **not** commit `dev.local.yaml` — it is gitignored.

### 4. Configure IDE Java runtime (VS Code / Cursor)

Use **JDK 17** for the Java language server so the IDE does not compile with a newer JDK.

Example `.vscode/settings.json` (adjust the path to your JDK 17 install):

```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.jdt.ls.java.home": "<path-to-jdk-17>",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "<path-to-jdk-17>",
      "default": true
    }
  ]
}
```

Windows Temurin example:

```text
C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot
```

After changing JDK settings: **Developer: Reload Window** (`Ctrl+Shift+P`).

## Configuration

### API vs App URLs

Use **separate** hosts for API and UI:

| Key | Purpose | Example |
|-----|---------|---------|
| `api.baseUrl` | Backend auth API | `https://users-api.dev.example.com` |
| `app.baseUrl` | Frontend application | `https://user-app.dev.example.com` |
| `auth.origin` | CORS Origin header | Same as app URL |
| `auth.referer` | Referer header | Same as app URL + `/` |
| `auth.partnerUrl` | OTP verify request body | Same as app URL |
| `auth.consent` | OTP verify consent flag | `false` |

### Example `dev.local.yaml`

```yaml
api:
  baseUrl: https://your-api-host.example.com

app:
  baseUrl: https://your-app-host.example.com

auth:
  origin: https://your-app-host.example.com
  referer: https://your-app-host.example.com/
  partnerUrl: https://your-app-host.example.com
  consent: false

users:
  admin:
    username: "9000000000"
    password: "000000"
```

### Override keys

Values can be set in `dev.local.yaml`, Maven system properties (`-Dkey=value`), or environment variables.

**Maven / config keys:**

```text
api.baseUrl
app.baseUrl
auth.origin
auth.referer
auth.partnerUrl
auth.consent
users.admin.username
users.admin.password
browser
headless
remote
gridUrl
env
allure.autoGenerate
allure.autoOpen
```

**Environment variables (examples):**

```text
API_BASE_URL
APP_BASE_URL
USERS_ADMIN_USERNAME
USERS_ADMIN_PASSWORD
BROWSER
HEADLESS
ENV
```

### Sensitive data policy

Do not commit real URLs, phone numbers, OTPs, tokens, or credentials.

- Committed `dev.yaml` → placeholders only
- Real values → `dev.local.yaml` (local) or CI secrets (Jenkins / GitHub Actions)

## Test Suites

| Suite file | What runs | Parallel |
|------------|-----------|----------|
| `testng.xml` (default) | API tests + `LoginTest` + `SmokeTest` | No |
| `testng-crossbrowser.xml` | `LoginTest` on Chrome, Firefox, Edge | Yes (3 threads) |

## Run Tests

Run all commands from the directory that contains `pom.xml`.

### Full suite (default)

```bash
mvn test
```

### By group

```bash
mvn test -Dgroups=api
mvn test -Dgroups=web
```

### Single browser

```bash
mvn test -Dgroups=web -Dbrowser=chrome
mvn test -Dgroups=web -Dbrowser=firefox
mvn test -Dgroups=web -Dbrowser=edge
```

Default browser: `chrome`.

### Headless

```bash
mvn test -Dgroups=web -Dheadless=true
```

### Cross-browser (parallel)

```bash
mvn test -Pcrossbrowser
```

Equivalent:

```bash
mvn test -Dsurefire.suiteXmlFiles=testng-crossbrowser.xml
```

Cross-browser with a specific environment:

```bash
mvn test -Pcrossbrowser,staging
```

**Invalid:** `mvn testng-crossbrowser.xml` — Maven does not accept a suite file as a goal.

### Maven profiles

| Profile | Command | Effect |
|---------|---------|--------|
| `dev` (default) | `mvn test` | Loads `config/dev.yaml` + optional `dev.local.yaml` |
| `staging` | `mvn test -Pstaging` | Loads `config/staging.yaml` |
| `prod` | `mvn test -Pprod` | Sets `-Denv=prod` (add `config/prod.yaml` when needed) |
| `crossbrowser` | `mvn test -Pcrossbrowser` | Runs `testng-crossbrowser.xml` |

### Selenium Grid (optional)

```bash
docker-compose up -d
mvn test -Dgroups=web -Dremote=true -Dbrowser=chrome
```

## Allure Report

Generate static report:

```bash
mvn allure:report -DskipTests
```

Open:

```text
target/site/allure-maven-plugin/index.html
```

Serve in browser:

```bash
mvn allure:serve -DskipTests
```

Raw results: `target/allure-results/`

## Troubleshooting

### `There is no POM in this directory`

```text
Run Maven from the folder that contains pom.xml.
Wrong:  D:\AIAutomationTest
Right:  D:\AIAutomationTest\files   (if using a nested layout)
Right:  <clone-root>                (GitHub repo root)
```

### `class file version 69.0` (Java mismatch)

Project targets **Java 17**. Stale classes may have been built with a newer JDK.

**PowerShell:**

```powershell
Remove-Item -Recurse -Force target\classes, target\test-classes -ErrorAction SilentlyContinue
mvn test
```

Set IDE to JDK 17 (see **Local Setup**) and reload the window.

### Edge WebDriver fails

Edge driver is resolved from [msedgedriver.microsoft.com](https://msedgedriver.microsoft.com) (not the deprecated `azureedge.net` CDN).

Requirements:

- Microsoft Edge installed
- Network access to `msedgedriver.microsoft.com`

Driver is cached under the system temp folder after the first successful download.

### `mvn clean` fails on `allure-serve.log`

Stop the Allure server, then:

```powershell
Remove-Item target\allure-serve.log -Force -ErrorAction SilentlyContinue
mvn compile test
```

## CI/CD

### GitHub Actions

File: `automation-ci.yml`

- Triggers: push/PR to `main`, schedule, manual dispatch
- Manual inputs: environment (`dev` / `staging` / `prod`), browser (`chrome` / `firefox` / `edge`)
- Publishes Allure artifacts

Use **JDK 17** in the workflow to match `pom.xml`.

### Jenkins

Required secret text credentials:

```text
automation-api-base-url
automation-app-base-url
automation-auth-origin
automation-auth-referer
automation-partner-url
automation-mobile
automation-otp
```

Setup:

1. Plugins: Git, Pipeline, JUnit, Allure Jenkins Plugin
2. Tools: **JDK 17**, Maven, Allure Commandline
3. Pipeline job → **Pipeline script from SCM** → script path: `Jenkinsfile`
4. Add credentials above

Published artifacts:

- JUnit: `target/surefire-reports/TEST-*.xml`
- Allure: `target/allure-results`

## Git Safety

Before every commit:

```bash
git status
git diff --cached
```

Never commit:

- `src/main/resources/config/*.local.yaml`
- `target/` (reports may contain JWT tokens)
- `.env` or credential files
