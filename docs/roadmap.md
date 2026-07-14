# Roadmap

## v1 — playable core (target: 2–3 evenings)
*2026-07-14 late evening: content pass done — encoder rewritten (SMF format 1, one
track per hand, rests, per-note fingering, sidecar generated in loader note order).
All 4 comptines now have a simplified left hand (level-1 whole/half-note bass in a
stationary position) with full fingering; Frère Jacques added (the low "ding" G3 is
a left-hand thumb note); Für Elise opening theme fingered (first 40 notes, classic
4-3 alternation + 5-2-1 arpeggios), rest of the piece pending.*
1. **Project scaffold**: Kotlin + Compose, single module to start; adaptive layouts
   phone/tablet; two local profiles (dad / daughter, DataStore or Room).
   *2026-07-14 late: profiles DONE (DataStore) — Papa/Lara badges on the home header,
   best scores saved per profile+song+mode on run completion, stars + tempo best shown
   on the song tiles. Home restyled: two-tone logo, USB status icon (green/grey),
   tile grid. App locked to landscape.*
   *2026-07-14: scaffold done, first `assembleDebug` green (placeholder screen only —
   profiles and adaptive layouts still to do). Gradle 8.14.3 + AGP 8.12 + Kotlin 2.2;
   builds with Temurin JDK 21 in `~/.jdks` (see `gradle.properties` — system JDK 25
   is too new for AGP).*
2. **MIDI pipeline**: `MidiManager` USB device detection (PSR-E383 over USB-B → USB-C
   OTG), note-on/note-off stream, connection status UI.
   *2026-07-14: implemented (`midi/` package: pure-Kotlin `MidiMessageParser` with 9 unit
   tests — running status, velocity-0 note-off, real-time bytes, split packets —,
   `MidiInputManager` hot-plug USB handling). Validated same day on real hardware
   (PSR-E383 → USB-B→USB-C → Galaxy S24 Ultra): keys light up green when played.*
3. **Discovery mode**: on-screen keyboard that lights up keys as they are played on
   the real keyboard. Validates the whole pipeline; immediately fun for the child.
   *2026-07-14: implemented (61-key Canvas keyboard, pressed keys in green, routed from
   connection state) and validated on the real PSR-E383.*
4. **MIDI file loading**: ktmidi, bundled assets; song model = sections of 4–8 bars,
   per-hand tracks.
   *2026-07-14: implemented. `song/` package: `Song`/`SongNote`/`Section` model,
   `SmfSongLoader` (ktmidi 0.11.0) with hands-per-track, fingering and sections from a
   .json sidecar next to each .mid (MIDI cannot carry fingering). `tools/encode_songs.py`
   authors the comptines. Bundled: Au clair de la lune (self-encoded, RH + fingering,
   16 bars = 4 sections) and Für Elise (Mutopia, Public Domain, 2 hands — the adult
   target piece and a robustness test for the loader). 7 loader unit tests.*
5. **Lesson mode (wait mode)**: falling-notes canvas, song frozen until correct key,
   dual green/red feedback on note + key, hands separate toggle.
6. **Content v1**: self-encoded comptines (Au clair de la lune, Frère Jacques,
   Une souris verte, Vive le vent…), melody-only + simplified-LH arrangements.
7. **Sound v1**: mididriver (feedback notes / demo playback). The keyboard itself
   produces the sound the child hears while playing.

## v2 — the learning system
User priority order (2026-07-14 evening): 1. mic mode, 2. semi-auto, 3. tempo+scoring.
1. **Microphone input mode (TOP PRIORITY — user decision 2026-07-14)**: play the real
   piano without the USB cable, notes detected through the phone mic. Scope honestly:
   MONOPHONIC pitch detection first (autocorrelation/YIN on AudioRecord) — reliable for
   the single-note comptines; chords/two-hands stay MIDI-only (documented market-wide
   weakness). Detection biased toward the expected wait-mode note (validation, not
   blind transcription). Feed detected notes into the same event stream as MIDI.
2. **Semi-auto assist (promoted from v3)**: the app plays the left hand in audio while
   the player plays the right hand — the piece sounds complete while learning one hand.
   *2026-07-14: DONE. Works in wait mode (accompaniment windows follow the player's
   advances at tempo-scaled offsets) and in tempo mode (follows the run clock).
   Toggle chip in the lesson header, disabled when the song has no other hand.*
3. **Tempo mode with scoring** (see below).
   *2026-07-14: DONE. TempoEngine (8 unit tests): ±120ms perfect / ±300ms good windows,
   missed notes turn grey and scroll past, wrong presses counted, live score in the
   header, final overlay with stars (3≥90%, 2≥70%) and perfect/good/missed detail.
   Not yet validated with the real keyboard.*
- Tempo mode with scoring (timing windows, % + 1–3 stars per section, soft-gating).
- Practice ladder per song: listen → RH → LH → both (wait) → 50/75% → full speed.
- **Fingering display**: finger numbers (1–5) inside falling notes + on the virtual
  keyboard; hands distinguished by color. MIDI has no fingering standard → we author
  it ourselves in our self-encoded files (sidecar metadata); comptines are mostly
  five-finger position, classical pieces copy PD-edition fingering (IMSLP).
- Section looping with selectable region; auto-repeat failed sections.
- Kid mode polish: celebrations, streak calendar, journey-map progression view.
- Adult track: import Beyer/Czerny/Burgmüller exercises (PD, re-encoded), Mutopia pieces.
- PianoBooster CC-BY beginner course as progression skeleton (credit required).

## v3+ — ideas
- Key-press animation polish (bloom at the key line, Rousseau-video style) — current
  light show shipped 2026-07-14 and approved; further polish stays low priority.
- Bluetooth MIDI support (BLE adapter on the keyboard's USB TO HOST port, e.g. Yamaha
  UD-BT01 ~€45-60 or CME WIDI Uhost ~€55-70): user will first evaluate how much the
  app gets used before buying; nearly free to support code-wise when the time comes.
- ~~Microphone note detection as an optional wireless mode~~ **PROMOTED to top
  priority by the user (2026-07-14 evening)** — see v2.
- Debug builds have a virtual-keyboard touch injector (dev tool only, never a
  learning feature): app fully testable without the piano.
- FluidSynth + GeneralUser GS / MuseScore General SF3 for better sound.
- Notation bridge: mini-staff above the highway, fading note-name labels.
- Notation bridge, Flowkey-style: a scrolling sheet-music banner above the highway.
  User note 2026-07-14: low priority; must be toggleable (phone screens too small),
  tablet-first feature.
- **Self-filmed hand videos** — LOWEST priority (user 2026-07-14) — for mastered
  comptines (phone camera above the keyboard,
  synced to the song for the "listen/watch" step — Flowkey-style, tiny catalog makes
  it feasible). Interim before that: official YouTube embed of hand videos in the
  "watch" step, adult profile only (ads/suggestions unsuitable for the kid profile).
  Never sync video with wait mode: player pause latency kills note-by-note freezing,
  and downloading/embedding third-party videos is not allowed.
- MusicXML import; user-loaded MIDI files (Synthesia's killer feature).
- Recording / replay of sessions.

## Open questions
- Dev workflow: adb over Wi-Fi while tablet's USB port is taken by the keyboard?
  (PSR-E383 occupies the only USB-C port on most tablets → wireless debugging likely
  required.)
- Project name shown to the child on screen (working title: piano-trainer).
- git init + GitHub remote? (not done yet)
