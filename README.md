# Evidencia de DSK-021 · pantalla Función y "Siguiente pelea"

Capturas de una función de 3 peleas (base 2 jueces; la 2 con 3 jueces; la 3 a 6 rounds) con jueces de judge-sim contra la central real, en tema claro y oscuro, generadas con `FuncionScreenshots`:

```
SIGEBOX_SCREENSHOTS=<carpeta> BOXCENTRAL/gradlew -p BOXCENTRAL test --tests '*FuncionScreenshots'
```

1. Función con las 3 peleas programadas.
2. Resultado tras cerrar la 1: "Siguiente pelea: Ana Torres vs Bea Salas".
3. Función con la 1 cerrada.
4. Jueces tras iniciar la 2: Juez 1 y 2 ya conectados y aprobados; solo el Juez 3 (lugar nuevo) muestra QR.
5. Función con la 2 en curso.
6. Función con la 3 saltada.

Esta rama solo guarda evidencia y no se integra a `main`.
