# Roadmap

## v1 — playable core (target: 2–3 evenings)
1. **Project scaffold**: Kotlin + Compose, single module to start; adaptive layouts
   phone/tablet; two local profiles (dad / daughter, DataStore or Room).
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
- Key-press animations (particles/glow rising from keys, Rousseau-video style) —
  user request 2026-07-14, low priority.
- Microphone note detection as an optional wireless mode — user request 2026-07-14,
  second phase. Reservation on record: mic detection is the #1 complaint across the
  whole market (chords/octaves/latency); preferred wireless answer is a BLE MIDI
  adapter on the USB TO HOST port (Yamaha UD-BT01 / CME WIDI Uhost), which Android
  MIDI supports natively.
- FluidSynth + GeneralUser GS / MuseScore General SF3 for better sound.
- Notation bridge: mini-staff above the highway, fading note-name labels.
- Semi-auto assist (app plays LH, player plays RH).
- **Self-filmed hand videos** for mastered comptines (phone camera above the keyboard,
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
