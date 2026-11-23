# IDEA · Box Central y Box Juez

> Proyecto: app desde cero (no es challenge) · Cliente: organizador de funciones de box profesional · Equipo: desarrolladores dueños del software · Fecha de corte: 24 oct 2026.
> Tags: **[Decided]** · **[Recommended]** · **[To validate]**.
> Fuente completa: documento "Requerimientos v1.1: app de puntuación de box" (IDs RO-xx, RJ-xx, RC-xx). Si este archivo y ese documento chocan, gana el documento y se registra con `uak decision`.
> Todo número de esta idea sale de §10 con fuente. §4 → tareas verticales · §6 → contrato · §7 → spikes · §8 → aceptación.

## 0. Name and brand (fixed at T0)
- Name: **Box Central** (central) y **Box Juez** (app del juez) [Decided] · antes SIGEBOX; cambio decidido por cris (spec 008 §Nombre, DSK-054 y AND-033): solo cambian los nombres visibles, los identificadores técnicos `sigebox` (repo, paquetes, esquema `sigebox://`, carpeta de datos) siguen igual · repo: `sigebox` (monorepo Gradle: `core/`, `desktop/`, `android/`, `tools/`, `docs/`) · dominio: no aplica, no hay web.
- Tagline: **Tarjetas de box sin papel ni internet** (7 palabras).
- Logotipo: ninguno por ahora [Decided]; pantallas y PDF reservan un espacio para él.
- Renombrar después de T−50% requiere un `uak decision` humano.

## 1. One line
Para el **organizador de una función de box profesional**, que hoy recoge tarjetas en papel y suma a mano **hasta 120 calificaciones por pelea (5 jueces × 12 rounds × 2 esquinas)**, **Box Central** (y **Box Juez** en el teléfono de cada juez) le permite **recibir la calificación de cada juez en su laptop en 1 segundo o menos y obtener el resultado calculado al cerrar la pelea, todo sin internet ni router**, a diferencia de **las tarjetas de papel y la calculadora**.

Diferencia frente a otras soluciones: funciona 100% en red local creada por un teléfono, con recuperación total si se cae la red o se cierra una app, y el resultado no se muestra en vivo para no influir. [To validate: revisar apps de puntuación existentes (por ejemplo, Controlboxing) y confirmar cuáles dependen de internet.]

## 2. Users
| Persona | Moment | Need | Flow step |
|---|---|---|---|
| Organizador / operador central (laptop Windows) | Antes y durante cada pelea, en la mesa de control | Configurar la pelea, aprobar jueces, controlar rounds, ver quién falta, cerrar y obtener el resultado sin hacer cuentas | 1, 2, 4, 6 |
| Juez (teléfono Android) | Al final de cada round, junto al ring | Calificar rápido con número 0–10, con una mano, sin ver tarjetas ajenas y sin perder lo enviado si falla la red | 2, 3, 4, 5 |
| Anunciador | Al terminar la pelea | Recibir del operador el resultado y los totales por juez para leerlos | 6 |

## 3. Demo case
No hay caso público: es un producto nuevo. Se usa una **pelea de referencia fija** (`demo_data/pelea_referencia.json`) con resultado calculado a mano, que recorre todo el flujo y los casos difíciles:

- Roja vs Azul · 4 rounds · 3 jueces.

| Juez | R1 | R2 | R3 | R4 | Total Roja–Azul | Gana |
|---|---|---|---|---|---|---|
| Juez 1 | 10–9 | 9–10 | 10–9 | 10–9 | 39–37 | Roja |
| Juez 2 | 9–10 | 9–10 | 10–9 (tardío) | 9–10 | 37–39 | Azul |
| Juez 3 | 10–9 | 10–9 | 9–10 | 10–9 | 39–37 | Roja |

- Resultado esperado: **Gana Roja por decisión dividida (2–1)**.
- En el round 3 el Juez 2 no envía antes del cierre: se fuerza el cierre (doble confirmación), su teléfono vibra y le exige calificar en ese momento, y el envío llega marcado tardío.
- Por qué este caso: cubre aprobación, captura, cierre forzado, envío tardío, decisión dividida y PDF. La validación real es una función completa con papel en paralelo (Hito 5).

