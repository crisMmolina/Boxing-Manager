# Spec 001 · SIGEBOX v1 (central + juez + core)
- Status: approved (cris, 2026-10-24) · Owner: cris (app/), zoe (BOXCENTRAL/) · PROJECT principles: 1–6
- Fuente: `IDEA.md` y "Requerimientos v1.1" (IDs RO-xx organizador, RJ-xx juez, RC-xx cálculo). Si chocan, gana el documento.

## Problem
El organizador de una función de box profesional recoge tarjetas en papel y suma a mano hasta 120 calificaciones por pelea (5 jueces × 12 rounds × 2 esquinas). Es lento, propenso a errores y no hay internet confiable en la arena.

## Stories
- Como organizador, quiero crear una pelea y emparejar a los jueces con un QR, para empezar en menos de 2 min.
- Como organizador, quiero ver quién envió y quién falta en cada round, sin ver totales, para no influir.
- Como organizador, quiero cerrar la pelea y ver el resultado calculado y exportarlo, para dárselo al anunciador.
- Como juez, quiero calificar con una mano (0–10 por esquina) y que lo enviado no se pierda aunque falle la red.
- Como anunciador, quiero recibir el resultado y los totales por juez con su nombre.

## Acceptance criteria (EARS)
- When el organizador crea una pelea (esquinas, 1–12 rounds, 1–5 jueces), the system shall mostrar un QR y un código de 6 dígitos por lugar de juez.
- When un juez escanea el QR (o escribe el código) y su nombre, the system shall mostrarle "Esperando aprobación" y, al aprobarlo el organizador, la pantalla de puntuación (RO-03, RJ-01).
- When el juez envía un round con el round abierto, the system shall marcar "Enviado" en la central en ≤ 1 s y no mostrar totales ni ganador.
- If un valor no es entero 0–10 o falta, the system shall rechazarlo en el teléfono y otra vez en el servidor (mensaje manipulado → rechazo y bitácora).
- When el organizador cierra un round con jueces faltantes, the system shall pedir doble confirmación (escribir AVANZAR), hacer vibrar el teléfono del faltante con una ventana obligatoria y marcar su envío como tardío.
- While el round está abierto, the system shall permitir al juez corregir y registrar cada versión (RJ-05).
- When el organizador corrige o captura desde papel, the system shall exigir motivo y registrar la versión (RO-09).
- If se corta el Wi‑Fi de un juez o se cierra a la fuerza cualquier app, the system shall reconectar solo (`HELLO` → `STATE`), reabrir en el mismo round y no perder ninguna calificación.
- While la pelea está en curso, the system shall enviar a cada juez solo su propio estado (nunca tarjetas ajenas).
- When el organizador toca "Ver resumen parcial" y confirma, the system shall mostrarlo 15 s y registrarlo en bitácora (RO-07).
- When el organizador registra una terminación extraordinaria (KO, KOT, médico, abandono, descalificación, decisión técnica, sin decisión, Otro + razón), the system shall pedir esquina ganadora, round, tiempo y doble confirmación (RO-08).
- When todos envían su tarjeta final y el organizador cierra la pelea, the system shall mostrar a pantalla completa (solo operador) el resultado ("Gana Roja por decisión dividida 2–1"), tarjetas y totales con nombres, y exportar PDF/CSV.
- The system shall calcular: total por juez = suma de rounds; ganador por juez = mayor total (igual = empate); final = esquina con más jueces (igual = empate); tipo unánime / mayoritaria / dividida; decisión técnica con rounds cerrados (RC-xx).
- When se usa por primera vez, the system shall pedir aceptar términos y aviso de privacidad (RO-14, RJ-12) y registrar todo en bitácora (RO-13).

## Non-functional
Ver `.uak/PROJECT.md` §NFR: ≤ 1 s, 0 pérdidas, 100 % coincidencia, 1–5 jueces × 1–12 rounds, emparejar 3 en < 2 min, 80 % cobertura en `core`. UI en español (México).

## Out of scope
macOS/iPhone como objetivo, reglamento CDMX, licencias, pesaje, médico, reloj y campana, deducciones por faltas, firma electrónica, web, streaming, boletos, proyección al público, apuestas.

## Open questions
- [ ] ¿Las apps de puntuación existentes (p. ej. Controlboxing) dependen de internet? · cris
- [ ] ¿El hotspot del Galaxy A37 deja que los clientes se vean entre sí? (spike AND-002) · cris
