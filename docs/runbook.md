# Runbook del evento · SIGEBOX

Guía para el día del evento: montar la red sin router ni internet, instalar las dos apps sin tienda, y qué hacer si algo falla. Basado en lo medido en `docs/spikes/AND-002.md` (hotspot y Android 16) y `docs/spikes/DSK-002.md` (servidor, firewall y jpackage).

## 0. Qué llevar

- **Laptop central** con Windows 11, cargador y la central instalada (§2).
- **Teléfono hotspot:** uno dedicado, sin SIM o con datos móviles apagados. Probado: **Honor 200**. El **Galaxy A37 no está probado**: si se usa, repite §1 con él antes del evento.
- **Un teléfono Android por juez** (Android 8.0 o superior) con la app del juez instalada (§3), cargados y con cargador.
- **Tarjetas de papel y pluma** para cada juez: son el respaldo (§6).
- Opcional: la APK y el instalador de la central en una USB, por si hay que reinstalar.

## 1. Montar la red (≈ 10 min)

1. **Teléfono hotspot:** apaga los datos móviles (o quita la SIM) y prende *Zona Wi-Fi / Hotspot personal* con contraseña **WPA2** en **2,4 GHz**. Si tiene opción de apagarse por inactividad, **desactívala**.
2. **Laptop central:** conecta su Wi-Fi al hotspot. **No conectes cable de red con internet.** Con cable, Windows suelta cada 16–30 s el Wi-Fi que no tiene internet y los jueces se desconectan. Si de verdad necesitas el cable, antes aplica como administrador:
   ```powershell
   New-Item -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WcmSvc\GroupPolicy" -Force | Out-Null
   Set-ItemProperty -Path "HKLM:\SOFTWARE\Policies\Microsoft\Windows\WcmSvc\GroupPolicy" -Name "fMinimizeConnections" -Value 0 -Type DWord
   ```
   y **reinicia la laptop** (no reinicies el servicio `WcmSvc` a mano: se queda trabado y apaga el Wi-Fi).
3. Si Windows dice "No es posible conectarse a esta red" al primer intento, vuelve a intentarlo: en la prueba el segundo intento funcionó.
4. **Firewall (una sola vez por laptop),** en PowerShell como administrador:
   ```bat
   netsh advfirewall firewall add rule name="BoxCentral WS 8765" dir=in action=allow protocol=TCP localport=8765 profile=any
   ```
   Para quitarla después: `netsh advfirewall firewall delete rule name="BoxCentral WS 8765"`.
5. **Cada teléfono de juez:** conéctalo al hotspot. Android 16 avisa *"… no está conectado a Internet. ¿Continuar usándolo?"*: pulsa **Continuar**. Apaga "Conectar automáticamente" en cualquier otra red Wi-Fi conocida del lugar, para que no se cambie de red.
6. Los datos móviles de los jueces pueden quedarse encendidos: la app manda todo por el Wi-Fi del hotspot.

## 2. Instalar la central sin internet

Se instala antes del evento; en el evento no hace falta internet.

- **Con instalador `.msi`:** ejecútalo y sigue el asistente; crea acceso en el menú Inicio y no necesita Java instalado. **Aún no se ha generado ni probado un `.msi`**: construirlo pide WiX en la máquina de build (ver `README.md`). Mientras tanto, usa el plan B.
- **Sin `.msi` (plan B):** copia la carpeta *app-image* (`SIGEBOX Central/`, trae su propio Java; el del spike pesó 128 MB) y abre `SIGEBOX Central.exe`.
- La central escucha en el puerto **8765** de todas las interfaces. Los datos quedan en SQLite en la laptop.

## 3. Instalar la app del juez sin tienda

