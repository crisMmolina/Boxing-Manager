# Shared-file owners

One writer per shared file. Others ask the owner with `uak msg ID --kind request`. Patterns are relative, with no spaces; directories end in `/`.
An owner is not a prerequisite for everyone: consumers work on published contracts and mocks. To transfer ownership, open a PR to main.

| Pattern | Owner task |
|---|---|
| core/build.gradle.kts | CORE-004 |
| core/settings.gradle.kts | CORE-001 |
| .github/workflows/ | CORE-001 |
| core/schemas/ | CORE-007 |
| core/src/main/java/com/sigebox/core/protocol/ | CORE-007 |
| core/src/main/java/com/sigebox/core/model/ | CORE-008 |
| demo_data/ | CORE-007 |
| android/build.gradle.kts | CORE-006 |
| android/settings.gradle.kts | CORE-006 |
| android/gradle/ | CORE-006 |
| android/app/build.gradle.kts | CORE-006 |
| android/app/src/main/AndroidManifest.xml | AND-021 |
| android/app/src/main/res/values/strings.xml | AND-022 |
| android/app/src/main/res/values/themes.xml | AND-022 |
| android/app/src/main/res/values/colors.xml | AND-022 |
| docs/DESIGN.md | DSN-004 |
| central/build.gradle.kts | CORE-006 |
| central/settings.gradle.kts | CORE-006 |
| central/judge-sim/build.gradle.kts | CORE-006 |
| central/src/main/java/com/sigebox/central/App.java | DSK-042 |
| central/src/main/java/com/sigebox/central/ui/Screens.java | DSK-035 |
| central/src/main/java/com/sigebox/central/db/ | DSK-040 |
| .uak/PROJECT.md | CORE-006 |
