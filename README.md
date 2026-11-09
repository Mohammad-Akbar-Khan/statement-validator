# Trading Statement Validator (Spring Boot + Vertex AI)

A small Java microservice starter with a browser upload form and four gated model calls:

1. File validation: reject documents that do not appear to be trading statements.
2. Account validation: compare statement account/owner details with an expected account ID and/or owner supplied by the user.
3. Trade identification: check for executed trades.
4. Extraction: return account details and trade rows as JSON.

The service accepts PDF and image uploads up to 20 MB. The file stays in memory for the request and is sent as inline data to Vertex AI; this starter does not persist uploaded files.

## Tech stack

- Java 21
- Spring Boot 3.5.7, Maven
- Google Gen AI Java SDK, configured for Vertex AI / Gemini Enterprise Agent Platform
- Plain HTML/CSS/JavaScript UI served by Spring Boot

## Prerequisites

- JDK 21 and Maven 3.9+
- A Google Cloud project with billing enabled
- Vertex AI API enabled in that project
- A Google identity with permission to call Vertex AI (for example, Vertex AI User)
- `gcloud` CLI installed for local Application Default Credentials (ADC)

## Configure Google Cloud and run locally

1. In Google Cloud Console, select/create a project, attach billing, and enable **Vertex AI API** (`aiplatform.googleapis.com`).
2. Install and authenticate the Google Cloud CLI. For local development, run:

   ```powershell
   gcloud auth application-default login
   gcloud config set project YOUR_PROJECT_ID
   ```

   ADC is used by the Google SDK; do not commit service account keys or credentials to GitHub.
3. From the project directory, set the project and location, then start Spring Boot:

   ```powershell
   $env:GOOGLE_CLOUD_PROJECT = "YOUR_PROJECT_ID"
   $env:GOOGLE_CLOUD_LOCATION = "us-central1"
   $env:VERTEX_MODEL = "gemini-2.5-flash"
   mvn spring-boot:run
   ```
4. Open [http://localhost:8080](http://localhost:8080), enter at least an expected account ID or owner, choose a PDF/image, and submit.

`GOOGLE_CLOUD_LOCATION` and model are configurable. Choose a Gemini model that is available to your project and location. If using a region/model combination different from the defaults, change these environment variables.

## API

`POST /api/validate` as `multipart/form-data`:

- `file` (required): PDF or image, max 20 MB
- `expectedAccount` (optional if `expectedOwner` is provided)
- `expectedOwner` (optional if `expectedAccount` is provided)

The response contains the stage outputs and a `status` of `completed` or `stopped`. Model outputs are asked for JSON and parsed by the backend. Treat extracted financial data as unverified until checked against the source statement; this is a prototype, not a compliance or trading decision system.

## Project layout

- `src/main/java/.../web/StatementController.java`: upload API and type/size checks
- `src/main/java/.../service/StatementPipeline.java`: four prompts and conditional flow
- `src/main/java/.../config/VertexConfig.java`: Vertex client
- `src/main/resources/static/index.html`: upload UI
- `src/main/resources/application.yml`: port, upload limit, project, location, model

## GitHub

Create an empty GitHub repository, then from this folder:

```powershell
git init
git add .
git commit -m "Initial Spring Boot statement validator"
git branch -M main
git remote add origin https://github.com/YOUR_USER/YOUR_REPOSITORY.git
git push -u origin main
```

Keep local credentials outside the repository. `.gitignore` excludes Maven output, IDE files, and environment files. GitHub is only source hosting; this does not deploy the service. For a next deployment step, Cloud Run is a straightforward target: build a container, assign its runtime service account Vertex AI permissions, and configure the project/location/model environment variables. Do not place a service account key in the container or GitHub.

## Before production

Add authentication/authorization, request throttling, audit and retention policy, stronger schema validation and retry/error handling, prompt versioning, confidence/evidence review, and tests against a representative corpus of statements. Financial statements contain sensitive data; choose storage/logging/retention practices deliberately before real-user use.
