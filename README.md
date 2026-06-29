# Hybrid API and Web Automation Framework

Java TestNG automation framework for API and browser-based web tests.

Repository: [github.com/rathoddilip/HybridFramework](https://github.com/rathoddilip/HybridFramework)

## Tech Stack

- Java 17
- Maven
- TestNG
- REST Assured
- Selenium WebDriver 4.x
- Allure Reports
- Jenkins Pipeline / GitHub Actions

## Prerequisites

- **JDK 17** (matches `pom.xml`)
- **Maven 3.x**
- **Browsers** (for web / cross-browser tests): Chrome, Firefox, Microsoft Edge
- Internet access (API tests and first-time Edge driver download)

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
  api/          API tests
  web/          Web UI tests

testng.xml                 Default suite — API + web tests
testng-crossbrowser.xml    Parallel cross-browser web suite
pom.xml                    Maven build and profiles
Jenkinsfile                Jenkins pipeline
automation-ci.yml          GitHub Actions workflow
docker-compose.yml         Optional Selenium Grid
```

## Local Setup

1. Clone the repository and open the project folder (where `pom.xml` lives).
2. Copy the local config template:

   ```bash
   cp src/main/resources/config/dev.local.example.yaml src/main/resources/config/dev.local.yaml
   ```

3. Edit `dev.local.yaml` with your real values (see **Configuration** below).
4. Do **not** commit `dev.local.yaml` — it is already listed in `.gitignore`.

### VS Code / Cursor (recommended)

Set the project JDK to **17** so the IDE does not compile with a newer Java version:

```json
{
  "java.jdt.ls.java.home": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.18.8-hotspot",
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-17",
      "path": "C:\\Program Files\\Eclipse Adoptium\\jdk-17.0.18.8-hotspot",
      "default": true
    }
  ]
}
```

Adjust the path to match your JDK 17 installation.

## Configuration

### API vs App URLs

Use **separate** hosts for API and UI:

| Key | Purpose | Example |
|-----|---------|---------|
| `api.baseUrl` | Backend auth API | `https://users-api.dev.example.com` |
| `app.baseUrl` | Frontend application | `https://user-app.dev.example.com` |
| `auth.origin` | CORS origin header | Same as app URL |
| `auth.referer` | Referer header | Same as app URL |
| `auth.partnerUrl` | Partner URL in OTP verify body | Same as app URL |

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

### Sensitive Data Policy

Do not commit real API URLs, app URLs, phone numbers, OTPs, tokens, cookies, or credentials.

The committed `dev.yaml` contains **placeholders only**. Runtime values belong in `dev.local.yaml`, Maven system properties, environment variables, or CI secrets.

Supported override keys:

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
allure.autoGenerate
allure.autoOpen
env
```

Environment variable equivalents use uppercase and underscores, for example:

```text
API_BASE_URL
APP_BASE_URL
USERS_ADMIN_USERNAME
USERS_ADMIN_PASSWORD
BROWSER
HEADLESS
ENV
```

## Run Tests

All Maven commands must be run from the directory that contains `pom.xml`.

### Full suite (default `testng.xml`)

Runs API tests, then web tests:

```bash
mvn test
```

### Run by test group

```bash
# API tests only
mvn test -Dgroups=api

# Web tests only
mvn test -Dgroups=web
```

### Single browser

```bash
mvn test -Dgroups=web -Dbrowser=chrome
mvn test -Dgroups=web -Dbrowser=firefox
mvn test -Dgroups=web -Dbrowser=edge
```

Supported values: `chrome` (default), `firefox`, `edge`.

### Headless browser

```bash
mvn test -Dgroups=web -Dheadless=true
```

### Cross-browser (parallel)

Runs `LoginTest` on Chrome, Firefox, and Edge **in parallel** using `testng-crossbrowser.xml`:

```bash
mvn test -Pcrossbrowser
```

Alternative (without Maven profile):

```bash
mvn test -Dsurefire.suiteXmlFiles=testng-crossbrowser.xml
```

**Note:** Do not run `mvn testng-crossbrowser.xml` — that is not valid Maven syntax.

### Maven profiles (environment)

| Profile | Command | Config file |
|---------|---------|-------------|
| `dev` (default) | `mvn test` | `config/dev.yaml` + `dev.local.yaml` |
| `staging` | `mvn test -Pstaging` | `config/staging.yaml` |
| `prod` | `mvn test -Pprod` | `config/prod.yaml` (if present) |
| `crossbrowser` | `mvn test -Pcrossbrowser` | Uses `testng-crossbrowser.xml` |

Combine profiles and groups as needed:

```bash
mvn test -Pstaging -Dgroups=api
```

### Optional: Selenium Grid

```bash
docker-compose up -d
mvn test -Dgroups=web -Dremote=true -Dbrowser=chrome
```

## Allure Report

Generate a static Allure report:

```bash
mvn allure:report -DskipTests
```

Static report path:

```text
target/site/allure-maven-plugin/index.html
```

Serve the report in a browser:

```bash
mvn allure:serve -DskipTests
```

Test results are written to `target/allure-results/` after each run.

## Troubleshooting

### `There is no POM in this directory`

Run Maven from the folder that contains `pom.xml`, not the parent repo root.

### `class file version 69.0` / Java version mismatch

The project targets **Java 17**. If VS Code or another tool compiles with Java 21+, delete stale classes and re-run with Maven:

```bash
# PowerShell
Remove-Item -Recurse -Force target\classes, target\test-classes -ErrorAction SilentlyContinue
mvn test
```

Also configure your IDE to use JDK 17 (see **Local Setup**).

### Edge WebDriver download fails

Edge driver is downloaded automatically from `msedgedriver.microsoft.com` (the old `azureedge.net` CDN is deprecated). Ensure:

- Microsoft Edge is installed
- Network access to `msedgedriver.microsoft.com` is allowed

The driver is cached under your system temp folder after the first successful download.

### `mvn clean` fails on `allure-serve.log`

Stop any running Allure server, delete the locked file, then rebuild:

```bash
Remove-Item target\allure-serve.log -Force -ErrorAction SilentlyContinue
mvn compile test
```

## CI/CD

### GitHub Actions

Workflow file: `automation-ci.yml`

- Runs on push/PR to `main` / `develop`
- Supports manual dispatch with environment and browser selection
- Publishes Allure artifacts

### Jenkins

The `Jenkinsfile` expects these Jenkins secret text credentials:

```text
automation-api-base-url
automation-app-base-url
automation-auth-origin
automation-auth-referer
automation-partner-url
automation-mobile
automation-otp
```

Jenkins job setup:

1. Install plugins: Git, Pipeline, JUnit, Allure Jenkins Plugin.
2. Configure tools: **JDK 17**, Maven, Allure Commandline.
3. Create a Pipeline job.
4. Select **Pipeline script from SCM**.
5. Set repository URL and branch.
6. Set script path to `Jenkinsfile`.
7. Add the credentials listed above in Jenkins Credentials.
8. Build with parameters.

The pipeline publishes:

- JUnit results from `target/surefire-reports/TEST-*.xml`
- Allure results from `target/allure-results`
- Archived test artifacts from `target/surefire-reports/**` and `target/allure-results/**`

## Git Safety

Before committing, verify no secrets are staged:

```bash
git status
git diff --cached
```

Never commit:

- `src/main/resources/config/*.local.yaml`
- `target/` (may contain tokens in test reports)
- `.env` files or credentials
