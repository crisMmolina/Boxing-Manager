# Shared-file owners

One writer per shared file. Others ask the owner with `uak msg ID --kind request`. Patterns are relative, with no spaces; directories end in `/`.
An owner is not a prerequisite for everyone: consumers work on published contracts and mocks. To transfer ownership, open a PR to main.

| Pattern | Owner task |
|---|---|
| core/build.gradle.kts | CORE-004 |
| core/settings.gradle.kts | CORE-001 |
| .github/workflows/ | CORE-001 |
| core/schemas/ | CORE-005 |
| core/src/main/java/com/sigebox/core/protocol/ | CORE-005 |
| core/src/main/java/com/sigebox/core/model/ | CORE-002 |
| demo_data/ | CORE-005 |
| android/build.gradle.kts | CORE-006 |
| android/settings.gradle.kts | CORE-006 |
| android/gradle/ | CORE-006 |
| android/app/build.gradle.kts | CORE-006 |
| android/app/src/main/AndroidManifest.xml | CORE-006 |
| android/app/src/main/res/values/strings.xml | CORE-006 |
| android/app/src/main/res/values/themes.xml | CORE-006 |
| android/app/src/main/res/values/colors.xml | CORE-006 |
| docs/DESIGN.md | DSK-027 |
| central/build.gradle.kts | CORE-006 |
| central/settings.gradle.kts | CORE-006 |
| central/judge-sim/build.gradle.kts | CORE-006 |
| central/src/main/java/com/sigebox/central/App.java | DSK-027 |
| central/src/main/java/com/sigebox/central/ui/Screens.java | DSK-027 |
| central/src/main/java/com/sigebox/central/db/ | DSK-027 |
| .uak/PROJECT.md | CORE-006 |