## 4. Demo flow (6 steps or fewer, 3 min or less; each step = one vertical task)
Transporte: WebSocket + JSON en red local (puerto 8765 por defecto). En la central, la UI JavaFX llama servicios internos; los teléfonos solo hablan por WebSocket.

| # | User does | Sees on screen | API | Prio |
|---|---|---|---|---|
| 1 | Organizador crea la pelea: esquinas, 4 rounds, 3 jueces | Un QR y un código de 6 dígitos por lugar de juez (Juez 1, 2, 3) | `FightService.create` · URI `sigebox://join?h=&p=&s=&seat=&t=` | P0 |
| 2 | Juez escanea el QR y escribe su nombre; el organizador toca Aprobar | Teléfono: "Esperando aprobación" → pantalla de puntuación. Central: "Juez 2 · María López · Conectado" | WS `JOIN` → `JOIN_PENDING` / `JOIN_APPROVED` | P0 |
| 3 | Organizador abre el round; juez teclea 10 y 9 y toca Enviar round | **Wow:** sin internet, la casilla del juez en la central pasa a "Enviado" en 1 s o menos; no aparecen totales ni ganador | WS `ROUND_OPENED` · `SUBMIT_ROUND` → `ACK` | P0 |
| 4 | Organizador cierra el round 3 con el Juez 2 faltante: confirma y escribe AVANZAR | Teléfono del Juez 2 vibra y muestra una ventana obligatoria para calificar; la central marca "Juez 2 · R3 tardío" al recibirlo | WS `ROUND_CLOSED` · `SUBMIT_ROUND` (late) → `ACK` | P0 |
| 5 | Se apaga el Wi‑Fi de un juez 60 s y se cierra a la fuerza la app central | Al volver: el juez se reconecta solo, la central reabre en el mismo round y no falta ninguna calificación | WS `HELLO` → `STATE` · recuperación desde SQLite | P0 |
| 6 | Cada juez envía su tarjeta final; el organizador cierra la pelea | Pantalla completa (solo operador): "Gana Roja por decisión dividida 2–1", tarjeta y totales de cada juez con su nombre; botón Exportar PDF/CSV | WS `SUBMIT_FINAL_CARD` · `FIGHT_CLOSED` · `ResultService.compute` · `ExportService.pdf/csv` | P0 |

- Wow en una frase: los jueces califican con su teléfono, sin internet ni router, y nada se pierde aunque se caiga la red o se cierre la app.
- Segundos hasta el wow: ~40 (paso 3).
- Plan B: video grabado de los 6 pasos; simulador de jueces (`tools/judge-sim`) si falla un teléfono; pelea de referencia precargada si no da tiempo de capturar.

**Tareas P0 fuera del flujo de demo** (también son tareas verticales):
- Terminación extraordinaria con motivo (KO, KO técnico, detenida por el médico, abandono, descalificación, decisión técnica, sin decisión, Otro con razón escrita), esquina ganadora, round, tiempo y doble confirmación (RO-08).
- Corrección del juez con el round abierto, con registro de cada versión (RJ-05); corrección o captura desde papel por el organizador con motivo (RO-09).
- Botón discreto "Ver resumen parcial" con confirmación, visible 15 s y registrado en bitácora (RO-07).
- Bitácora de eventos (RO-13) y aceptación de términos y aviso de privacidad (RO-14, RJ-12).

## 5. Technical core
- Método [Decided]: cálculo determinista en el módulo `core` (Java 17 puro, compartido por escritorio y Android):
  - Validación: enteros 0–10 por esquina, sin valor por defecto; se valida en el teléfono y otra vez en el servidor.
  - Total por juez = suma de rounds enviados, tardíos, capturados o corregidos. Ganador por juez = mayor total; igualdad = empate.
  - Resultado final = esquina con más jueces a favor; igualdad de jueces = empate. Tipo: unánime, mayoritaria (los demás dieron empate), dividida (alguien votó por la otra esquina).
  - Terminación extraordinaria: el resultado lo da el motivo; en decisión técnica se calcula con los rounds cerrados.
