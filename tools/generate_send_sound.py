#!/usr/bin/env python3
"""Génère le son d'envoi signature de Tinda SMS (100 % original).

Motif « carillon Tinda » : deux notes ascendantes (B5 puis E6, quarte
juste) jouées comme des lames de marimba synthétisées (fondamentale +
harmoniques, décroissance exponentielle). Aucun échantillon externe :
tout est calculé ici.

Sortie : app/src/main/res/raw/tinda_send.wav (44,1 kHz, 16 bits, mono).
Usage : python3 tools/generate_send_sound.py
"""

import math
import struct
import wave
from pathlib import Path

SAMPLE_RATE = 44100

# Notes du motif (fréquences en Hz) : (début_en_s, fréquence).
NOTES = [
    (0.00, 987.77),   # B5
    (0.18, 1318.51),  # E6
]

NOTE_DECAY = 0.16      # Constante de décroissance (s) : son bref et net.
ATTACK = 0.005         # Attaque anti-clic (s).
DURATION = 0.70        # Durée totale (s).
PEAK = 0.75            # Niveau crête cible.


def note_samples(start: float, freq: float, total: int) -> list[float]:
    """Échantillons d'une note de marimba synthétisée."""
    out = [0.0] * total
    start_i = int(start * SAMPLE_RATE)
    for i in range(start_i, total):
        t = (i - start_i) / SAMPLE_RATE
        env = math.exp(-t / NOTE_DECAY)
        # Attaque douce sur 5 ms pour éviter le clic.
        if t < ATTACK:
            env *= 0.5 - 0.5 * math.cos(math.pi * t / ATTACK)
        # Fondamentale + harmoniques (timbre « lame »).
        s = (
            math.sin(2 * math.pi * freq * t)
            + 0.35 * math.sin(2 * math.pi * 2 * freq * t) * math.exp(-t / 0.08)
            + 0.12 * math.sin(2 * math.pi * 3 * freq * t) * math.exp(-t / 0.05)
        )
        out[i] = s * env
    return out


def main() -> None:
    total = int(DURATION * SAMPLE_RATE)
    mix = [0.0] * total
    for start, freq in NOTES:
        for i, s in enumerate(note_samples(start, freq, total)):
            mix[i] += s
    # Fondu de sortie sur les 50 dernières ms.
    fade = int(0.05 * SAMPLE_RATE)
    for i in range(fade):
        mix[total - fade + i] *= 1.0 - i / fade
    # Normalisation.
    peak = max(abs(s) for s in mix) or 1.0
    gain = PEAK / peak
    frames = struct.pack(
        "<" + "h" * total,
        *(max(-32768, min(32767, int(s * gain * 32767))) for s in mix),
    )
    out_path = (
        Path(__file__).resolve().parent.parent
        / "app" / "src" / "main" / "res" / "raw" / "tinda_send.wav"
    )
    out_path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(out_path), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SAMPLE_RATE)
        w.writeframes(frames)
    print(f"Écrit : {out_path} ({len(frames) // 2} échantillons)")


if __name__ == "__main__":
    main()