- Pasa la APK (`app-debug.apk` o la de release) al teléfono por cable USB, USB-OTG o Bluetooth, ábrela y permite "Instalar apps desconocidas" para el administrador de archivos.
- Con depuración USB: `adb install -r app-debug.apk`.
- La app se llama **SIGEBOX Juez** y pide la cámara para leer el QR. Sin cámara se puede unir escribiendo el código (§4).

## 4. Antes de la primera pelea (≈ 5 min)

1. Abre la central y acepta los términos (solo la primera vez).
2. **Inicio** → **Nuevo evento:** pon la fecha, los rounds por pelea, los jueces y el número de peleas (el nombre es opcional) y pulsa **Crear evento**.
3. **Evento:** en la **Cartelera** escribe la esquina roja y la azul de cada pelea. Ordénalas con **Subir** y **Bajar**, o usa **Saltar**, **Quitar** y **Agregar pelea**. Pulsa **Iniciar pelea**.
4. **Jueces:** en **Dirección para los jueces**, elige **la IP de la laptop en el hotspot** (en la prueba, `10.202.x.x`). Si eliges otra (cable, VPN, WSL) el QR apunta mal y nadie se une.
5. Cada juez se une a su lugar (abajo). En la central compara la **Huella del teléfono** con la que muestra el juez y pulsa **Aprobar**, o **Rechazar** si no coincide.
6. Con todos aprobados, pulsa **Iniciar pelea**. El botón sigue apagado mientras falte alguien.
7. Regla para los jueces: **no salir de la app ni apagar la pantalla** durante la pelea. La pantalla de puntuar no se apaga sola. Si salen o la apagan, Android corta la red a los ~7 s: no se pierde nada (al volver reconecta en ≤ 6 s y reenvía), pero mientras tanto no reciben avisos ni la vibración de round cerrado.

### Unirse con código

Cada lugar de **Jueces** tiene su QR y su **Código** de 6 dígitos. En el teléfono, tras aceptar los términos, la pantalla **Unirme a la pelea** pide:

- **Nombre** del juez.
- **Código** de su lugar, o **Escanear QR** para llenarlo con la cámara.
- **IP de la central:** la **Dirección para los jueces**.

Al pulsar **Unirme**, el teléfono queda en **Esperando aprobación** y muestra su huella hasta que la mesa lo apruebe.

| Aviso en el teléfono | Qué hacer |
|---|---|
| "Ese código no es válido" | Dicta otra vez el código de ese lugar. Si no funciona, pulsa **Nuevo código** en la central: el anterior ya no sirve |
| "No encontramos la central" | Revisa la IP y que el teléfono siga en el hotspot (guía "?", §6) |
| "No fuiste aprobado" | La mesa lo rechazó: revisa que sea el teléfono correcto y únete de nuevo |

La pantalla Jueces solo muestra el código de 6 números de cada lugar. Si no hay pelea que emparejar, **Ir a Evento** lleva a la cartelera para iniciar la siguiente.

## 5. Durante la pelea

- **En vivo:** `Abrir round N` / `Cerrar round N`, o `Ctrl+Enter`. El tablero marca a cada juez como Pendiente, Enviado, Tardío o Corregido en cuanto llega su tarjeta, **sin puntos, totales ni ganador**.
- **Falta conectar un juez:** `Abrir round N` queda desactivado y dice cuál falta. Si su lugar está vacío y no va a llegar, baja los jueces en **Evento** (solo antes del round 1). Si el juez se cayó, que abra SIGEBOX en su teléfono: se reconecta solo.
- **Falta un juez al cerrar:** la central pide confirmar y escribir **AVANZAR**. El teléfono de ese juez vibra y le exige calificar; su tarjeta llega marcada **Tardío**.
- **Corregir una tarjeta:** solo el juez puede corregir la suya, desde su teléfono; llega marcada **Corregido**. La mesa no captura ni cambia puntos.
- **Terminación extraordinaria (KO, descalificación, etc.):** **Terminar pelea** en En vivo. Pide solo el motivo (y la esquina o la razón si el motivo las pide); el round y el tiempo los pone la central. Al confirmar, la pelea se cierra con su resultado y se abre su hoja en **Resultado**.
- **Bitácora:** el enlace **Bitácora** muestra todo lo que pasó, con filtros por pelea y tipo.

