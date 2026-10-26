# Runbook del evento · SIGEBOX

Guía para el día de la función: montar la red sin router ni internet, instalar las dos apps sin tienda, y qué hacer si algo falla. Basado en lo medido en `docs/spikes/AND-002.md` (hotspot y Android 16) y `docs/spikes/DSK-002.md` (servidor, firewall y jpackage).

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
- La app se llama **SIGEBOX Juez** y pide la cámara para leer el QR.
- **Lo normal es unirse con el QR.** Si la cámara falla, usa la entrada manual de la app: **IP** (la "Dirección para los jueces" de la central), **Pelea** (el **Código de la pelea** de la tarjeta Pelea, p. ej. `N5JZAN`), **lugar** y el **código de 6 dígitos** de ese lugar. No confundas los dos códigos: el de la pelea tiene letras y es el mismo para todos; el de 6 dígitos es de cada lugar.

## 4. Antes de la primera pelea (≈ 5 min)

1. Abre la central y acepta los términos (solo la primera vez).
2. **Pelea:** escribe las esquinas, los rounds y el número de jueces, y crea la pelea.
3. En el selector de IP, elige **la IP de la laptop en el hotspot** (en la prueba, `10.202.x.x`). Si eliges otra (cable, VPN, WSL) el QR apunta mal y nadie se une.
4. **Jueces:** cada juez abre la app, acepta los términos, escanea el QR de su lugar y pone su nombre. En la central compara la **huella corta** del teléfono y pulsa **Aprobar**.
5. Regla para los jueces: **no salir de la app ni apagar la pantalla** durante la pelea. La pantalla de puntuar no se apaga sola. Si salen o la apagan, Android corta la red a los ~7 s: no se pierde nada (al volver reconecta en ≤ 6 s y reenvía), pero mientras tanto no reciben avisos ni la vibración de round cerrado.

## 5. Durante la pelea

- **En vivo:** `Abrir round N` / `Cerrar round N`, o `Ctrl+Enter`. El tablero muestra cada tarjeta en cuanto llega y "Va ganando" (solo en la laptop; los teléfonos nunca ven totales).
- **Falta un juez al cerrar:** la central pide confirmar y escribir **AVANZAR**. El teléfono de ese juez vibra y le exige calificar; su tarjeta llega marcada **Tardío**.
- **Corregir o capturar desde papel:** "Corregir un round" → Corrección, con motivo. Queda en la bitácora.
- **Terminación extraordinaria (KO, descalificación, etc.):** enlace **Terminación**, con doble confirmación y motivo.
- **Cierre:** tras el último round, "Cerrar pelea" → **Resultado**: pantalla completa y Exportar PDF/CSV.

## 6. Si algo falla

| Qué pasa | Qué hacer |
|---|---|
| Un juez sale "sin conexión" | Que vuelva a abrir la app con la pantalla encendida. Reconecta sola en ≤ 6 s y reenvía lo pendiente. Si no, revisa que su Wi-Fi siga en el hotspot y pulsa Continuar en el aviso |
| Se cae el Wi-Fi de un juez | Sigue calificando: la app guarda la tarjeta y la envía sola al volver la red (probado con 40 s y 60 s de corte) |
| La laptop pierde el hotspot cada pocos segundos | Desconecta el cable de red (§1.2) |
| Nadie se puede unir | ¿La laptop está en el hotspot? ¿La IP elegida en la central es la del hotspot? ¿Existe la regla de firewall (§1.4)? |
| Se cierra o se cuelga la central | Ábrela otra vez. Recupera la pelea desde SQLite en el mismo round; los jueces reconectan solos (probado en QA-001 con reinicio a mitad de pelea) |
| Se apaga el teléfono hotspot | Préndelo con la misma red y contraseña. Los teléfonos y la laptop vuelven solos; las tarjetas pendientes se reenvían |
| Se descompone el teléfono de un juez | El organizador captura sus tarjetas de papel con **Corrección** (motivo "captura desde papel") |
| Nada funciona | **Volver a papel** (abajo) |

### Volver a papel

1. Avisa a los jueces: siguen en **tarjetas de papel** desde el round actual.
2. Lo ya recibido no se pierde: queda en SQLite y en la bitácora de la central.
3. Al terminar, captura en **Corrección** lo que falte, con motivo, o calcula a mano.
4. Si el resultado de la central no coincide con el papel, **gana el papel** (plan del spec 001, Hito 5).
