# ADR 0005 · Descuentos de punto en el resultado
- Status: accepted · Date: 2026-11-03 · Decider: cris · Spec: 004 §En vivo · principio 2

## Context
El réferi puede quitar un punto a una esquina, y eso se puede repetir varias veces en la pelea. Hoy nada en `core`, `central` ni `android` modela un descuento. El principio 2 exige que ningún número del resultado salga de otro lugar que `core`.

## Decision
- **Modelo en `core/model`:** `Descuento(id, fightId, round, esquina, puntos = 1, anulado, motivoAnulacion)`. No hay límite de descuentos.
- **Aplicación:** `ResultCalculator` recibe la lista de descuentos y, por cada uno vigente, resta 1 punto a esa esquina en ese round de la tarjeta de **cada** juez. El puntaje de un round no baja de 0. La firma vieja de `calcular` sigue existiendo y equivale a una lista vacía.
- **Salidas de `core`:**
  - Por juez: puntaje por round ya descontado, descuentos aplicados y suma de cada esquina.
  - Ganador y tipo de decisión con las sumas descontadas, para el acta (PDF/CSV).
- **Central:** el descuento es solo de la central. Se guarda en la tabla `descuento` (SQLite) antes de mostrarse. Para anularlo se pide un motivo, se marca como anulado y nunca se borra. Poner y anular quedan en la bitácora.
- **Lo que no cambia:**
  - Las tarjetas firmadas de los jueces y su `FinalCardHash` siguen siendo lo que el juez marcó. El descuento se aplica al calcular, nunca a la tarjeta guardada.
  - No hay mensaje nuevo: los jueces no ven descuentos y `STATE` no cambia (principio 3).

## Alternatives
- **Restar al total final y no a un round:** el réferi lo marca en un round y el acta debe decir en cuál. No.
- **Cambiar la tarjeta guardada del juez:** rompería la firma y el hash de su tarjeta final. No.
- **Calcular el descuento en la central:** viola el principio 2. No.

## Consequences
- Se agrega un caso de referencia calculado a mano: la pelea de referencia con 2 descuentos (uno de cada esquina, uno de ellos anulado) y 1 descuento repetido en el mismo round. Las pruebas de `core` lo comparan.
- El PDF y el CSV muestran los descuentos.
- Revertir: sin descuentos vigentes, el resultado es el mismo que hoy. La tabla se queda (migración solo de expansión).
