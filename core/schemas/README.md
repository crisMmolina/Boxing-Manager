# Protocolo WebSocket v1 · contrato

Un JSON Schema (draft 2020-12) por mensaje: `<type en minúsculas>.schema.json`. El DTO Java equivalente vive en `com.sigebox.core.protocol` y `ProtocolSchemaTest` comprueba que ambos coincidan: cada campo requerido que se quita hace fallar al Schema y también a `Messages.decode`. Hay un ejemplo de cada mensaje en `demo_data/mensajes/<TYPE>.json`.

## Reglas
- Todos los mensajes llevan `v` (= 1), `id` (UUID) y `type`. Los nombres de campo están en inglés camelCase. Los valores enumerados son los del modelo (`ENVIADA`, `TARDIA`, `EN_CURSO`…).
- Los campos `null` se omiten. Opcional quiere decir que no aparece en `required`.
- `Messages.decode` lanza `ProtocolException` en estos casos:
  - el texto no es JSON o no es un objeto
  - `type` es desconocido
  - `v` no es 1
  - `id` no es un UUID
  - falta un campo requerido: nunca se lee como 0 o `false`

  Los campos desconocidos se ignoran, aunque el Schema los rechaza.
- `Messages.decode` no revisa rangos (0–10, lugar 1–5, round 1–12). Los revisan el Schema y `com.sigebox.core.validation` (CORE-003).
- La central nunca confía en `seat` ni en `fightId` del mensaje: los compara con la sesión aprobada.
- `ACK.ackId` es el `id` del mensaje que se confirma. El `ACK` sale después de persistir. Si el servidor recibe dos veces el mismo `id`, responde el mismo `ACK`.
- `STATE` lleva solo las tarjetas del juez que lo recibe y nunca totales ni ganador.
- `SUBMIT_FINAL_CARD.hash` = `FinalCardHash.of(fightId, seat, cards)`, es decir, SHA-256 hex del texto canónico descrito en esa clase.
- El QR lleva `sigebox://join?h=&p=&s=&seat=&t=` (`JoinUri`).

## Android
- `core` corre en Android 8 (API 26). En `src/main` solo se usan APIs de Java disponibles en API 26:
  - Prohibidas: `List.of`/`copyOf`, `String.isBlank`, `HexFormat`, y las sobrecargas de `URLEncoder`/`URLDecoder` con `Charset`.
- En builds con R8 hay que conservar los nombres de los records: `-keep class com.sigebox.core.protocol.** { *; }` y `-keep class com.sigebox.core.model.** { *; }`.
- Por debajo de API 34, Gson no reconoce los records desugarizados: los llena por reflexión y no ejecuta su constructor. Por eso, en el teléfono, la validación de campos requeridos no corre al decodificar. La central (JVM) sí la ejecuta.
