#!/usr/bin/env python3
"""Mide los tokens de color de DESIGN.md v5 (con los de la v4) y escribe docs/design/contraste.md.

Métodos:
- Contraste: luminancia relativa de WCAG 2.x, (L1 + 0.05) / (L2 + 0.05).
- Daltonismo: matrices de Machado, Oliveira y Fernandes (2009), severidad 1.0, aplicadas en RGB lineal.
- Diferencia de color: ΔE76 en CIELAB (D65). ≥ 20 se distingue a simple vista; < 10 se confunde.

Uso: python3 docs/design/contraste.py   (solo biblioteca estándar)
"""
import math
from pathlib import Path

TOKENS = {
    "claro": {
        "bg": "#FAF9F6", "surface": "#FFFFFF", "surface_2": "#F2F0EB", "line": "#E3DFD7",
        "ink": "#111110", "ink_2": "#57534C", "field_border": "#8C877D",
        "primary": "#111110", "on_primary": "#FFFFFF",
        "accent": "#7A5A2A", "accent_soft": "#F3E7CF",
        "bar": "#111110", "on_bar": "#F7F5F0", "on_bar_2": "#B9B3A8", "bar_accent": "#C9A35E",
        "red": "#9F2241", "on_red": "#FFFFFF", "red_numeral": "#9F2241",
        "blue": "#266CB4", "on_blue": "#FFFFFF", "blue_numeral": "#266CB4",
        "connected": "#027A35", "disconnected": "#57534C",
        # v5 (DSN-005)
        "nav": "#111110", "nav_ink": "#B9B3A8", "nav_ink_dim": "#8F897E", "nav_active": "#2A2418",
        "success_soft": "#E3F3E8", "success_ink": "#027A35", "pending_ink": "#5C431E", "ink_3": "#8F897E",
        "ring_track": "#E9E6DF",
    },
    "oscuro": {
        "bg": "#0E0E0C", "surface": "#1A1917", "surface_2": "#24221F", "line": "#34312C",
        "ink": "#F5F3EE", "ink_2": "#B9B3A8", "field_border": "#7D776D",
        "primary": "#D4AF5A", "on_primary": "#111110",
        "accent": "#D4AF5A", "accent_soft": "#3A3020",
        "bar": "#0E0E0C", "on_bar": "#F5F3EE", "on_bar_2": "#B9B3A8", "bar_accent": "#D4AF5A",
        "red": "#E58A9D", "on_red": "#111110", "red_numeral": "#E58A9D",
        "blue": "#8CB8EE", "on_blue": "#111110", "blue_numeral": "#8CB8EE",
        "connected": "#4CC97A", "disconnected": "#B9B3A8",
        # v5 (DSN-005)
        "nav": "#0A0A09", "nav_ink": "#B9B3A8", "nav_ink_dim": "#8F897E", "nav_active": "#2A2418",
        "success_soft": "#16301F", "success_ink": "#7FE0A2", "pending_ink": "#E9C978", "ink_3": "#8F897E",
        "ring_track": "#24221F",
    },
}

# (primer plano, fondo, tipo): "texto" exige 4.5; "grande" (≥ 24 px o 19 px en negrita) y "ui" exigen 3.0.
PARES = [
    ("ink", "bg", "texto"), ("ink", "surface", "texto"), ("ink", "surface_2", "texto"),
    ("ink_2", "bg", "texto"), ("ink_2", "surface", "texto"), ("ink_2", "surface_2", "texto"),
    ("on_primary", "primary", "texto"), ("ink", "accent_soft", "texto"),
    ("on_bar", "bar", "texto"), ("on_bar_2", "bar", "texto"), ("bar_accent", "bar", "texto"),
    ("on_red", "red", "texto"), ("on_blue", "blue", "texto"),
    ("red_numeral", "surface", "texto"), ("blue_numeral", "surface", "texto"),
    ("red_numeral", "bg", "texto"), ("blue_numeral", "bg", "texto"),
    ("accent", "bg", "texto"), ("accent", "surface", "texto"), ("accent", "surface_2", "texto"),
    ("primary", "bg", "ui"), ("primary", "surface", "ui"), ("field_border", "surface", "ui"),
    ("red", "surface", "ui"), ("blue", "surface", "ui"),
    ("accent", "surface_2", "ui"), ("connected", "surface", "ui"), ("disconnected", "surface", "ui"),
    # v5 (DSN-005)
    ("nav_ink", "nav", "texto"), ("nav_ink_dim", "nav", "texto"), ("on_bar", "nav_active", "texto"),
    ("bar_accent", "nav_active", "texto"), ("success_ink", "success_soft", "texto"),
    ("pending_ink", "accent_soft", "texto"), ("ink_3", "surface", "grande"), ("bar_accent", "nav_active", "ui"),
]

