# Contributing to Ramzyar

Thanks for helping out. This project is small on purpose — please keep it that way.

## Good first contributions

- **Real OTP templates the parser gets wrong.** Open an issue with the SMS text and **redact the actual code** (`رمز پویا: XXXXXX`). Sender, bank/service name and the result you expected are what matters. This is the single most useful thing you can send.
- **Translations / wording fixes** for the Persian UI.
- **Device reports** for OEMs where the overlay or notification dismissal misbehaves (include Android version and ROM).

## Pull requests

1. Open an issue first for anything bigger than a typo, so we do not duplicate work.
2. Keep the change focused: one behaviour per PR.
3. **Parser changes require a unit test.** `CodeExtractor` is pure Kotlin and runs on the JVM:

   ```bash
   ./gradlew testDebugUnitTest
   ```

   Add your case to `app/src/test/java/ir/asgvp/ramzyar/CodeExtractorTest.kt`.
4. Build before pushing: `./gradlew assembleDebug`.
5. Follow the existing style — Kotlin official code style, Compose for UI, no new dependencies without a reason.

## Hard constraints

- **No `INTERNET` permission, ever.** This is a privacy guarantee, not a preference.
- No analytics, crash reporting or ad SDKs.
- No network calls, remote config or telemetry of any kind.
- Do not log SMS bodies or extracted codes.

## Commit messages

Short imperative subject line, present tense, e.g. `fix: prefer digits after the last keyword`. Reference the issue number when there is one.

## License

By contributing you agree that your contribution is licensed under the [MIT License](LICENSE).