### Descuentos

Un descuento le quita 1 punto a una esquina en un round. Se aplica en **En vivo**, con la pelea en curso:

1. Elige el **Round** y pulsa **−1 roja** o **−1 azul**. No hay límite de descuentos.
2. Confirma "Quitar 1 punto a la esquina …".
3. El punto se resta a esa esquina en ese round, en la tarjeta de cada juez. Los jueces no ven los descuentos.
4. Si fue un error, pulsa **Anular** y escribe el **Motivo**. El descuento no se borra: queda anulado y en la bitácora.

### Fin de la pelea

1. Al cerrar el último round la pelea termina sola, y cada teléfono envía solo su tarjeta final firmada. Un teléfono desconectado la envía al reconectar.
2. En **En vivo**, **Cerrar pelea** lleva a **Resultado**. Ahí se ven los puntos de cada juez round por round, los descuentos y la suma, sin ganador ni tipo de decisión.
3. Con todas las tarjetas finales, pulsa **Cerrar pelea**. Si falta alguna, la pantalla dice "Aún sin tarjeta final" y el juez que falta.
4. En la ventana **Resultado final**, usa **Exportar PDF** o **Exportar CSV**: ahí van el ganador y el tipo de decisión.
5. Inicia la siguiente pelea. Mientras tanto, el teléfono dice **Espera la siguiente pelea**. Tras la última dice **El evento terminó** y ofrece **Volver a Unirse**.

## 6. Si algo falla

Primero, la guía **"?"** (**Recuperar la conexión**). Está en la central y en el teléfono, y el aviso de desconexión la abre con **? Cómo reconectar**. Muestra los pasos del teléfono y de la central; en el teléfono también la IP guardada y el estado de la conexión.

| Qué pasa | Qué hacer |
|---|---|
| Un juez sale "sin conexión" | Que vuelva a abrir la app con la pantalla encendida. Reconecta sola en ≤ 6 s y reenvía lo pendiente. Si no, sigue la guía "?": revisa que su Wi-Fi siga en el hotspot y pulsa Continuar en el aviso |
| Se cae el Wi-Fi de un juez | Sigue calificando: la app guarda la tarjeta y la envía sola al volver la red (probado con 40 s y 60 s de corte) |
| La laptop pierde el hotspot cada pocos segundos | Desconecta el cable de red (§1.2) |
| Nadie se puede unir | ¿La laptop está en el hotspot? ¿La IP elegida en la central es la del hotspot? ¿Existe la regla de firewall (§1.4)? |
| Se cierra o se cuelga la central | Ábrela otra vez. Recupera la pelea desde SQLite en el mismo round; los jueces reconectan solos (probado en QA-001 con reinicio a mitad de pelea) |
| Se apaga el teléfono hotspot | Préndelo con la misma red y contraseña. Los teléfonos y la laptop vuelven solos; las tarjetas pendientes se reenvían |
| Se descompone el teléfono de un juez | Ese juez sigue en **tarjetas de papel**. Sin su tarjeta final la central no cierra la pelea: resuélvela con **Volver a papel** |
| Nada funciona | **Volver a papel** (abajo) |

### Volver a papel

La central no captura tarjetas de papel: el plan B es calcular a mano.

1. Avisa a los jueces: siguen en **tarjetas de papel** desde el round actual.
2. Lo ya recibido no se pierde: queda en SQLite y en la bitácora de la central.
3. Al terminar, calcula el resultado a mano con las tarjetas de papel y lo ya recibido, y aplica los descuentos.
4. Si el resultado de la central no coincide con el papel, **gana el papel** (plan del spec 001, Hito 5).