- Confiabilidad: cada mensaje lleva id único; el servidor es idempotente; se escribe en SQLite (WAL) antes de enviar el `ACK`; el teléfono reenvía hasta recibir `ACK`.
- **No hay IA ni LLM en el producto.** Ningún número del resultado lo produce un modelo; todo sale de `core`.
- Validación honesta: pruebas JUnit 5 de `core` (1 a 5 jueces, empates, tipos de decisión, tardíos, terminaciones); prueba de 12 rounds × 5 jueces contra cálculo a mano; función real con papel en paralelo, reportando coincidencia por pelea (meta: 100%).

## 6. Data (contract in `app/schemas/`)
Contrato: JSON Schema de cada mensaje en `app/schemas/`; los DTO Java equivalentes viven en `core/.../protocol` y `core/.../model`. Las dos definiciones deben coincidir (prueba automática).

| Entity | Key fields | Source (license) |
|---|---|---|
| Pelea | id, esquina roja, esquina azul, rounds (1–12), número de jueces (1–5), estado, fecha | Capturada por el organizador (datos del cliente) |
| Lugar de juez | pelea, número, nombre del juez, dispositivo, huella de credencial, estado de conexión, aprobado por/hora | Capturado al unirse (datos del cliente) |
| Round | pelea, número, estado (pendiente, abierto, cerrado), apertura, cierre, cierre forzado | Generado por la app |
| Tarjeta de round | pelea, juez, round, valor rojo, valor azul, estado (enviada, tardía, capturada, corregida), origen, hora | Capturada por el juez (datos del cliente) |
| Versión de tarjeta | tarjeta, valores anteriores y nuevos, quién (juez u organizador), motivo, hora | Generada por la app |
| Tarjeta final | pelea, juez, huella (hash), hora | Generada por la app |
| Terminación | pelea, motivo, texto de "Otro", esquina ganadora, round, tiempo, observaciones | Capturada por el organizador |
| Resultado | pelea, totales por juez, ganador por juez, resultado final, tipo o motivo | Calculado por `core` |
| Evento de bitácora | pelea, tipo, detalle, hora | Generado por la app |

Mensajes WS: `JOIN`, `JOIN_PENDING`, `JOIN_APPROVED`, `JOIN_REJECTED`, `HELLO`, `STATE`, `ROUND_OPENED`, `ROUND_CLOSED`, `SUBMIT_ROUND`, `ACK`, `SUBMIT_FINAL_CARD`, `FIGHT_ENDED`, `FIGHT_CLOSED`, `PING`, `PONG`.

## 7. AI and external APIs (each = a 30-min spike + a recorded fallback)
No hay IA ni APIs en la nube. Se reemplaza por **dependencias técnicas de riesgo**, cada una con spike de 30 min y respaldo.

| Use | Model/API [To validate] | Key owner | Fallback |
|---|---|---|---|
| Red local sin router | Punto de acceso de un Galaxy A37: que los dispositivos conectados se vean entre sí | Dev Android | Router de viaje doble banda sin internet |
| Servidor local en la central | Javalin 7 (Jetty 12) WebSocket en Windows 11 + regla de firewall | Dev escritorio | Java-WebSocket (servidor mínimo) |
| Escaneo de QR sin internet | CameraX + ML Kit Barcode Scanning con modelo incluido | Dev Android | ZXing Android Embedded · código de 6 dígitos |
| Mantener la conexión en Wi‑Fi sin internet | `ConnectivityManager.bindProcessToNetwork` en Android 16 | Dev Android | Teléfonos sin SIM y datos móviles apagados |
| Recuperación tras cierre forzado | SQLite WAL con sqlite-jdbc (central) y Room (juez) | Dev escritorio / Android | Respaldo JSON por evento en disco |
| Instalador sin Java preinstalado | jpackage (JDK 25) → .msi/.exe; requiere una máquina Windows para construir | Dev escritorio | ZIP portable con runtime incluido |
| PDF del resultado | OpenPDF o Apache PDFBox | Dev escritorio | Exportación HTML imprimible |

## 8. Rubric → evidence
No hay rúbrica de concurso; se usan los criterios de aceptación del cliente.

