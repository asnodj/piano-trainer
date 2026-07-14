#!/usr/bin/env python3
"""Encode the bundled public-domain songs as Standard MIDI Files.

Each song is authored here as plain note lists (zero license risk: we encode
traditional melodies ourselves) and written to app/src/main/assets/songs/
together with a .json sidecar carrying what MIDI cannot: hand assignment,
fingering (1=thumb..5=pinky) and section chunking.

Authoring format: per hand, a sequence of (pitch, quarters, finger) tuples;
pitch None = rest. Files are SMF format 1 (meta track + one track per hand);
the sidecar fingering array is generated in the loader's note order
(start time, then pitch), so hands can be authored independently.

Usage: python3 tools/encode_songs.py
"""

import json
import struct
from pathlib import Path

TICKS_PER_QUARTER = 480
NOTE_GAP_TICKS = 60  # small silence between notes so repeated notes re-trigger
VELOCITY = 90

ASSETS_DIR = Path(__file__).resolve().parent.parent / "app/src/main/assets/songs"

C2, G2, A2, B2 = 36, 43, 45, 47
C3, D3, E3, F3, G3, A3, B3 = 48, 50, 52, 53, 55, 57, 59
C4, D4, E4, F4, G4, A4, B4 = 60, 62, 64, 65, 67, 69, 71


def variable_length(value: int) -> bytes:
    """Encode an integer as a MIDI variable-length quantity."""
    encoded = [value & 0x7F]
    value >>= 7
    while value:
        encoded.append((value & 0x7F) | 0x80)
        value >>= 7
    return bytes(reversed(encoded))


def sequence_to_absolute(sequence):
    """Convert (pitch, quarters, finger) tuples into absolute-time notes.

    Returns a list of (start_ticks, duration_ticks, pitch, finger); rests
    (pitch None) only advance time.
    """
    absolute_notes = []
    cursor_ticks = 0
    for pitch, quarters, finger in sequence:
        duration_ticks = int(quarters * TICKS_PER_QUARTER)
        if pitch is not None:
            absolute_notes.append((cursor_ticks, duration_ticks, pitch, finger))
        cursor_ticks += duration_ticks
    return absolute_notes


def build_meta_track(tempo_microseconds: int) -> bytes:
    """Build the format-1 meta track: 4/4 time signature + tempo."""
    events = bytearray()
    events += variable_length(0) + bytes([0xFF, 0x58, 0x04, 0x04, 0x02, 0x18, 0x08])
    events += variable_length(0) + bytes([0xFF, 0x51, 0x03]) + struct.pack(">I", tempo_microseconds)[1:]
    events += variable_length(0) + bytes([0xFF, 0x2F, 0x00])
    return bytes(events)


def build_note_track(absolute_notes) -> bytes:
    """Build one note track from absolute-time notes (acoustic grand piano)."""
    timed_events = []  # (tick, order, status, pitch)
    for start_ticks, duration_ticks, pitch, _finger in absolute_notes:
        gate_ticks = max(duration_ticks - NOTE_GAP_TICKS, NOTE_GAP_TICKS)
        timed_events.append((start_ticks, 1, 0x90, pitch))
        timed_events.append((start_ticks + gate_ticks, 0, 0x80, pitch))
    timed_events.sort()

    events = bytearray()
    events += variable_length(0) + bytes([0xC0, 0x00])
    cursor_ticks = 0
    for tick, _order, status, pitch in timed_events:
        events += variable_length(tick - cursor_ticks)
        events += bytes([status, pitch, VELOCITY if status == 0x90 else 0x00])
        cursor_ticks = tick
    events += variable_length(0) + bytes([0xFF, 0x2F, 0x00])
    return bytes(events)


