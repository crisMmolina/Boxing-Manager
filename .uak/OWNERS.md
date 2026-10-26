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
| demo_data/ | CORE-002 |
| build.gradle.kts | AND-001 |
| settings.gradle.kts | AND-001 |
| gradle/ | AND-001 |
| app/build.gradle.kts | AND-012 |
| app/src/main/AndroidManifest.xml | AND-001 |
| app/src/main/res/values/strings.xml | AND-012 |
| app/src/main/res/values/themes.xml | AND-012 |
| app/src/main/res/values/colors.xml | AND-012 |
| docs/DESIGN.md | DSN-003 |
| BOXCENTRAL/build.gradle.kts | DSK-018 |
| BOXCENTRAL/settings.gradle.kts | DSK-001 |
| BOXCENTRAL/judge-sim/build.gradle.kts | DSK-001 |
| BOXCENTRAL/src/main/java/com/sigebox/central/App.java | DSK-018 |
| BOXCENTRAL/src/main/java/com/sigebox/central/ui/Screens.java | DSK-018 |
| BOXCENTRAL/src/main/java/com/sigebox/central/db/ | DSK-019 |
| .uak/PROJECT.md | CORE-001 |