| Official criterion | Weight | Step/artifact that proves it |
|---|---|---|
| Ninguna calificación enviada se pierde (corte de 60 s, cierre forzado de apps) | Crítico | Paso 5 · prueba de integración con `judge-sim` |
| Resultado igual al cálculo a mano (12 rounds × 5 jueces) | Crítico | Pruebas de `core` · Paso 6 · función con papel en paralelo |
| Solo enteros 0–10, sin valor por defecto; servidor rechaza mensajes manipulados | Crítico | Paso 3 · prueba de contrato |
| Un juez no ve la tarjeta de otro | Crítico | Paso 3 · prueba de `STATE` por juez |
| Round enviado o corregido visible en la central en 1 s o menos | Alto | Paso 3 · medición con `judge-sim` |
| Round sin calificar: vibración y calificación exigida en ese momento; central señala al faltante | Alto | Paso 4 |
| Cierre forzado y terminación extraordinaria con doble confirmación, motivo y bitácora | Alto | Paso 4 · tarea RO-08 |
| Sin totales ni ganador visibles durante la pelea | Alto | Pasos 3–4 · captura de pantalla |
| Emparejar 3 jueces con QR, nombre y aprobación en menos de 2 min | Medio | Paso 2 |
| Resultado a pantalla completa con nombres y exportación PDF/CSV | Medio | Paso 6 |
| Instalación sin tienda ni internet, y todo sin router | Medio | Instalador + APK firmado · prueba en sitio |

## 9. Scope
- **Dentro:** app central Windows 10/11 (JavaFX), app del juez Android 8+ (Java), 1–5 jueces, 1–12 rounds, una pelea activa a la vez, resultado por mayoría, terminación extraordinaria, PDF/CSV, bitácora, recuperación.
- **Out:** macOS (posible después con el mismo código), iPhone, reglamento de la CDMX, licencias, pesaje, médico, reloj y campana, deducciones por faltas, firma electrónica, publicación web, streaming, boletos, proyección del resultado al público.
- **Simulated with `demo_data`:** pelea de referencia (§3) y simulador de 1 a 5 jueces por línea de comandos (`tools/judge-sim`).
- **UI language:** español (México).
- **Ethics/data:** datos solo en los equipos del evento, nunca salen de la red local; aviso de privacidad simplificado al unirse; términos y condiciones aceptados en el primer uso; prohibido usar el sistema para apuestas; las peleas se conservan hasta que el organizador las borre. El software es propiedad de los desarrolladores.
- **Stack [Decided]:** Java en todo. Central: JDK 25 LTS + JavaFX 25 + Javalin 7 + SQLite + ZXing + OpenPDF, empaquetado con jpackage. Juez: Android Studio, Java, minSdk 26, vistas XML + Material, OkHttp, Room, CameraX + ML Kit. Paquete Android: `com.sigebox.juez`.
- **Equipo [Recommended]:** laptop Windows 11; Samsung Galaxy A37 para jueces, uno como punto de acceso y uno de reserva (5 teléfonos para 3 jueces, 7 para 5).

## 10. Citable facts (verified on 24 oct 2026)
| Fact | Source URL |
|---|---|
| Capacidad: hasta 5 jueces y 12 rounds por pelea (de ahí 120 calificaciones) | Requerimientos v1.1, decisión del cliente (interno) |
| JavaFX 25 corresponde al JDK 25 LTS y es gratuito para producción | https://www.oracle.com/java/technologies/downloads/javafx/ |
| Javalin 7 (feb 2026) requiere Java 17 y Jetty 12, e incluye WebSockets | https://javalin.io/news/javalin-7.0.0-stable.html |
| El punto de acceso de Windows 11 exige normalmente una conexión a internet activa | https://allthings.how/turn-windows-11-pc-into-a-mobile-hotspot/ |
| Galaxy A37 en México: 8/256 GB a 9,999 pesos (lanzamiento) | https://www.xataka.com.mx/celulares-y-smartphones/samsung-galaxy-a57-galaxy-a37-lanzamiento-precio-mexico-caracteristicas-especificaciones-ficha-tecnica |
| Galaxy A37: pantalla AMOLED de 6.7", batería de 5,000 mAh | https://www.tusbuenasnoticias.com/amp/noticias/buenas-noticias/2026/04/07/58735-samsung-galaxy-a57-vs-galaxy-a37-cual-vale-la-pena-comprar |
| La nueva LFPDPPP entró en vigor el 21 mar 2025; la autoridad es la Secretaría Anticorrupción y Buen Gobierno | https://www.ey.com/content/dam/ey-unified-site/ey-com/es-mx/technical/tax/documents/ey-law-flash-nueva-lfpdppp.pdf |
