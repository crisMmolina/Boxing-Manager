# worklog/WL-1794240412-crxthrx-1-DSK-039-hack.mYfXrs.md · DSK-039

## Live summary
- Done: Claim created; implementation pending
- Decision: Own branch and worktree
- Why: Parallel work without touching the main checkout
- Failing: No validation run yet
- Commands: `central/gradlew -p central test`
- Next: `ConsistencyTest` con los títulos de cada sección

## History
- 1794240412 | CLAIMED | crxthrx-1 | claim; next: `ConsistencyTest` con los títulos de cada sección
- 1794240486 | CLAIMED | crxthrx-1 | pending decision DSK-039-1794240486; option: Crear un evento se llama 'Crear evento' en todos lados (botón de Inicio, título y botón que lo crea); Why: spec 005 pide el mismo verbo para la misma acción; 'Nuevo' no es verbo y el botón que crea ya dice 'Crear evento'
- 1794240546 | CLAIMED | crxthrx-1 | pending decision DSK-039-1794240546; option: Corrige la anterior: 'Nuevo evento' abre el formulario (y es su título) y 'Crear evento' lo crea, en Inicio y en Evento; no se cambia. Inicio fuera del evento no muestra la barra: su título 'Eventos' queda; Why: son dos acciones distintas, cada una con un solo nombre; InicioTest las fija
