# Spec 009 · UX móvil del juez: lo que la investigación pide aplicar ya
- Status: approved (cris, 2026-11-20: «mandes a hacer un deepsearch sobre los mejores diseños de mobile… mejoren el UX/UI a la brevedad… agrega las tareas y empieza a trabajar») · Owner: cris · PROJECT principles: 1, 2, 3
- Fuente: [investigacion.md](investigacion.md) (Material 3 Expressive, WCAG 2.2, apps de puntuación, factores humanos, legibilidad y conexión, con fuentes).
- Alcance: `android/` y `docs/DESIGN.md`. **No cambia el protocolo** (`core/`) ni el reglamento de puntuación.

## Problem
La v6 ya cumple buena parte de la investigación (pantalla encendida al calificar, escáner ML Kit integrado, «Round N enviado» solo tras el `ACK`, cifras tabulares, sin valor por defecto). Faltan cinco cosas que protegen al juez bajo presión:
1. Si Android cierra la app a mitad del round, el puntaje a medio poner se pierde (solo se guardan las tarjetas enviadas).
2. Enviar pasa por una hoja «¿Enviar este round?» en cada round: las confirmaciones repetidas dejan de leerse a las 2–3 veces.
3. La vibración no dice si la central recibió la tarjeta; TalkBack no anuncia «Round enviado» ni «Sin conexión».
4. El código de 6 dígitos no se agrupa («123 456»), no acepta pegar con espacios y obliga a pulsar Unirme.
5. − y + se aplanan con rebote: Google reserva el rebote para momentos destacados, no para controles repetidos.

## Acceptance criteria (EARS)
### Borrador (AND-035)
- CUANDO el juez toca − o + en un round abierto, el teléfono DEBERÁ guardar el puntaje en Room antes de mostrarlo, y al volver tras un cierre forzado DEBERÁ restaurarlo en el mismo round.
- CUANDO llega el `ACK` de ese round, o el round ya está cerrado, el borrador NO DEBERÁ restaurarse.
- Una esquina sin tocar sigue en «—» (sin valor por defecto, principio 2).

### Confirmación por la central (AND-036)
- CUANDO llega el `ACK` correcto, el teléfono DEBERÁ vibrar con `CONFIRM` (API 30+; `VIRTUAL_KEY` en API 26–29) y anunciar «Round N enviado» a TalkBack; con error, `REJECT` y el anuncio del error.
- La vibración NUNCA es la única señal (desde API 33 el usuario puede apagarla).
- Al perder la conexión, TalkBack DEBERÁ anunciar «Sin conexión» una vez.

### Unirse (AND-037)
- El campo de código DEBERÁ mostrarse agrupado «123 456», aceptar pegar con espacios o guiones y, con nombre escrito, consultar solo al sexto dígito.
- El escáner sigue siendo ML Kit integrado (`barcode-scanning`), que funciona sin internet; no se cambia a Google Code Scanner (módulo descargable).

### Enviar con intención (AND-038)
- La hoja «¿Enviar este round?» DEBERÁ reemplazarse por el resumen grande de la tarjeta junto a «Mantén para enviar»: un anillo se llena en 0,8 s; soltar antes cancela.
- TalkBack y Switch Access DEBERÁN tener una acción «Enviar round» que no exige mantener (WCAG 2.5.7).
- Con «quitar animaciones» el anillo no se anima, pero el tiempo de mantener se respeta.

### Sin rebote (AND-039, DSN-007)
- − y + DEBERÁN responder con el esquema de movimiento estándar (sin rebote visible) y un estado presionado que se vea bajo la luz del ring (no solo la capa del 10 %).
- `docs/DESIGN.md` DEBERÁ recoger las reglas nuevas: movimiento estándar en controles de calificación, números críticos ≥ 7:1, hápticos según el `ACK`, texto hasta 200 %, anuncios de TalkBack.

## Decisiones abiertas (no se implementan sin decisión de cris; gana «Requerimientos v1.1»)
1. Calificar con «ganador + margen» (10-9, 10-8, 10-7) exige adoptar la regla de 10 puntos obligatoria (spec 008, pregunta 1).
2. Admitir o no el 10-10.
3. Umbrales para una segunda confirmación en tarjetas inusuales.
4. Validar en la arena la superficie negra de la v6 con luces bajas (blanco sobre negro en penumbra fue lo menos legible en Dobres 2017).
5. Corrección después del cierre del round, a cargo de la mesa y con motivo. (Antes del cierre ya existe: reenviar `SUBMIT_ROUND` con el round abierto.)

## Out of scope
- «Deshacer» retrasando el envío: rompe la entrega en ≤ 1 s.
- Cambios de protocolo o de la central.
