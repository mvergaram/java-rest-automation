# Twilio Intelligence E2E REST Automation

End-to-end REST Assured + JUnit 5 suite validating the Twilio Intelligence transcript
pipeline (presign → create transcript → poll → validate operator results), parameterized
over a configurable list of audio fixtures.

## Infrastructure stack

| Concern              | Choice                                                            |
|-----------------------|-------------------------------------------------------------------|
| Language / runtime     | Java, compiled at `--release 17` (requires **JDK 17+**; verified running on JDK 26) |
| HTTP / test DSL        | REST Assured `4.1.2` (`given/when/then`)                          |
| JSON Schema validation | REST Assured `json-schema-validator` `4.1.2`                      |
| Test engine            | JUnit 5 (Jupiter), `@ParameterizedTest` + `@MethodSource`          |
| JSON (de)serialization | Jackson (`jackson-databind`), Java records with `@JsonProperty`    |
| Semantic validation    | Jakarta Bean Validation via Hibernate Validator (`@NotBlank`, `@Pattern`, `@DecimalMin/@Max`) — the Java analogue to pydantic |
| Polling                | Awaitility (replaces a fixed `sleep` with a bounded poll-until-completed) |
| Assertions             | AssertJ                                                            |
| Build tools            | **Both** Maven (Maven Wrapper) and Gradle (Gradle Wrapper) against the same `src/` tree |

Build tool versions are pinned in the wrappers themselves: Maven `3.9.9`
(`.mvn/wrapper/maven-wrapper.properties`) and Gradle `9.8.1`
(`gradle/wrapper/gradle-wrapper.properties`) — both confirmed to run on JDK 26.

No local Maven or Gradle installation is required; use the wrapper scripts below.

**Requirement: JDK 17 or newer.** The build pins `--release 17` — the minimum Java version
that supports the records and switch expressions used in this codebase. A JDK older than 17
will fail to compile; any JDK 17+ (including non-LTS versions) works.

## Commands

```bash
# Run the full suite (unit test + E2E, one run per audio fixture) — Windows, Maven
mvnw.cmd test

# Run the full suite — Windows, Gradle
gradlew.bat test

# Run the full suite — Unix/macOS, Maven
./mvnw test

# Run the full suite — Unix/macOS, Gradle
./gradlew test

# Run only the signature unit test (fast, deterministic, no network)
mvnw.cmd test -Dtest=TwilioSignatureTest

# Run only the E2E suite (needs real credentials — see Configuration below)
mvnw.cmd test -Dtest=TranscriptE2ETest

# Run with full request/response HTTP logging (PowerShell syntax)
$env:VERBOSE_HTTP_LOGS="true"; mvnw.cmd test
```

Without real Twilio credentials configured, `TranscriptE2ETest` skips itself cleanly
(no failure, no network calls) via a `@BeforeAll` assumption. `TwilioSignatureTest` always
runs since it needs no credentials.

## Configuration

Copy `.env.example` to `.env` at the project root and fill in the values — `TestConfig`
loads `.env` automatically, no shell export needed (`.env` is git-ignored; never commit
real secrets). A real environment variable still overrides `.env` when both are set, which
is what CI and one-off runs use instead of a file.

- `PRESIGN_BASE_URL` — base URL of the in-house presign endpoint
- `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_SERVICE_SID` — Twilio credentials
- `MEDIA_MODE` — `REFERENCE_ONLY` (default) or `UPLOAD_THEN_REFERENCE`
- `VERBOSE_HTTP_LOGS` — `true` to always log full HTTP request/response bodies

## Important files and folders

```
pom.xml / build.gradle         Build definitions (Maven / Gradle), same dependencies
mvnw, mvnw.cmd, .mvn/          Maven Wrapper
gradlew, gradlew.bat, gradle/  Gradle Wrapper
.env.example                   Template for .env — copy to .env (git-ignored), loaded automatically
AGENTS.md                      Source of truth: conventions, commands, architecture decisions
CLAUDE.md / CLAUDE.local.md    Claude Code–specific pointers / local session notes (not shared)

src/test/java/com/twilio/automation/
├── config/     TestConfig (env var > .env > config.properties), DotenvLoader, MediaMode
├── http/       ApiClients — reusable RequestSpecifications (presign, Twilio Intelligence)
├── signature/  TwilioSignature (X-Twilio-Signature) + its unit test
├── media/      MediaResolver — resolves an audio fixture into a media_url
├── model/      Jackson records for every API shape (PresignResponse, TranscriptResponse, ...)
├── support/    AudioFixture — one row of testdata/audio-fixtures.json
├── util/       JsonUtil (ObjectMapper), BeanValidation (semantic assertions)
└── e2e/        TranscriptE2ETest — the 4-step pipeline, run per audio fixture

src/test/resources/
├── config.properties          Non-secret defaults (poll timing, media mode)
├── testdata/audio-fixtures.json   Audio fixtures to parameterize the E2E suite over
└── schemas/operator-results-schema.json   JSON Schema for OperatorResults
```

## Further documentation

See [AGENTS.md](./AGENTS.md) for code style conventions, the full command reference, and
the architecture decisions behind this project — including the known assumptions that still
need to be confirmed against the real Twilio/presign contracts before this suite is run
against production data.
