#!/usr/bin/env python3
"""Encode the bundled public-domain songs as Standard MIDI Files (format 0).

Each song is authored here as plain note lists (zero license risk: we encode
traditional melodies ourselves) and written to app/src/main/assets/songs/
together with a .json sidecar carrying what MIDI cannot: hand assignment,
fingering (1=thumb..5=pinky) and section chunking.

Usage: python3 tools/encode_songs.py
"""

import json
import struct
from pathlib import Path

TICKS_PER_QUARTER = 480
NOTE_GAP_TICKS = 60  # small silence between notes so repeated notes re-trigger
VELOCITY = 90

ASSETS_DIR = Path(__file__).resolve().parent.parent / "app/src/main/assets/songs"


def variable_length(value: int) -> bytes:
    """Encode an integer as a MIDI variable-length quantity."""
    encoded = [value & 0x7F]
    value >>= 7
    while value:
        encoded.append((value & 0x7F) | 0x80)
        value >>= 7
    return bytes(reversed(encoded))


def build_track(notes, tempo_microseconds: int) -> bytes:
    """Build a format-0 track: tempo + time signature + the melody notes.

    notes: list of (midi_note, duration_in_quarters) tuples.
    """
    events = bytearray()
    # 4/4 time signature.
    events += variable_length(0) + bytes([0xFF, 0x58, 0x04, 0x04, 0x02, 0x18, 0x08])
    events += variable_length(0) + bytes([0xFF, 0x51, 0x03]) + struct.pack(">I", tempo_microseconds)[1:]
    # Acoustic grand piano.
    events += variable_length(0) + bytes([0xC0, 0x00])

    pending_gap = 0
    for midi_note, quarters in notes:
        duration_ticks = int(quarters * TICKS_PER_QUARTER)
        gate_ticks = duration_ticks - NOTE_GAP_TICKS
        events += variable_length(pending_gap) + bytes([0x90, midi_note, VELOCITY])
        events += variable_length(gate_ticks) + bytes([0x80, midi_note, 0x00])
        pending_gap = NOTE_GAP_TICKS

    events += variable_length(0) + bytes([0xFF, 0x2F, 0x00])
    return bytes(events)


def write_song(slug: str, notes, metadata: dict, bpm: int) -> None:
    """Write <slug>.mid (format 0) and <slug>.json into the assets directory."""
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    track = build_track(notes, tempo_microseconds=60_000_000 // bpm)
    header = b"MThd" + struct.pack(">IHHH", 6, 0, 1, TICKS_PER_QUARTER)
    chunk = b"MTrk" + struct.pack(">I", len(track)) + track
    (ASSETS_DIR / f"{slug}.mid").write_bytes(header + chunk)
    (ASSETS_DIR / f"{slug}.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    print(f"wrote {slug}.mid ({len(notes)} notes) + {slug}.json")


# --- Au clair de la lune (traditional, C major, structure A A B A) -----------

C4, D4, E4, F4, G4, A4 = 60, 62, 64, 65, 67, 69
G3, A3, B3 = 55, 57, 59

PHRASE_A = [(C4, 1), (C4, 1), (C4, 1), (D4, 1), (E4, 2), (D4, 2),
            (C4, 1), (E4, 1), (D4, 1), (D4, 1), (C4, 4)]
PHRASE_B = [(D4, 1), (D4, 1), (D4, 1), (D4, 1), (A3, 2), (A3, 2),
            (D4, 1), (C4, 1), (B3, 1), (A3, 1), (G3, 4)]

# Right hand: phrase A in C five-finger position (thumb on C4),
# phrase B with the hand shifted down (thumb on G3).
FINGERING_A = [1, 1, 1, 2, 3, 2, 1, 3, 2, 2, 1]
FINGERING_B = [5, 5, 5, 5, 2, 2, 5, 4, 3, 2, 1]

AU_CLAIR_NOTES = PHRASE_A + PHRASE_A + PHRASE_B + PHRASE_A
AU_CLAIR_METADATA = {
    "title": "Au clair de la lune",
    "bpm": 100,
    "barsPerSection": 4,
    "trackHands": ["right"],
    "fingering": FINGERING_A + FINGERING_A + FINGERING_B + FINGERING_A,
    "license": "Traditional melody (public domain), encoded by the project",
}

# --- Estrellita / Twinkle Twinkle (traditional, C major, A A' B B A A') ------

ESTRELLITA_A1 = [(C4, 1), (C4, 1), (G4, 1), (G4, 1), (A4, 1), (A4, 1), (G4, 2)]
ESTRELLITA_A2 = [(F4, 1), (F4, 1), (E4, 1), (E4, 1), (D4, 1), (D4, 1), (C4, 2)]
ESTRELLITA_B = [(G4, 1), (G4, 1), (F4, 1), (F4, 1), (E4, 1), (E4, 1), (D4, 2)]

# C five-finger position, pinky stretches up to A.
ESTRELLITA_FINGERS_A1 = [1, 1, 5, 5, 5, 5, 5]
ESTRELLITA_FINGERS_A2 = [4, 4, 3, 3, 2, 2, 1]
ESTRELLITA_FINGERS_B = [5, 5, 4, 4, 3, 3, 2]

ESTRELLITA_NOTES = (ESTRELLITA_A1 + ESTRELLITA_A2 + ESTRELLITA_B + ESTRELLITA_B
                    + ESTRELLITA_A1 + ESTRELLITA_A2)
ESTRELLITA_METADATA = {
    "title": "Estrellita, ¿dónde estás?",
    "bpm": 90,
    "barsPerSection": 4,
    "trackHands": ["right"],
    "fingering": (ESTRELLITA_FINGERS_A1 + ESTRELLITA_FINGERS_A2 + ESTRELLITA_FINGERS_B
                  + ESTRELLITA_FINGERS_B + ESTRELLITA_FINGERS_A1 + ESTRELLITA_FINGERS_A2),
    "license": "Traditional melody (public domain), encoded by the project",
}

# --- Vive le vent / Jingle Bells chorus (traditional, C major) ---------------

VIVE_LE_VENT_NOTES = [
    (E4, 1), (E4, 1), (E4, 2),
    (E4, 1), (E4, 1), (E4, 2),
    (E4, 1), (G4, 1), (C4, 1), (D4, 1),
    (E4, 4),
    (F4, 1), (F4, 1), (F4, 1), (F4, 1),
    (F4, 1), (E4, 1), (E4, 1), (E4, 1),
    (E4, 1), (D4, 1), (D4, 1), (E4, 1),
    (D4, 2), (G4, 2),
]
# Entirely inside the C five-finger position.
VIVE_LE_VENT_FINGERS = [
    3, 3, 3,
    3, 3, 3,
    3, 5, 1, 2,
    3,
    4, 4, 4, 4,
    4, 3, 3, 3,
    3, 2, 2, 3,
    2, 5,
]
VIVE_LE_VENT_METADATA = {
    "title": "Vive le vent",
    "bpm": 100,
    "barsPerSection": 4,
    "trackHands": ["right"],
    "fingering": VIVE_LE_VENT_FINGERS,
    "license": "Traditional melody (public domain), encoded by the project",
}

if __name__ == "__main__":
    write_song("au_clair_de_la_lune", AU_CLAIR_NOTES, AU_CLAIR_METADATA, bpm=100)
    write_song("estrellita", ESTRELLITA_NOTES, ESTRELLITA_METADATA, bpm=90)
    write_song("vive_le_vent", VIVE_LE_VENT_NOTES, VIVE_LE_VENT_METADATA, bpm=100)
