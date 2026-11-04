# Recuperar la conexión

Texto de la ayuda que abre el botón "?" en las dos apps (DESIGN.md v4 §5.8). AND-020 y DSK-026 lo copian tal cual: no requiere internet. Cada app muestra primero su parte y después la otra. Se basa en `docs/runbook.md` §1 y §5.

Reglas de estilo: pasos numerados, una acción por paso, máximo dos líneas. Las palabras en **negritas** son lo que se ve en pantalla.

## En el teléfono del juez

Título: **Recuperar la conexión**

1. **Espera unos segundos.** La app reconecta sola, por lo general en menos de 6 segundos. Tus puntos están guardados y se envían al reconectar.
2. **Revisa el Wi‑Fi.** Abre Ajustes → Wi‑Fi y confirma que estás en la red del evento (el hotspot de la mesa), no en otra.
3. **Si Android pregunta por internet,** pulsa **Continuar** o **Mantener conexión**. La red del evento no tiene internet y así debe ser.
4. **Revisa la IP.** Debe ser la misma que muestra la pantalla de la central en "Dirección para los jueces". Si no, vuelve a escanear el QR de tu lugar.
5. **Vuelve a abrir la app.** Ciérrala y ábrela con la pantalla encendida. Te regresa a donde ibas.
6. **¿Sigue sin conectar?** Avisa a la mesa y di tu número de juez. No borres la app ni sus datos.

## En la central (laptop)

Título: **Recuperar la conexión**

1. **El hotspot está encendido.** Revisa que el teléfono de la mesa tenga la Zona Wi‑Fi prendida y la pantalla sin bloqueo por inactividad.
2. **La laptop está en la misma red.** Su Wi‑Fi debe estar en el hotspot del evento. Desconecta el cable de red con internet si lo tiene.
3. **La IP es la correcta.** En **Jueces**, la dirección elegida debe ser la de la laptop en el hotspot. Si cambiaste de red, elige de nuevo la IP y que los jueces escaneen otra vez.
4. **El cortafuegos de Windows deja pasar a SIGEBOX.** Debe existir la regla "BoxCentral WS 8765" (puerto 8765). Si no, créala como administrador (ver `docs/runbook.md` §1, paso 4) o pide a quien instaló la laptop que lo haga.
5. **Vuelve a abrir la central.** Ciérrala y ábrela. Los datos del evento están guardados y los teléfonos se reconectan solos.
6. **Un solo juez desconectado:** pídele que siga los pasos de su teléfono. Sus puntos llegan cuando se reconecte.

## Texto corto para avisos

- Teléfono: "Sin conexión. Tus puntos se guardan y se envían al reconectar." + "? Cómo reconectar".
- Central: "Juez 2 sin conexión" + "? Cómo reconectar".
