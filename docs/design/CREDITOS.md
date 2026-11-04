# Créditos y licencias de recursos gráficos

Todo recurso de terceros que usen las apps queda aquí con su origen y su licencia, verificada en la página de origen. Nada se descarga en ejecución: los archivos van dentro de las apps.

## Fuentes

| Recurso | Licencia | Dónde está |
|---|---|---|
| Barlow Condensed | SIL Open Font License 1.1 | `android/app/src/main/res/font/`, `central/src/main/resources/com/sigebox/central/fonts/` |
| Inter | SIL Open Font License 1.1 | mismas carpetas |

## Logo: guantes de box rojos (DSN-004)

**Estado: elegida la opción B** (ver «Elegida»). Se preguntó con `uak msg human` (#189) el 2026-11-03 y no se descargó nada antes de la respuesta.

### Licencia de la fuente

Las tres opciones son de **Openclipart**. Su página de licencia, https://openclipart.org/share (consultada el 2026-11-03), dice que todo lo que se sube queda en dominio público con *Creative Commons Zero 1.0* (CC0), que se puede usar «even for unlimited commercial use» y que no hace falta citar a Openclipart. Cada página de detalle muestra además «Openclipart is 100% Public Domain».

- **CC0 1.0:** https://creativecommons.org/publicdomain/zero/1.0/ — uso comercial permitido, sin atribución obligatoria, se puede modificar.
- **Límite de la verificación:** CC0 lo declara quien sube el dibujo. No es posible comprobar desde aquí que esa persona sea su autora original. Ninguna de las tres reproduce una marca, un personaje o un logotipo conocido; la opción A trae la palabra «BOXE» en el puño, que se quita.

### Opciones

| | Título y autor (Openclipart) | URL de origen | Subido | Cómo se ve | Notas para usarlo como logo |
|---|---|---|---|---|---|
| **A** | «Boxing gloves» · spadassin | https://openclipart.org/detail/202038/boxing-gloves | 2014-09-15 | Par de guantes rojos brillantes, estilo caricatura con volumen y brillos, puño negro con la palabra «BOXE» | Es la más «medio animada» y la que mejor se lee a tamaño grande. Hay que borrar «BOXE» del SVG y simplificar brillos para el ícono de 48 dp |
| **B** | «Burning boxing gloves» · ksrujana96 | https://openclipart.org/detail/171575/burning-boxing-gloves | 2012-08-09 | Par de guantes rojos colgando de sus cordones, trazo negro, estilo dibujo plano | Silueta vertical: funciona bien en la pantalla de carga; en la barra superior queda más angosta. Sencilla de animar (balanceo) |
| **C** | «Boxing glove» · netalloy | https://openclipart.org/detail/171268/boxing-glove | 2012-07-21 | Un guante rojo con sombreado y puño gris oscuro, de perfil | Un solo guante: el ícono más limpio a 16–48 px; menos «caricatura» que A |

Formatos que ofrece cada página: SVG y PNG de 400, 800 y 2000 px. Se usará el **SVG** (o el PNG de 2000 px si el SVG da problemas).

### Elegida

**B · «Burning boxing gloves»**, de ksrujana96 en Openclipart. Elegida por cris el 2026-11-03 (respuesta textual: «B · Par colgando»).

| Campo | Valor |
|---|---|
| Archivo | `docs/design/logo/guantes-colgando.svg` (SVG original, sin cambios) |
| URL de origen | https://openclipart.org/detail/171575/burning-boxing-gloves |
| Descarga | https://openclipart.org/download/171575, el 2026-11-03 (30 859 bytes, `image/svg+xml`) |
| Autor | ksrujana96 (Openclipart), subido el 2012-08-09 |
| Licencia | CC0 1.0 / dominio público (https://openclipart.org/share). Los metadatos del SVG apuntan a http://creativecommons.org/licenses/publicdomain/ y permiten reproducción, distribución y obras derivadas |
| Atribución | No obligatoria; se deja aquí como registro |
| Cambios | Ninguno todavía. Las versiones para la barra (≤ 32 px) y el ícono (DESIGN.md §3.6) serán derivadas y se anotarán aquí |