def write_song(slug: str, hands: dict, title: str, bpm: int, license_text: str,
               bars_per_section: int = 4) -> None:
    """Write <slug>.mid (format 1) and its generated .json sidecar.

    hands: {"right": sequence, "left": sequence} — omit absent hands.
    The sidecar fingering array follows the loader's order: all notes of all
    hands sorted by (start time, pitch); 0 = no fingering for that note.
    """
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    hand_order = [hand for hand in ("right", "left") if hand in hands]
    absolute_by_hand = {hand: sequence_to_absolute(hands[hand]) for hand in hand_order}

    tracks = [build_meta_track(60_000_000 // bpm)]
    tracks += [build_note_track(absolute_by_hand[hand]) for hand in hand_order]

    header = b"MThd" + struct.pack(">IHHH", 6, 1, len(tracks), TICKS_PER_QUARTER)
    body = b"".join(b"MTrk" + struct.pack(">I", len(track)) + track for track in tracks)
    (ASSETS_DIR / f"{slug}.mid").write_bytes(header + body)

    merged = sorted(
        (note for hand in hand_order for note in absolute_by_hand[hand]),
        key=lambda note: (note[0], note[2]),
    )
    fingering = [note[3] if note[3] else 0 for note in merged]

    metadata = {
        "title": title,
        "bpm": bpm,
        "barsPerSection": bars_per_section,
        "trackHands": hand_order,
        "fingering": fingering,
        "license": license_text,
    }
    (ASSETS_DIR / f"{slug}.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    note_count = sum(len(notes) for notes in absolute_by_hand.values())
    print(f"wrote {slug}.mid ({note_count} notes, hands: {', '.join(hand_order)})")


TRADITIONAL = "Traditional melody (public domain), encoded by the project"

# --- Au clair de la lune (C major, A A B A) ----------------------------------
# RH: C five-finger position for phrase A, thumb slides to G3 position for B.
# LH level 1: one bass note per bar, stationary G2..D3 position (G2=5, C3=2, D3=1).

AU_CLAIR_RH_A = [(C4, 1, 1), (C4, 1, 1), (C4, 1, 1), (D4, 1, 2), (E4, 2, 3), (D4, 2, 2),
                 (C4, 1, 1), (E4, 1, 3), (D4, 1, 2), (D4, 1, 2), (C4, 4, 1)]
AU_CLAIR_RH_B = [(D4, 1, 5), (D4, 1, 5), (D4, 1, 5), (D4, 1, 5), (A3, 2, 2), (A3, 2, 2),
                 (D4, 1, 5), (C4, 1, 4), (B3, 1, 3), (A3, 1, 2), (G3, 4, 1)]
AU_CLAIR_LH_A = [(C3, 4, 2), (G2, 4, 5), (G2, 4, 5), (C3, 4, 2)]
AU_CLAIR_LH_B = [(G2, 4, 5), (D3, 4, 1), (G2, 4, 5), (G2, 4, 5)]

# --- Estrellita / Twinkle Twinkle (C major, A A' B B A A') -------------------
# RH: C five-finger position, pinky stretches to A. LH: C3=5, F3=2, G2 jump=5.

ESTRELLITA_RH_A1 = [(C4, 1, 1), (C4, 1, 1), (G4, 1, 5), (G4, 1, 5), (A4, 1, 5), (A4, 1, 5), (G4, 2, 5)]
ESTRELLITA_RH_A2 = [(F4, 1, 4), (F4, 1, 4), (E4, 1, 3), (E4, 1, 3), (D4, 1, 2), (D4, 1, 2), (C4, 2, 1)]
ESTRELLITA_RH_B = [(G4, 1, 5), (G4, 1, 5), (F4, 1, 4), (F4, 1, 4), (E4, 1, 3), (E4, 1, 3), (D4, 2, 2)]
ESTRELLITA_LH_A1 = [(C3, 4, 5), (F3, 2, 2), (C3, 2, 5)]
ESTRELLITA_LH_A2 = [(F3, 2, 2), (C3, 2, 5), (G2, 2, 5), (C3, 2, 5)]
ESTRELLITA_LH_B = [(C3, 2, 5), (F3, 2, 2), (C3, 2, 5), (G2, 2, 5)]

# --- Vive le vent / Jingle Bells chorus (C major) ----------------------------
# RH: entirely inside the C five-finger position. LH: C3=2, F3=1, G2=5.

VIVE_LE_VENT_RH = [
    (E4, 1, 3), (E4, 1, 3), (E4, 2, 3),
    (E4, 1, 3), (E4, 1, 3), (E4, 2, 3),
    (E4, 1, 3), (G4, 1, 5), (C4, 1, 1), (D4, 1, 2),
    (E4, 4, 3),
    (F4, 1, 4), (F4, 1, 4), (F4, 1, 4), (F4, 1, 4),
    (F4, 1, 4), (E4, 1, 3), (E4, 1, 3), (E4, 1, 3),
    (E4, 1, 3), (D4, 1, 2), (D4, 1, 2), (E4, 1, 3),
    (D4, 2, 2), (G4, 2, 5),
]
VIVE_LE_VENT_LH = [
    (C3, 4, 2), (C3, 4, 2), (C3, 4, 2), (C3, 4, 2),
    (F3, 4, 1), (C3, 4, 2), (G2, 4, 5), (G2, 4, 5),
]

# --- Frère Jacques (C major, four repeated phrases) --------------------------
# RH: C five-finger position, pinky stretch to A. The low "ding" G3 belongs to
# the LEFT hand (thumb), the natural piano gesture.

FRERE_JACQUES_RH = (
    [(C4, 1, 1), (D4, 1, 2), (E4, 1, 3), (C4, 1, 1)] * 2
    + [(E4, 1, 3), (F4, 1, 4), (G4, 2, 5)] * 2
    + [(G4, 0.5, 5), (A4, 0.5, 5), (G4, 0.5, 5), (F4, 0.5, 4), (E4, 1, 3), (C4, 1, 1)] * 2
    + [(C4, 1, 1), (None, 1, None), (C4, 2, 1)] * 2
)
FRERE_JACQUES_LH = (
    [(None, 8, None)]  # phrase 1: melody alone
    + [(C3, 4, 3), (C3, 4, 3)]
    + [(C3, 2, 3), (G2, 2, 5), (C3, 2, 3), (G2, 2, 5)]
    + [(None, 1, None), (G3, 1, 1), (None, 2, None)] * 2
)

if __name__ == "__main__":
    write_song(
        "au_clair_de_la_lune",
        hands={
            "right": AU_CLAIR_RH_A + AU_CLAIR_RH_A + AU_CLAIR_RH_B + AU_CLAIR_RH_A,
            "left": AU_CLAIR_LH_A + AU_CLAIR_LH_A + AU_CLAIR_LH_B + AU_CLAIR_LH_A,
        },
        title="Au clair de la lune",
        bpm=100,
        license_text=TRADITIONAL,
    )
    write_song(
        "estrellita",
        hands={
            "right": (ESTRELLITA_RH_A1 + ESTRELLITA_RH_A2 + ESTRELLITA_RH_B
                      + ESTRELLITA_RH_B + ESTRELLITA_RH_A1 + ESTRELLITA_RH_A2),
            "left": (ESTRELLITA_LH_A1 + ESTRELLITA_LH_A2 + ESTRELLITA_LH_B
                     + ESTRELLITA_LH_B + ESTRELLITA_LH_A1 + ESTRELLITA_LH_A2),
        },
        title="Estrellita, ¿dónde estás?",
        bpm=90,
        license_text=TRADITIONAL,
    )
    write_song(
        "vive_le_vent",
        hands={"right": VIVE_LE_VENT_RH, "left": VIVE_LE_VENT_LH},
        title="Vive le vent",
        bpm=100,
        license_text=TRADITIONAL,
    )
    write_song(
        "frere_jacques",
        hands={"right": FRERE_JACQUES_RH, "left": FRERE_JACQUES_LH},
        title="Frère Jacques",
        bpm=100,
        license_text=TRADITIONAL,
    )
