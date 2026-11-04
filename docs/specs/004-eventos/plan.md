# Plan 004 · Eventos: inicio con historial, cierre sin pasos extra y rediseño negro, blanco y dorado

## Design
Tres frentes que se pueden trabajar en paralelo después de la ola 1:

1. **Contratos (`core`, ola 1).**
   - CORE-007 agrega `CODE_LOOKUP`, `CODE_INFO`, `CODE_UNKNOWN`, `EVENT_ENDED` y el QR nuevo (ADR 0004).
   - CORE-008 agrega `Descuento` y su aplicación en `ResultCalculator` (ADR 0005).
   - DSN-004 fija la guía v4, el logo y el texto de la guía de conexión, que comparten las dos apps.
2. **Central (`central/`).** El orden lo dicta quién toca `App.java`, `RoundsScreen` y `ResultService`:
   - Primero el evento: DSK-023 (datos y pantalla del evento) → DSK-029 (Inicio + carga).
   - Luego DSK-030 (quitar Corrección y resumen parcial) → DSK-031 (descuentos en vivo) → DSK-024 (cierre automático y Resultado con sumas) → DSK-032 (todo se actualiza solo, skeletons).
   - En paralelo: DSK-033 (código numérico, QR, `EVENT_ENDED`) y DSK-026 (tema v4, logo en el encabezado, "?").
   - Al final, DSK-027 pasa por todas las pantallas: etiquetas arriba, "Qué sigue" y ningún código visible.
3. **Teléfono (`android/`).**
   - AND-017 (español y tarjeta final automática) → AND-019 (espera de la siguiente pelea y fin de evento).
   - En paralelo: AND-018 (carga + unirse con nombre, código e IP) y AND-020 ("?" con la guía).
   - Al final, AND-021 aplica el tema v4, el ícono, las etiquetas arriba y los skeletons.

Por qué así:
- Los contratos van primero para que el teléfono y la central trabajen contra el mismo esquema (los fixtures sirven de mocks).
- El diseño va antes del pulido para no rehacer pantallas dos veces.
- Las funciones nuevas usan componentes simples y el estilo v4 llega en DSK-026 / AND-021.

**Actualización constante.**
- `server/ServerEvents` ya avisa de envíos, conexiones y tarjetas finales. DSK-031 le agrega descuentos.
- Cada pantalla visible se suscribe y actualiza con `Platform.runLater`. Como red de seguridad queda un `Timeline` de 1 s, como ya tienen En vivo y Evento.
- No queda ningún botón "Actualizar".

**Tarjeta final automática.** Al recibir `ROUND_CLOSED` del último round, o `STATE` con la pelea terminada y `finalCardSent = false`, el teléfono arma la tarjeta final con sus rounds guardados. La firma y la mete a la `Outbox` como cualquier envío (principio 1). La central no cambia la regla de cierre (`ResultService.canClose`): las tarjetas simplemente llegan solas.

**Pantalla de carga.**
- Teléfono: un tema de arranque con el logo (`windowBackground` con *layer-list*) en `MainActivity`, sin dependencias nuevas.
- Central: una `Stage` sin decoración con el logo mientras `Database` migra y `RecoveryService` reconstruye. Después aparece Inicio.

**Skeletons.**
- Central: un componente `Skeleton` en `ui/theme`, hecho de rectángulos con una animación de opacidad que se apaga con movimiento reducido.
- Teléfono: vistas grises con la forma de la tarjeta mientras llega el primer `STATE`.

Alternativas descartadas:
- Reescribir la central con FXML o una librería de componentes: costoso y fuera de alcance.
- Hacer los cambios por app en una sola tarea grande: no cabe en un día y bloquea a los 4 agentes.

## Data and migrations
`central/src/main/resources/db/V3__eventos.sql` es solo de expansión y se agrega a `Database.MIGRATIONS`:
- `ALTER TABLE funcion ADD COLUMN nombre TEXT` (NULL = "Evento <fecha>" al mostrar). `funcion.date` ya existe: ahora se elige al crear.
- `CREATE TABLE descuento (id INTEGER PRIMARY KEY, fight_id TEXT NOT NULL, round INTEGER NOT NULL CHECK (round BETWEEN 1 AND 12), esquina TEXT NOT NULL CHECK (esquina IN ('ROJA','AZUL')), puntos INTEGER NOT NULL DEFAULT 1 CHECK (puntos = 1), anulado INTEGER NOT NULL DEFAULT 0, motivo_anulacion TEXT, created_ms INTEGER NOT NULL, anulado_ms INTEGER)`, con índice por `fight_id`.

Las tablas y tarjetas de corrección y captura se quedan. Se deja de escribirlas, pero las peleas viejas siguen leyéndose y el acta las muestra.

Revertir: el código viejo ignora la columna y la tabla nuevas. No hay paso de contracción en esta spec.

