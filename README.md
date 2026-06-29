# Hybrid API and Web Automation Framework

Java TestNG automation framework for API and browser-based web tests.

## Tech Stack

- Java 25
- Maven
- TestNG
- REST Assured
- Selenium WebDriver
- Allure Reports
- Jenkins Pipeline

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
  dev.yaml      Safe placeholder config

src/test/java/com/tests
  api/          API tests
  web/          Web UI tests

testng.xml      API + web suite
Jenkinsfile     Jenkins pipeline
```

## Sensitive Data Policy

Do not commit real API URLs, app URLs, phone numbers, OTPs, tokens, cookies, or credentials.

The committed `dev.yaml` contains placeholders only. Runtime values should be passed using Maven system properties, environment variables, or Jenkins credentials.

Supported override keys:

```text
api.baseUrl
app.baseUrl
auth.origin
auth.referer
auth.partnerUrl
users.admin.username
users.admin.password
browser
headless
remote
gridUrl
allure.autoGenerate
allure.autoOpen
```

Environment variable equivalents use uppercase and underscores, for example:

```text
API_BASE_URL
APP_BASE_URL
USERS_ADMIN_USERNAME
USERS_ADMIN_PASSWORD
```

## Run Locally

Recommended local setup:

1. Copy `src/main/resources/config/dev.local.example.yaml` to `src/main/resources/config/dev.local.yaml`.
2. Put your real API URL, app URL, test mobile, and OTP in `dev.local.yaml`.
3. Do not commit `dev.local.yaml`; it is already ignored by Git.

Run the full TestNG suite:

```bash
mvn test
```

For headless browser execution:

```bash
mvn test -Dheadless=true
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

To view with a local Allure web server:

```bash
mvn allure:serve -DskipTests
```

## Jenkins Pipeline

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
2. Configure tools: JDK 25, Maven, Allure Commandline.
3. Create a Pipeline job.
4. Select `Pipeline script from SCM`.
5. Set repository URL and branch.
6. Set script path to `Jenkinsfile`.
7. Add the credentials listed above in Jenkins Credentials.
8. Build with parameters.

The pipeline publishes:

- JUnit results from `target/surefire-reports/TEST-*.xml`
- Allure results from `target/allure-results`
- Archived test artifacts from `target/surefire-reports/**` and `target/allure-results/**`

## Git Commands

Initialize and push the project:

```bash
git init
git add .
git commit -m "Add automation framework and Jenkins pipeline"
git branch -M main
git remote add origin <your-repository-url>
git push -u origin main
```

Before committing, verify no sensitive values are staged:

```bash
git diff --cached
```
