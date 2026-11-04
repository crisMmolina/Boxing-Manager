# Protocolo WebSocket v1 · contrato

Un JSON Schema (draft 2020-12) por mensaje: `<type en minúsculas>.schema.json`. El DTO Java equivalente vive en `com.sigebox.core.protocol` y `ProtocolSchemaTest` comprueba que ambos coincidan: cada campo requerido que se quita hace fallar al Schema y también a `Messages.decode`. Hay un ejemplo de cada mensaje en `demo_data/mensajes/<TYPE>.json`.

## Reglas
- Todos los mensajes llevan `v` (= 1), `id` (UUID) y `type`. Los nombres de campo están en inglés camelCase. Los valores enumerados son los del modelo (`ENVIADA`, `TARDIA`, `EN_CURSO`…).
- Los campos `null` se omiten. Opcional quiere decir que no aparece en `required`.
- `Messages.decode` lanza `ProtocolException` en estos casos:
  - el texto no es JSON estándar o no es un objeto
  - `type` es desconocido
  - `v` no es 1
  - `id` no es un UUID
  - falta un campo requerido: nunca se lee como 0 o `false`
  - un campo tiene otro tipo: `"10"` no es entero, `9.5` tampoco, `"yes"` no es booleano, y un valor de enum desconocido es error

  Los campos desconocidos se ignoran, aunque el Schema los rechaza.
- Los records validan las mismas reglas que su Schema: requeridos, longitudes, rangos 0–10 / 1–5 / 1–12 y patrones. Los textos no pueden estar vacíos ni traer caracteres de control o de formato (`\n`, U+0000, U+202E…). La central aplica todo esto sin el validador de Schema.
- La central nunca confía en `seat` ni en `fightId` del mensaje: los compara con la sesión aprobada (CORE-003 `ScoreValidator`, y una prueba en DSK).
- `ACK.ackId` es el `id` del mensaje que se confirma. El `ACK` sale después de persistir. Un mensaje repetido se reconoce por (lugar de la sesión, `id`), no solo por `id`, y recibe el mismo `ACK`.
- `ACK.error` es un código del enum `Ack.Error`, nunca texto del mensaje recibido. Los errores de `decode` tampoco repiten el contenido.
- `STATE` lleva solo las tarjetas del juez que lo recibe y nunca totales ni ganador.
- `SUBMIT_FINAL_CARD.hash` = `FinalCardHash.of(fightId, seat, cards)`, es decir, SHA-256 hex del texto canónico descrito en esa clase.
- El QR lleva `sigebox://join?h=&p=&c=[&f=]` (`JoinUri`): `c` es el código de 6 dígitos del lugar. `JoinUri.parse` sigue leyendo la forma anterior `sigebox://join?h=&p=&s=&seat=&t=[&f=]`; si llegan las dos, manda `c`. `f` (huella SHA-256 del certificado de la central) queda reservado para cuando la conexión sea `wss://`.

## Identidad del teléfono (amenaza Spoofing)
1. El teléfono crea una sola vez un par de llaves EC P-256 en el Android Keystore. La llave privada nunca sale del equipo.
2. En cada conexión, la central manda primero `CHALLENGE{nonce}`: 128 bits aleatorios, que sirven una sola vez.
3. Para unirse, el teléfono manda `JOIN` con `publicKey` y `sig = sign(DeviceProof.joinText(...))` sobre ese nonce. La central verifica la firma y muestra al organizador la huella (`DeviceProof.fingerprint`) junto al nombre para que apruebe.
4. Para reconectarse, el teléfono manda `HELLO` con `sig = sign(DeviceProof.helloText(...))`. La central lo verifica con la llave registrada en el `JOIN`. Capturar un `HELLO` no sirve, porque el siguiente nonce es otro.
5. `SUBMIT_FINAL_CARD` lleva `sig` sobre `DeviceProof.finalCardText(...)`, de modo que el juez no puede negar su tarjeta.
6. `DeviceProof.sign` funciona con una llave del Keystore y también con una llave en memoria (`judge-sim`).

