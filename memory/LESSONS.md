# Lessons (uak learn)

Append-only. Format: - N | epoch | agent | task | paths | text. `uak recall` and `uak next` show the ones for your paths.

- 1 | 1792907038 | cris-1 | CORE-001 | core/build.gradle.kts,core/settings.gradle.kts,core/gradle.properties,core/gradle/,core/gradlew,core/gradlew.bat,core/src/test/java/com/sigebox/core/SmokeTest.java,.github/workflows/ci.yml | core/ se incluye como composite build y corre con el Gradle del anfitrión (raíz 8.2), no con su wrapper 9.8.2 · core/build.gradle.kts · usar solo APIs compatibles con Gradle 8: .set(...), project.property(), sin toolchains
- 2 | 1792907042 | cris-1 | CORE-001 | core/build.gradle.kts,core/settings.gradle.kts,core/gradle.properties,core/gradle/,core/gradlew,core/gradlew.bat,core/src/test/java/com/sigebox/core/SmokeTest.java,.github/workflows/ci.yml | core/build/ no está en .gitignore · core/ · nunca git add -A en core; añade por nombre
