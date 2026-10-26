# Contraste y daltonismo de los tokens v3

Generado por `docs/design/contraste.py`. No editar a mano: cambia un token y vuelve a correr el script.

## Tema claro

| Primer plano | Fondo | Tipo | Contraste | Mínimo | ¿Pasa? |
|---|---|---|---|---|---|
| `ink` #1C1A17 | `bg` #F7F4EF | texto | 15.83 | 4.5 | sí |
| `ink` #1C1A17 | `surface` #FFFFFF | texto | 17.36 | 4.5 | sí |
| `ink` #1C1A17 | `surface_2` #EFEAE2 | texto | 14.50 | 4.5 | sí |
| `ink_2` #55585A | `bg` #F7F4EF | texto | 6.53 | 4.5 | sí |
| `ink_2` #55585A | `surface` #FFFFFF | texto | 7.17 | 4.5 | sí |
| `ink_2` #55585A | `surface_2` #EFEAE2 | texto | 5.99 | 4.5 | sí |
| `on_primary` #FFFFFF | `primary` #1C1A17 | texto | 17.36 | 4.5 | sí |
| `on_red` #FFFFFF | `red` #9F2241 | texto | 7.56 | 4.5 | sí |
| `on_blue` #FFFFFF | `blue` #266CB4 | texto | 5.40 | 4.5 | sí |
| `red_numeral` #9F2241 | `surface` #FFFFFF | texto | 7.56 | 4.5 | sí |
| `blue_numeral` #266CB4 | `surface` #FFFFFF | texto | 5.40 | 4.5 | sí |
| `red_numeral` #9F2241 | `bg` #F7F4EF | texto | 6.89 | 4.5 | sí |
| `blue_numeral` #266CB4 | `bg` #F7F4EF | texto | 4.93 | 4.5 | sí |
| `accent` #7A5A2A | `bg` #F7F4EF | texto | 5.76 | 4.5 | sí |
| `accent` #7A5A2A | `surface` #FFFFFF | texto | 6.32 | 4.5 | sí |
| `primary` #1C1A17 | `bg` #F7F4EF | ui | 15.83 | 3.0 | sí |
| `red` #9F2241 | `surface` #FFFFFF | ui | 7.56 | 3.0 | sí |
| `blue` #266CB4 | `surface` #FFFFFF | ui | 5.40 | 3.0 | sí |
| `accent` #7A5A2A | `surface_2` #EFEAE2 | ui | 5.28 | 3.0 | sí |
| `connected` #027A35 | `surface` #FFFFFF | ui | 5.47 | 3.0 | sí |
| `disconnected` #55585A | `surface` #FFFFFF | ui | 7.17 | 3.0 | sí |

| Par | ΔE normal | ΔE deuteranopía | ΔE protanopía | ΔE tritanopía |
|---|---|---|---|---|
| `red_numeral` / `blue_numeral` | 75.5 | 67.1 | 47.9 | 95.8 |
| `red` / `blue` | 75.5 | 67.1 | 47.9 | 95.8 |
| `red` / `accent` | 48.3 | 14.0 | 31.8 | 43.5 |
| `blue` / `accent` | 76.7 | 80.9 | 74.7 | 52.5 |
| `red` / `connected` | 98.6 | 8.0 | 38.6 | 97.4 |
| `blue` / `connected` | 88.8 | 74.2 | 78.3 | 15.3 |

## Tema oscuro

| Primer plano | Fondo | Tipo | Contraste | Mínimo | ¿Pasa? |
|---|---|---|---|---|---|
| `ink` #F3EFE8 | `bg` #14120F | texto | 16.31 | 4.5 | sí |
| `ink` #F3EFE8 | `surface` #1F1C18 | texto | 14.81 | 4.5 | sí |
| `ink` #F3EFE8 | `surface_2` #2A2621 | texto | 13.11 | 4.5 | sí |
| `ink_2` #B8B1A6 | `bg` #14120F | texto | 8.79 | 4.5 | sí |
| `ink_2` #B8B1A6 | `surface` #1F1C18 | texto | 7.98 | 4.5 | sí |
| `ink_2` #B8B1A6 | `surface_2` #2A2621 | texto | 7.07 | 4.5 | sí |
| `on_primary` #1C1A17 | `primary` #F3EFE8 | texto | 15.15 | 4.5 | sí |
| `on_red` #1C1A17 | `red` #E58A9D | texto | 6.99 | 4.5 | sí |
| `on_blue` #1C1A17 | `blue` #8CB8EE | texto | 8.44 | 4.5 | sí |
| `red_numeral` #E58A9D | `surface` #1F1C18 | texto | 6.83 | 4.5 | sí |
| `blue_numeral` #8CB8EE | `surface` #1F1C18 | texto | 8.25 | 4.5 | sí |
| `red_numeral` #E58A9D | `bg` #14120F | texto | 7.53 | 4.5 | sí |
| `blue_numeral` #8CB8EE | `bg` #14120F | texto | 9.09 | 4.5 | sí |
| `accent` #BC955C | `bg` #14120F | texto | 6.76 | 4.5 | sí |
| `accent` #BC955C | `surface` #1F1C18 | texto | 6.13 | 4.5 | sí |
| `primary` #F3EFE8 | `bg` #14120F | ui | 16.31 | 3.0 | sí |
| `red` #E58A9D | `surface` #1F1C18 | ui | 6.83 | 3.0 | sí |
| `blue` #8CB8EE | `surface` #1F1C18 | ui | 8.25 | 3.0 | sí |
| `accent` #BC955C | `surface_2` #2A2621 | ui | 5.43 | 3.0 | sí |
| `connected` #4CC97A | `surface` #1F1C18 | ui | 8.03 | 3.0 | sí |
| `disconnected` #B8B1A6 | `surface` #1F1C18 | ui | 7.98 | 3.0 | sí |

| Par | ΔE normal | ΔE deuteranopía | ΔE protanopía | ΔE tritanopía |
|---|---|---|---|---|
| `red_numeral` / `blue_numeral` | 53.0 | 42.6 | 30.4 | 74.8 |
| `red` / `blue` | 53.0 | 42.6 | 30.4 | 74.8 |
| `red` / `accent` | 42.6 | 27.9 | 36.9 | 21.3 |
| `blue` / `accent` | 68.2 | 70.4 | 66.2 | 55.8 |
| `red` / `connected` | 92.5 | 17.0 | 40.1 | 89.9 |
| `blue` / `connected` | 79.6 | 59.5 | 66.4 | 19.9 |

Pares de contraste que no pasan: 0.