## Contracts
| Mensaje | Dirección | Campos | Consumidores |
|---|---|---|---|
| `CODE_LOOKUP` | teléfono → central | v, id, type, code (`^[0-9]{6}$`) | `JudgeHub`, `judge-sim` |
| `CODE_INFO` | central → teléfono | v, id, type, code, fightId, seat, redCorner, blueCorner | `JudgeConnection`, `judge-sim` |
| `CODE_UNKNOWN` | central → teléfono | v, id, type, code | `JoinActivity`, `judge-sim` |
| `EVENT_ENDED` | central → teléfono | v, id, type, fightId | `JudgeRuntime`, `judge-sim` |
| QR `sigebox://join` | central → teléfono | h, p, c, [f]; la forma vieja s/seat/t sigue válida | `QrParser`, `SetupScreen` |
| `ResultCalculator.calcular(..., descuentos)` | `core` → central | sumas por juez con descuentos | `ResultService`, `export/` |

- Cada mensaje existe tres veces: esquema, record en `com.sigebox.core.protocol` y fixture en `demo_data/mensajes/`. `ProtocolSchemaTest` lo verifica.
- Antes de hacer push, CORE-007 y CORE-008 mandan `uak msg impact:ID --kind contract`.

## Threat model (STRIDE)
Aplica porque se toca el emparejamiento (identidad del dispositivo) y el resultado.

| Threat | Vector | Mitigation | Test |
|---|---|---|---|
| Spoofing | Adivinar un código de 6 dígitos y ocupar el lugar de un juez | El código es de un solo uso y solo de lugares libres de peleas abiertas. Se permite 1 búsqueda/s por conexión y la conexión se cierra tras 5 `CODE_UNKNOWN`. La aprobación del organizador sigue siendo obligatoria y `JOIN` sigue firmado. | `JudgeHub` cierra la conexión al 5.º código malo; un código ya usado da `CODE_UNKNOWN` (DSK-033) |
| Tampering | Alterar un descuento o borrarlo para cambiar el resultado | Solo la central crea descuentos. Anular pide motivo y nunca borra. Todo queda en la bitácora. El cálculo lo hace solo `core`. | El descuento anulado sigue en la tabla y en la bitácora; el resultado no lo cuenta (DSK-031, CORE-008) |
| Repudiation | La tarjeta final ya no la confirma el juez a mano | Sigue firmada con la llave del dispositivo y su hash. La bitácora dice "enviada automáticamente". El juez confirma cada round, como hoy. | La tarjeta final llega con firma válida sin acción del juez (AND-017) |
| Information disclosure | `CODE_INFO` revela esquinas a quien adivine un código | Solo da nombres de esquinas, que son públicos en el evento, y solo de lugares libres. Nunca da puntos ni totales (principio 3). | `CODE_INFO` no tiene campos de puntos (esquema, CORE-007) |
| Denial of service | Inundar la central con `CODE_LOOKUP` | Límite por conexión y cierre; los mensajes se validan antes de procesar | Prueba del límite (DSK-033) |
| Elevation of privilege | Un teléfono manda `EVENT_ENDED` o un descuento | La central rechaza cualquier tipo que solo manda la central (como hoy con `STATE`) | `JudgeHub` rechaza `EVENT_ENDED` entrante (DSK-033) |

## Observability
- Bitácora local: `CODE_UNKNOWN` (sin el código completo, solo los últimos 2 dígitos), conexión cerrada por intentos, `DESCUENTO`, `DESCUENTO_ANULADO`, `TARJETA_FINAL_AUTO`, `EVENT_ENDED`. Todos traducidos en `ui/Labels`.
- `judge-sim` sigue midiendo envío → `ACK` (p50/p95). QA-004 agrega la latencia del último round → Resultado listo.
- Sin telemetría ni datos fuera de la red local (principio 4).

## Rollout and rollback
- Sin bandera: no hay usuarios remotos y cada evento se instala completo (APK + instalador). Las dos apps se publican juntas en la misma versión.
- Criterio para pasar al evento real: QA-004 en verde y un ensayo con 3 teléfonos reales y un evento de 2 peleas (`docs/demo.md`).
- Revertir: instalar la versión anterior de las dos apps. La migración V3 es compatible hacia atrás y los mensajes nuevos son aditivos (ADR 0004 y 0005).

## Cost
$0: sin nube ni APIs. El logo debe tener licencia libre verificable (CC0 o dominio público). Antes de descargar nada, DSN-004 pide a cris que elija la imagen.

## Tasks
| Ola | Tareas |
|---|---|
| 1 | CORE-007 · CORE-008 · DSN-004 · DSK-025 · DSK-028 |
| 2 | DSK-023 · AND-017 · AND-018 · AND-020 · DSK-026 · DSK-033 |
| 3 | DSK-029 · AND-019 |
| 4 | DSK-030 · AND-021 |
| 5 | DSK-031 |
| 6 | DSK-024 |
| 7 | DSK-032 |
| 8 | DSK-027 · QA-004 |
| 9 | DOC-001 |

Las de spec 003 que chocaban (DSK-023, DSK-024, DSK-026, DSK-027, AND-017 y DOC-001) se reescriben con los mismos ID. Cada tarea empieza con una prueba que falla. `uak graph` y `uak lint` pasan.
