# Spec 008 · Box Central: resultado sin congelarse, teléfonos que no se quedan atorados y nombre nuevo
- Status: approved (cris, 2026-11-17: «le di a cerrar pelea y se me quedó trabada la app de escritorio… requiero que la app se llame Box Central… [el teléfono] no se puede quedar en esa pantalla por siempre»; eligió «Box Juez» y «Preguntar y dejar salir») · Owner: cris · PROJECT principles: 1, 2, 3
- Alcance: `central/` (resultado y nombre), `android/` (salida y nombre), docs. **No cambia el protocolo** (`core/`): la central ya puede terminar una pelea («Terminar pelea → Sin decisión / Otro») y ya existe `EVENT_ENDED`.

## Problem
1. **La central se congela al cerrar la pelea (macOS).** `ResultScreen.show` crea una ventana con dueño (`initOwner`) y le pone pantalla completa antes de `show()`. En macOS, JavaFX entra a `MacView._enterFullscreen` y el hilo de la interfaz no vuelve (volcado de hilos del 2026-11-17: `ResultScreen.show:41` → `WindowStage.setFullScreen` → `_enterNestedEventLoop`). La pelea sí queda cerrada en SQLite (`service.close` corre antes), pero la ventana queda en blanco y la app no responde.
2. **Una pelea cerrada ya no se puede exportar.** La ventana de resultado (PDF, CSV, Excel) solo se abre en el momento de cerrar. Si esa ventana falla o se cierra, no hay forma de volver a abrirla.
3. **El teléfono se queda atorado.** Si la central deja de hablar (se cerró, se apagó la laptop, la mesa se fue) el teléfono se queda para siempre en Esperando, Puntuar sin round abierto, «Round N enviado» o Siguiente pelea, sin manera de salir.
4. **El juez no puede volver a su lugar (visto el 2026-11-17).** El teléfono perdió su lugar guardado y quedó en Unirme. La central lo tiene aprobado y desconectado en la pelea siguiente (lugar copiado por `NEXT_FIGHT`), pero la pantalla Jueces solo muestra código y QR a un lugar sin conectar o rechazado, y `JudgeHub.codeInfo` responde `CODE_UNKNOWN` a cualquier lugar ya registrado. «Rechazar» tampoco sirve: el mismo teléfono rechazado sigue rechazado. Además, el teléfono borra su lugar guardado **antes** de saber si el código nuevo sirve (`JudgeConnection.join`), así que un código equivocado lo deja sin lugar.
5. **Nombre.** El producto se llama **Box Central** (app de escritorio) y **Box Juez** (app del teléfono), no SIGEBOX.

## Stories
- Como operador, quiero que cerrar la pelea me lleve al resultado sin que la app se congele, y poder volver a abrir ese resultado para exportarlo cuando lo necesite.
- Como juez, si la pelea se queda sin terminar, quiero que el teléfono me pregunte si sigue y me deje salir, sin perder lo que califiqué.
- Como organizador, quiero que las apps se llamen Box Central y Box Juez.

## Acceptance criteria (EARS)
### Resultado (DSK-053, bug)
- CUANDO se cierra una pelea, la central DEBERÁ abrir el resultado sin congelar la interfaz: en macOS, en ventana maximizada (macOS no permite pantalla completa a una ventana con dueño); en Windows, a pantalla completa como hoy.
- MIENTRAS una pelea está cerrada, la pantalla Resultado DEBERÁ mostrar «Ver resultado», que abre la ventana con Exportar PDF, CSV y Excel.
- Windows queda sin verificar en este equipo (el desarrollo es en macOS); se prueba en la laptop del evento antes de la función.

### Salida del teléfono (AND-032)
- MIENTRAS el teléfono está en Esperando aprobación, Puntuar sin round abierto, «Round N enviado / espera a que la mesa cierre» o Siguiente pelea, y pasan **10 minutos sin ningún mensaje de la central**, el teléfono DEBERÁ preguntar «¿La pelea sigue?» con «Seguir esperando» (reinicia la cuenta) y «Salir de la pelea».
- MIENTRAS hay un round abierto en Puntuar, el teléfono NO DEBERÁ preguntar (el juez está calificando).
- La cuenta se basa en el **tiempo sin contacto con la central**, no en la inactividad de la pelea: descansos, revisión médica o espera entre peleas pueden durar mucho con la central conectada.
- CUANDO el juez elige «Salir de la pelea» y **no** hay tarjetas sin `ACK`, el teléfono DEBERÁ desconectarse, borrar la sesión y volver a Unirme.
- CUANDO hay tarjetas sin `ACK`, el teléfono DEBERÁ decir cuántas y **no** borrarlas (garantía de entrega, principio 1): conserva la sesión y su Outbox, vuelve a Unirme, y si el juez vuelve a unirse a esa pelea las reenvía. Nunca se pierde una tarjeta por salir.
- La cuenta de 10 minutos es una constante del teléfono; con «reducir movimiento» el diálogo aparece igual (no es animación).