# Colores que no deben confundirse entre sí.
DISTINGUIR = [("red_numeral", "blue_numeral"), ("red", "blue"), ("red", "accent"), ("blue", "accent"),
              ("red", "connected"), ("blue", "connected")]

MACHADO = {
    "deuteranopía": [[0.367322, 0.860646, -0.227968], [0.280085, 0.672501, 0.047413], [-0.011820, 0.042940, 0.968881]],
    "protanopía": [[0.152286, 1.052583, -0.204868], [0.114503, 0.786281, 0.099216], [-0.003882, -0.048116, 1.051998]],
    "tritanopía": [[1.255528, -0.076749, -0.178779], [-0.078411, 0.930809, 0.147602], [0.004733, 0.691367, 0.303900]],
}


def _lin(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _srgb(c):
    c = max(0.0, min(1.0, c))
    return 255 * (12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055)


def _rgb(h):
    h = h.lstrip("#")
    return [int(h[i:i + 2], 16) for i in (0, 2, 4)]


def luminancia(h):
    r, g, b = (_lin(x) for x in _rgb(h))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contraste(a, b):
    hi, lo = sorted((luminancia(a), luminancia(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def simular(h, tipo):
    lin = [_lin(x) for x in _rgb(h)]
    m = MACHADO[tipo]
    return "#" + "".join("%02X" % round(_srgb(sum(m[i][j] * lin[j] for j in range(3)))) for i in range(3))


def lab(h):
    r, g, b = (_lin(x) for x in _rgb(h))
    x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
    y = 0.2126 * r + 0.7152 * g + 0.0722 * b
    z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
    f = lambda t: t ** (1 / 3) if t > 0.008856 else 7.787 * t + 16 / 116
    return 116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z))


def delta_e(a, b):
    return math.dist(lab(a), lab(b))


def informe():
    out = ["# Contraste y daltonismo de los tokens v4", "",
           "Generado por `docs/design/contraste.py`. No editar a mano: cambia un token y vuelve a correr el script.", ""]
    fallas = 0
    for tema, t in TOKENS.items():
        out += [f"## Tema {tema}", "", "| Primer plano | Fondo | Tipo | Contraste | Mínimo | ¿Pasa? |", "|---|---|---|---|---|---|"]
        for fg, bg, tipo in PARES:
            c = contraste(t[fg], t[bg])
            minimo = 4.5 if tipo == "texto" else 3.0
            ok = c >= minimo
            fallas += not ok
            out.append(f"| `{fg}` {t[fg]} | `{bg}` {t[bg]} | {tipo} | {c:.2f} | {minimo} | {'sí' if ok else '**NO**'} |")
        out += ["", "| Par | ΔE normal | " + " | ".join(f"ΔE {k}" for k in MACHADO) + " |", "|---|---|---|---|---|"]
        for a, b in DISTINGUIR:
            fila = [f"{delta_e(t[a], t[b]):.1f}"] + [f"{delta_e(simular(t[a], k), simular(t[b], k)):.1f}" for k in MACHADO]
            out.append(f"| `{a}` / `{b}` | " + " | ".join(fila) + " |")
        out.append("")
    out.append(f"Pares de contraste que no pasan: {fallas}.")
    return "\n".join(out) + "\n", fallas


if __name__ == "__main__":
    texto, fallas = informe()
    destino = Path(__file__).with_name("contraste.md")
    destino.write_text(texto, encoding="utf-8")
    print(f"{destino}: {fallas} pares sin AA")
    raise SystemExit(1 if fallas else 0)
