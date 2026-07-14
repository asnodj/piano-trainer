# piano-trainer

Native Android piano-learning app (Synthesia-style falling notes) for two users:
the developer (adult beginner) and his 7-year-old daughter. French-speaking users.

## Communication
- Responses to the user: **French**. Code, comments, docs, commit messages: **English**.
- Never add any AI attribution anywhere (commits, code, docs).

## Hardware / target
- Keyboard: **Yamaha PSR-E383** — USB TO HOST port (USB-B), class-compliant USB MIDI
  (also class-compliant USB audio). No Bluetooth MIDI on this model.
- Connection: USB-B → USB-C OTG cable into an Android tablet or phone.
- Min target: recent Android tablet + phone, adaptive Compose layouts for both.

## Stack (decided — do not relitigate)
- **Kotlin + Jetpack Compose**, native. Hybrid (Capacitor & co.) was considered and rejected.
- MIDI input: `android.media.midi` (`MidiManager`), USB device mode.
- MIDI file parsing: **ktmidi** (atsushieno/ktmidi, MIT, Kotlin).
- Sound v1: **billthefarmer/mididriver** (Apache-2.0, wraps Android's built-in Sonivox synth,
  tiny, on JitPack). Upgrade path: FluidSynth Android build (LGPL, Oboe driver) +
  GeneralUser GS or MuseScore General SF3 soundfont if quality matters later.
- Persistence: Room or DataStore for per-profile progression.
- Piano roll: Compose Canvas. Game engine (score state, timing windows, scoring)
  decoupled from UI and unit-testable.

## Product design (see docs/market-ux.md for the full survey)
Core system distilled from Simply Piano / Flowkey / Synthesia / Skoove:
1. **Wait mode is the DEFAULT learning state**: song frozen, next note(s) glow, nothing
   advances until the correct key is pressed. Wrong key = feedback, never a restart.
   Tempo mode is the graduation, not the start.
2. Practice ladder per song: listen → right hand → left hand → both hands (wait mode)
   → slow 50/75% → full speed.
3. Chunk songs into 4–8 bar sections with a selectable loop; failed section auto-repeats
   2–3× at lower demand.
4. Dual color feedback: falling note AND virtual key light together
   (green correct / red wrong / highlight upcoming).
5. Score % + 1–3 stars per section; stars soft-gate progression (never hard-block).
6. Kid mode (7yo profile): 5–10 min sessions, one big "Continue" button, celebration
   animation per section, journey-map progression view (not a list), colored notes.
7. Notation bridge (gap in every falling-notes app): mini staff fading in above the
   note highway, note-name labels that fade out as levels rise.
8. MIDI-only input, no microphone detection — sidesteps the #1 complaint of the market.

## v1 scope
1. Discovery mode: on-screen keyboard lights up when real keys are played (validates
   the MIDI pipeline; fun for the child).
2. Lesson mode: falling notes + wait mode.
3. Tempo mode with scoring (accuracy + timing windows).
4. Bundled content: self-encoded public-domain French comptines as MIDI
   (Au clair de la lune, Frère Jacques, Une souris verte, Vive le vent…) —
   encode ourselves, zero license risk, control of left-hand simplification per level.
5. Two local profiles (dad / daughter) with separate progression.

## Content & licensing (see docs/resources.md — licenses were verified on-site)
- SAFE: Mutopia Project (PD/CC-BY/CC-BY-SA per piece), OpenScore corpora (CC0),
  IMSLP PD method books (Beyer op.101, Czerny op.599, Burgmüller op.100 — the adult
  difficulty ladder), PianoBooster's beginner course (course content CC-BY; its CODE
  is GPL-3 — never copy code from it), FluidR3/MuseScore General soundfonts (MIT),
  GeneralUser GS (own permissive license), Salamander Grand (CC-BY, mind NC variants).
- NOT SAFE to embed: BitMidi/freemidi (no license), kunstderfuge (non-commercial),
  mfiles.co.uk (personal use only), MuseScore.com scores unless individually tagged
  PD/CC0/CC-BY, LeffelMania/android-midi-lib (no license file).

## Docs
- docs/resources.md — full licensed-resources research (MIDI sources, soundfonts, synths, curricula).
- docs/market-ux.md — full market survey (Simply Piano, Flowkey, Synthesia, La Touche Musicale…) and UX patterns.
- docs/roadmap.md — v1 plan and open questions.
