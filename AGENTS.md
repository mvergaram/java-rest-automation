# AGENTS.md

## Project context

Java 21+ / REST Assured 4.1 / JUnit 5 end-to-end suite validating the Twilio Intelligence
transcript pipeline (presign → create transcript → poll → validate operator results) across
a configurable list of audio fixtures.

## Code style preferences

- Root package: `com.twilio.automation`. All code lives under `src/test/java` — this is a
  pure test-automation project with no shipped main artifact, so every dependency is
  declared `test` scope and nothing lives under `src/main`.
- Model classes are immutable Java records, one per API shape, with every field annotated
  with an explicit `@JsonProperty` — never rely on an implicit Jackson naming strategy.
- Semantic constraints (confidence ranges, SID formats) are expressed with Jakarta Bean
  Validation annotations (`@NotBlank`, `@Pattern`, `@DecimalMin`/`@DecimalMax`) directly on
  model records, checked via `util.BeanValidation.assertValid(...)`. This is the idiomatic
  Java analogue to the pydantic-style validation used in the original Python suite.
- Assertions use AssertJ (`assertThat(...)`), except JSON Schema checks, which use REST
  Assured's own `matchesJsonSchemaInClasspath(...)` matcher.
- Never hardcode secrets or environment-specific URLs. Everything configurable goes through
  `config.TestConfig`, which reads environment variables first and falls back to
  `src/test/resources/config.properties` only for non-secret defaults (see `.env.example`).
- Test method names describe behavior, not implementation (e.g.
  `transcriptPipelineCompletesForAudioFixture`, never `test1`).
- New HTTP calls to the presign or Twilio Intelligence APIs go through `http.ApiClients` —
  do not build ad hoc `RequestSpecification`s inline in test classes.

## Commands

```bash
# Generate the Maven Wrapper (only needed once, or to change the pinned Maven version)
mvn -N wrapper:wrapper -Dmaven=3.9.9

# Run the full suite (unit test + E2E, parameterized over every audio fixture) — Unix/macOS, Maven
./mvnw test

# Run the full suite — Windows, Maven
mvnw.cmd test

# Run the full suite — Unix/macOS, Gradle
./gradlew test

# Run the full suite — Windows, Gradle
gradlew.bat test

# Run only the signature unit test (fast, deterministic, no network) — Maven
mvnw.cmd test -Dtest=TwilioSignatureTest

# Run only the signature unit test — Gradle
gradlew.bat test --tests "com.twilio.automation.signature.TwilioSignatureTest"

# Run only the E2E suite (requires PRESIGN_BASE_URL/TWILIO_* env vars — see .env.example) — Maven
mvnw.cmd test -Dtest=TranscriptE2ETest

# Run only the E2E suite — Gradle
gradlew.bat test --tests "com.twilio.automation.e2e.TranscriptE2ETest"

# Run with full request/response HTTP logging enabled (PowerShell syntax) — Maven
$env:VERBOSE_HTTP_LOGS="true"; mvnw.cmd test

# Run with full request/response HTTP logging enabled (PowerShell syntax) — Gradle
$env:VERBOSE_HTTP_LOGS="true"; gradlew.bat test
```

Without `PRESIGN_BASE_URL`/`TWILIO_ACCOUNT_SID`/`TWILIO_AUTH_TOKEN`/`TWILIO_SERVICE_SID` set,
`TranscriptE2ETest` skips itself cleanly via a `@BeforeAll` assumption — it does not fail and
makes no network calls. `TwilioSignatureTest` always runs; it needs no credentials.

## Architecture decisions

- `config.TestConfig` / `config.MediaMode` — single entry point for configuration.
  Environment variables always win over `config.properties`; `isFullyConfigured()` gates the
  whole E2E suite so it skips instead of failing when secrets are absent.
- `http.ApiClients` — two `RequestSpecification`s: `presign()` (in-house endpoint) and
  `intelligence()` (Basic Auth against `intelligence.twilio.com`). Both log only on
  assertion failure unless `VERBOSE_HTTP_LOGS=true`.
- `signature.TwilioSignature` — pure HMAC-SHA1 + Base64 utility for `X-Twilio-Signature`.
  Verified by `signature.TwilioSignatureTest` against a vector that was cross-checked with
  an independent HMAC-SHA1 computation in this environment, not against Twilio's live docs
  (no internet access was available when this suite was written) — re-verify against
  Twilio's current "Validating Requests" guide, see that test's Javadoc.
- `media.MediaResolver` / `config.MediaMode` — the only branching point for the unresolved
  question of whether an audio fixture is already hosted (`REFERENCE_ONLY`, implemented) or
  needs uploading first (`UPLOAD_THEN_REFERENCE`, throws `UnsupportedOperationException`
  until that upload contract is confirmed).
- `model.*` — one record per API shape: `PresignResponse`, `ChannelPayload` +
  `MediaProperties` (serialized to the `Channel` form field as a JSON string), `TranscriptResponse`,
  `OperatorResultEnvelope` + `OperatorResultsPage`.
- `support.AudioFixture` + `src/test/resources/testdata/audio-fixtures.json` — the list of
  audio fixtures parameterizing `e2e.TranscriptE2ETest` via `@ParameterizedTest`/
  `@MethodSource`. Replace the placeholder entries with real `audioRef`s and
  `expectedOperators` for your Twilio Intelligence Service before running against real data.
- `src/test/resources/schemas/operator-results-schema.json` — JSON Schema for
  `OperatorResults`. It is a reasonable hypothesis (Twilio's standard list-resource shape),
  not captured from a real response — adjust it once you have one.
- Both `pom.xml` and `build.gradle` compile at release level 21 and pin Groovy 2.5.23 /
  Hamcrest 2.2, because REST Assured 4.1.2 (2019) bundles Groovy 2.5.x, which cannot run
  directly against newer JDK bytecode levels. This has been verified to run on JDK 26 with
  this configuration — see the comments in both build files before changing those pins.
- The `X-Twilio-Signature` algorithm is Twilio's own inbound-webhook validation scheme,
  reused here to sign an *outbound* call to the in-house presign endpoint. This assumes that
  endpoint expects the same convention — confirm the exact contract (param names, query vs.
  body placement) with whoever owns `/media/presign` before relying on this in production.
