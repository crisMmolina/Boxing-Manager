# Contraste y daltonismo de los tokens v4

Generado por `docs/design/contraste.py`. No editar a mano: cambia un token y vuelve a correr el script.

## Tema claro

| Primer plano | Fondo | Tipo | Contraste | Mínimo | ¿Pasa? |
|---|---|---|---|---|---|
| `ink` #111110 | `bg` #FAF9F6 | texto | 17.95 | 4.5 | sí |
| `ink` #111110 | `surface` #FFFFFF | texto | 18.89 | 4.5 | sí |
| `ink` #111110 | `surface_2` #F2F0EB | texto | 16.59 | 4.5 | sí |
| `ink_2` #57534C | `bg` #FAF9F6 | texto | 7.26 | 4.5 | sí |
| `ink_2` #57534C | `surface` #FFFFFF | texto | 7.64 | 4.5 | sí |
| `ink_2` #57534C | `surface_2` #F2F0EB | texto | 6.71 | 4.5 | sí |
| `on_primary` #FFFFFF | `primary` #111110 | texto | 18.89 | 4.5 | sí |
| `ink` #111110 | `accent_soft` #F3E7CF | texto | 15.42 | 4.5 | sí |
| `on_bar` #F7F5F0 | `bar` #111110 | texto | 17.34 | 4.5 | sí |
| `on_bar_2` #B9B3A8 | `bar` #111110 | texto | 9.07 | 4.5 | sí |
| `bar_accent` #C9A35E | `bar` #111110 | texto | 7.99 | 4.5 | sí |
| `on_red` #FFFFFF | `red` #9F2241 | texto | 7.56 | 4.5 | sí |
| `on_blue` #FFFFFF | `blue` #266CB4 | texto | 5.40 | 4.5 | sí |
| `red_numeral` #9F2241 | `surface` #FFFFFF | texto | 7.56 | 4.5 | sí |
| `blue_numeral` #266CB4 | `surface` #FFFFFF | texto | 5.40 | 4.5 | sí |
| `red_numeral` #9F2241 | `bg` #FAF9F6 | texto | 7.18 | 4.5 | sí |
| `blue_numeral` #266CB4 | `bg` #FAF9F6 | texto | 5.13 | 4.5 | sí |
| `accent` #7A5A2A | `bg` #FAF9F6 | texto | 6.00 | 4.5 | sí |
| `accent` #7A5A2A | `surface` #FFFFFF | texto | 6.32 | 4.5 | sí |
| `accent` #7A5A2A | `surface_2` #F2F0EB | texto | 5.55 | 4.5 | sí |
| `primary` #111110 | `bg` #FAF9F6 | ui | 17.95 | 3.0 | sí |
| `primary` #111110 | `surface` #FFFFFF | ui | 18.89 | 3.0 | sí |
| `field_border` #8C877D | `surface` #FFFFFF | ui | 3.57 | 3.0 | sí |
| `red` #9F2241 | `surface` #FFFFFF | ui | 7.56 | 3.0 | sí |
| `blue` #266CB4 | `surface` #FFFFFF | ui | 5.40 | 3.0 | sí |
| `accent` #7A5A2A | `surface_2` #F2F0EB | ui | 5.55 | 3.0 | sí |
| `connected` #027A35 | `surface` #FFFFFF | ui | 5.47 | 3.0 | sí |
| `disconnected` #57534C | `surface` #FFFFFF | ui | 7.64 | 3.0 | sí |

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
| `ink` #F5F3EE | `bg` #0E0E0C | texto | 17.42 | 4.5 | sí |
| `ink` #F5F3EE | `surface` #1A1917 | texto | 15.84 | 4.5 | sí |
| `ink` #F5F3EE | `surface_2` #24221F | texto | 14.31 | 4.5 | sí |
| `ink_2` #B9B3A8 | `bg` #0E0E0C | texto | 9.27 | 4.5 | sí |
| `ink_2` #B9B3A8 | `surface` #1A1917 | texto | 8.43 | 4.5 | sí |
| `ink_2` #B9B3A8 | `surface_2` #24221F | texto | 7.61 | 4.5 | sí |
| `on_primary` #111110 | `primary` #D4AF5A | texto | 9.07 | 4.5 | sí |
| `ink` #F5F3EE | `accent_soft` #3A3020 | texto | 11.66 | 4.5 | sí |
| `on_bar` #F5F3EE | `bar` #0E0E0C | texto | 17.42 | 4.5 | sí |
| `on_bar_2` #B9B3A8 | `bar` #0E0E0C | texto | 9.27 | 4.5 | sí |
| `bar_accent` #D4AF5A | `bar` #0E0E0C | texto | 9.27 | 4.5 | sí |
| `on_red` #111110 | `red` #E58A9D | texto | 7.61 | 4.5 | sí |
| `on_blue` #111110 | `blue` #8CB8EE | texto | 9.18 | 4.5 | sí |
| `red_numeral` #E58A9D | `surface` #1A1917 | texto | 7.07 | 4.5 | sí |
| `blue_numeral` #8CB8EE | `surface` #1A1917 | texto | 8.54 | 4.5 | sí |
| `red_numeral` #E58A9D | `bg` #0E0E0C | texto | 7.78 | 4.5 | sí |
| `blue_numeral` #8CB8EE | `bg` #0E0E0C | texto | 9.39 | 4.5 | sí |
| `accent` #D4AF5A | `bg` #0E0E0C | texto | 9.27 | 4.5 | sí |
| `accent` #D4AF5A | `surface` #1A1917 | texto | 8.43 | 4.5 | sí |
| `accent` #D4AF5A | `surface_2` #24221F | texto | 7.61 | 4.5 | sí |
| `primary` #D4AF5A | `bg` #0E0E0C | ui | 9.27 | 3.0 | sí |
| `primary` #D4AF5A | `surface` #1A1917 | ui | 8.43 | 3.0 | sí |
| `field_border` #7D776D | `surface` #1A1917 | ui | 3.96 | 3.0 | sí |
| `red` #E58A9D | `surface` #1A1917 | ui | 7.07 | 3.0 | sí |
| `blue` #8CB8EE | `surface` #1A1917 | ui | 8.54 | 3.0 | sí |
| `accent` #D4AF5A | `surface_2` #24221F | ui | 7.61 | 3.0 | sí |
| `connected` #4CC97A | `surface` #1A1917 | ui | 8.32 | 3.0 | sí |
| `disconnected` #B9B3A8 | `surface` #1A1917 | ui | 8.43 | 3.0 | sí |

| Par | ΔE normal | ΔE deuteranopía | ΔE protanopía | ΔE tritanopía |
|---|---|---|---|---|
| `red_numeral` / `blue_numeral` | 53.0 | 42.6 | 30.4 | 74.8 |
| `red` / `blue` | 53.0 | 42.6 | 30.4 | 74.8 |
| `red` / `accent` | 54.8 | 39.7 | 51.3 | 22.0 |
| `blue` / `accent` | 79.6 | 81.9 | 78.8 | 55.1 |
| `red` / `connected` | 92.5 | 17.0 | 40.1 | 89.9 |
| `blue` / `connected` | 79.6 | 59.5 | 66.4 | 19.9 |

Pares de contraste que no pasan: 0.