### Volver al lugar (DSK-055, AND-034)
- MIENTRAS un lugar está aprobado y desconectado, la pantalla Jueces DEBERÁ ofrecer «Nuevo código» (código de 6 dígitos y QR, como un lugar sin conectar); el código anterior deja de servir.
- CUANDO el mismo teléfono (misma llave y dispositivo) se une con ese código, la central DEBERÁ responder `CODE_INFO` y, al `JOIN`, devolverlo a su lugar aprobado sin pedir aprobación otra vez (como hoy hace con un `JOIN` del mismo dispositivo).
- CUANDO otro teléfono se une con ese código, la central DEBERÁ dejarlo en espera de aprobación y el operador DEBERÁ ver «Cambiar teléfono» (huella nueva frente a la anterior). Al aprobarlo se reemplaza la llave del lugar y queda en la bitácora; el teléfono anterior ya no entra con `HELLO`. Las tarjetas ya recibidas no cambian.
- La central NO DEBERÁ dar `CODE_INFO` de un lugar registrado sin un código emitido para él después del registro (un código viejo sigue sin servir).
- CUANDO el juez escribe un código, el teléfono NO DEBERÁ borrar su lugar guardado hasta recibir `CODE_INFO`; con `CODE_UNKNOWN` conserva el lugar anterior y su Outbox.
- No cambia el protocolo (`core/`): se usan `CODE_LOOKUP`, `CODE_INFO`, `JOIN` y `JOIN_PENDING` como hoy.

### Nombre (DSK-054, AND-033, DOC-002)
- La central DEBERÁ decir **Box Central** en la marca de la barra, el título de la ventana, los encabezados del PDF, CSV y Excel, y el instalador (`jpackage --name "Box Central"`).
- La app del juez DEBERÁ llamarse **Box Juez** (nombre en el teléfono, marca de la barra, términos y textos) y la marca debe caber junto a «Juez N», el chip y el «?» a 390 dp.
- NO cambian (costoso o rompe instalaciones): paquetes `com.sigebox.*`, coordenadas Gradle, el esquema del QR `sigebox://join`, `SharedPreferences`, y la carpeta de datos (`%APPDATA%\SIGEBOX`, `~/.sigebox`): así una laptop o un teléfono ya instalados conservan sus peleas.
- Las pruebas que fijan el texto «SIGEBOX» se actualizan solo con `uak allow` de cris por tarea.

## Investigación del flujo (2026-11-17)
**Coincide con la práctica:**
- Calificar cada round y que un oficial junte las tarjetas para que la mesa sume es lo que piden varias comisiones: en California, Massachusetts e Illinois se recogen al final de cada round; en la BBBofC el réferi las pasa al supervisor «al final de cada round»; en Washington el inspector verifica la suma.
- No mostrar puntos durante la pelea es el estándar; la puntuación abierta del CMB (tras los rounds 4 y 8) es opcional y solo en peleas que la sanción elige.
- El sistema de 10 puntos obligatorio es el estándar de la ABC: el ganador del round lleva 10 y el perdedor 9 o menos.
- Los descuentos los marca el réferi y se aplican a todas las tarjetas: igual que ADR 0005.
- La puntuación electrónica en el box profesional sigue en pruebas (CMB en Ciudad de México, AMB con tabletas); no hay comisión que la exija.

**Preguntas abiertas (no se cambian sin decisión; gana «Requerimientos v1.1»):**
1. **Regla de 10 puntos obligatoria.** Hoy se acepta cualquier par 0–10 (por ejemplo 8–7). Con la regla, al menos una esquina lleva 10.
2. **Round parcial en decisión técnica.** Según fuentes que siguen a la ABC, si la pelea se detiene después del round 4 se califica también el round incompleto; hoy la decisión técnica usa solo los rounds cerrados.
3. **Teléfono que se descompone antes de su tarjeta final.** La central no cierra la pelea sin ella y el runbook manda a papel: es el mismo «atorado», del lado de la central.
4. **México:** no se encontró un reglamento de puntuación de la CDMX ni nacional; cada estado tiene su comisión. Falta el reglamento de la comisión donde se hará el evento.

Fuentes: ABC (abcboxing.com, manual de jueces 2024), BBBofC (bbbofc.com/how-to-score), reglamentos de WA (WAC 36-12-300), CA (4 CCR § 356), MA (523 CMR 14.05), NY (19 NYCRR 211.36), IL (68 Ill. Adm. Code 1370.500), CMB Ucrania sobre puntuación abierta, World Boxing News y WBA sobre puntuación electrónica, Récord (2025) sobre comisiones mexicanas.

## Out of scope
- Las preguntas abiertas 1 a 4 (se deciden aparte).
- Renombrar paquetes, esquema del QR o carpeta de datos.