**Lo que debe hacer la central (DSK):**
1. Crear un nonce por conexión con `DeviceProof.newNonce(SecureRandom)` y guardarlo en esa conexión. Construir el texto firmado con el nonce guardado, nunca con `msg.nonce()`; si no coinciden, rechazar.
2. Aceptar un solo `JOIN`/`HELLO` por nonce y cerrar la conexión si falla. También cerrarla si no llega a tiempo. Antes de autenticarse, rechazar cualquier otro tipo de mensaje.
3. En `HELLO`, tomar fightId, lugar y deviceId del registro aprobado y verificar con su llave guardada. Un `JOIN` nunca reemplaza una llave ya registrada.
4. En `SUBMIT_FINAL_CARD`, reconstruir el texto con las tarjetas guardadas de esa sesión y verificar la firma. Si no coincide, avisar al organizador y conservar lo guardado. Repetidos por lugar y pelea: `ALREADY_FINAL`. Nunca usar los bytes de la firma como clave, porque ECDSA es maleable.
5. Limitar los intentos fallidos de token por lugar, no por conexión, y descartar el código al agotarlos. El teléfono muestra su huella para que el organizador la compare con la de la central.
6. Limitar el tamaño del frame WebSocket. `Messages.decode` rechaza más de 16 KiB, más de 8 niveles de anidamiento y números de más de 20 caracteres.

**Lo que queda abierto (aceptado en v1, red local sin internet):** alguien que intercepte activamente el hotspot (ARP spoofing) podría secuestrar una conexión ya autenticada. La solución es `wss://` con el certificado fijado por `f` en el QR.

**Código de 6 dígitos:** el `token` admite 6 caracteres para el código manual. El token del QR debe tener 22 o más caracteres aleatorios (`SecureRandom`). Con el código manual, la central limita los intentos por conexión. El código es el token del lugar: con `c` en el QR o escrito a mano, el teléfono lo manda en `JOIN.token`.

## Código de lugar y fin de evento

`JOIN`, `HELLO` y la prueba de identidad no cambian; estos mensajes se agregan a v1 (ADR 0004) y `v` sigue en 1.

1. El teléfono conecta y recibe `CHALLENGE`.
2. Manda `CODE_LOOKUP` con `code` (exactamente 6 dígitos, `^[0-9]{6}$`). Va antes de `JOIN`.
3. La central contesta una de dos:
   - `CODE_INFO`: el lugar está libre. Trae `code`, `fightId`, `seat`, `redCorner` y `blueCorner`, nunca puntajes. El teléfono sigue con `JOIN`.
   - `CODE_UNKNOWN`: el código no existe, ya se usó o la pelea cerró. A propósito no dice cuál.
4. Después de `FIGHT_CLOSED`, si era la última pelea, la central manda `EVENT_ENDED` y el teléfono vuelve al inicio.

`CODE_INFO`, `CODE_UNKNOWN` y `EVENT_ENDED` solo salen de la central. Límites por conexión: 1 `CODE_LOOKUP` por segundo, y la central cierra después de 5 `CODE_UNKNOWN` (lo implementa DSK-033).

## Android
- `core` corre en Android 8 (API 26). En `src/main` solo se usan APIs de Java disponibles en API 26:
  - Prohibidas: `List.of`/`copyOf`, `String.isBlank`, `HexFormat`, y las sobrecargas de `URLEncoder`/`URLDecoder` con `Charset`.
- **Sin verificar, lo comprueba la primera tarea AND que use `core` en un build real:**
  - **R8.** En builds con R8 probablemente hay que conservar los nombres de los records: `-keep class com.sigebox.core.protocol.** { *; }` y `-keep class com.sigebox.core.model.** { *; }`.
  - **Gson y records.** Por debajo de API 34, Gson podría no reconocer los records desugarizados. En ese caso los llenaría por reflexión sin ejecutar su constructor, y la validación de campos requeridos no correría en el teléfono. La central (JVM) sí la ejecuta.
  - **D8 y `sealed`.** D8 debe compilar las clases Java 17 de `core`, incluida la interfaz `sealed` `Message`. Si no puede, se puede quitar `sealed`: nada depende de él.
  - **Prueba sugerida.** Decodificar en el teléfono cada fixture de `demo_data/mensajes/` y comparar con `Messages.encode`.
