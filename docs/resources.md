# Licensed resources research (2026-07-14)

Research on downloadable/public resources usable in the app. Licenses were verified
on the actual sites at research time — re-check before any public/paid release.

## 1. MIDI files of beginner songs

| Source | What you get | License / reuse in an app |
|---|---|---|
| **Mutopia Project** — https://www.mutopiaproject.org/ | ~2,100 classical/traditional pieces, each as PDF + MIDI + LilyPond source. Searchable by instrument and style. | **SAFE.** Every piece tagged Public Domain, CC-BY, or CC-BY-SA (https://www.mutopiaproject.org/legal.html). All allow redistribution and commercial use; CC-BY/BY-SA require crediting the contributor, BY-SA is viral on derivatives. Best all-round legal source. |
| **OpenScore corpora** — https://github.com/OpenScore/Lieder | 1,200+ professionally-encoded scores; MSCZ/MusicXML on GitHub, MIDI/MusicXML/PDF per score on musescore.com. | **SAFE.** **CC0** — embed freely, attribution courtesy only. Art-song oriented, melodies extractable. |
| **kunstderfuge.com** | Huge classical MIDI archive. | **NOT SAFE.** Non-commercial only, limited republication (https://www.kunstderfuge.com/notes.htm). Personal reference only. |
| **BitMidi / freemidi.org** | 100k+ / 26k+ crowd-uploaded MIDIs. | **NOT SAFE.** No license at all (see unanswered feross/bitmidi.com#141). User uploads of unknown provenance; arrangements themselves copyrighted even when the song is PD. |
| **mfiles.co.uk** | ~50 nursery rhymes + traditional songs (sheet + MIDI + MP3), musically good. | **NOT SAFE.** "Personal use only", app embedding requires a paid license (https://www.mfiles.co.uk/copyright.htm). |
| French comptine hobby sites | MIDIs of Au clair de la lune, Frère Jacques, Une souris verte… | **Unclear.** "Free download" ≠ redistribution right; the arrangement in a MIDI carries its own copyright even for a PD melody. |

**Decision:** comptine melodies are PD and 10–30 notes long → **encode them ourselves**
(MuseScore Studio or programmatically) and ship our own MIDI/MusicXML. Zero license
risk, full control of fingering/tempo/left-hand simplification per level.
Use Mutopia + PD method books for the classical/adult tier.

## 2. MusicXML / sheet sources

- **Mutopia** — PDF/MIDI/LilyPond (LilyPond convertible to MusicXML). PD / CC-BY / CC-BY-SA per piece.
- **OpenScore** — native MSCZ + MusicXML (.mxl) bulk downloads (also https://fourscoreandmore.org/openscore/). **CC0** — safest large corpus.
- **MuseScore.com community scores** — mixed, mostly **NOT safe**: most uploads CC-BY-NC or all-rights-reserved; Pro-licensed arrangements are personal-use only. Only reuse scores explicitly tagged PD/CC0/CC-BY.
- **IMSLP** — reference library for PD scans (Beyer, Czerny, Burgmüller…), mostly PDF. PD works safe; check each file's tag.

## 3. Piano sounds

### SoundFonts / sample sets
- **FluidR3 GM** (Frank Wen) — 148 MB GM bank, good piano. **MIT** (https://member.keymusician.com/Member/FluidR3_GM/README.html). Extract the piano preset with Polyphone to shrink.
- **MuseScore General / FluidR3Mono** — SF3 ogg-compressed ~35–70 MB, **MIT** (https://github.com/musescore/MuseScore/blob/master/share/sound/FluidR3Mono_License.md). SF3 supported by FluidSynth — good size/quality for mobile.
- **Salamander Grand Piano** (Yamaha C5, 16 velocity layers) — **CC-BY 3.0** (https://github.com/sfzinstruments/SalamanderGrandPiano). Native SFZ ~1 GB; SF2 conversions vary: "Salamander C5 Light" is **CC-BY-NC** (blocks paid release), "Accurate-Salamander" packs on musical-artifacts.com are CC-BY.
- **GeneralUser GS** (S. Christian Collins) — 30 MB GM bank, excellent quality/MB. Own license v2.0 explicitly permits embedding, commercial or not (https://www.schristiancollins.com/generaluser.php).

### Android synth options
- **FluidSynth** — LGPL-2.1, first-party Android support, **Oboe default audio driver** (https://github.com/FluidSynth/fluidsynth/tree/master/doc/android). Kotlin+JNI example: https://github.com/robsonsmartins/android-midi-synth. LGPL OK as separate .so.
- **billthefarmer/mididriver** — https://github.com/billthefarmer/mididriver — **Apache-2.0**, wraps Android's built-in Sonivox EAS synth, on JitPack. Zero soundfont, tiny. Basic GM sound but fine for v1. ← **v1 choice**
- **mikrosoundfont / midi-android** (io.github.lemcoder, Apache-2.0) — Kotlin Multiplatform SF2 synth, lighter alternative to FluidSynth.
- **Oboe** (Apache-2.0) + own decoded samples — most work, lowest latency control.

## 4. Curricula & reusable open-source projects

- **PianoBooster** — https://github.com/pianobooster/PianoBooster — desktop MIDI play-along game with a bundled beginner course (https://www.pianobooster.org/Beginner%20Course.html). Code **GPL-3.0** (never copy code); **course/music/docs are CC-BY** → the lesson progression and course MIDIs are reusable with credit. Best curriculum find.
- **Mayron Cole Piano Method** — https://www.freepianomethod.com/ — full method free for non-profit use, translation permitted. Mirror the pedagogy/progression, do not ship her PDFs without asking.
- **PD method books on IMSLP** — Beyer Op. 101 (106 graded exercises), Czerny Op. 599, Burgmüller Op. 100. Fully PD: re-encode any exercise as MIDI and ship it. Proven difficulty ladder for the adult.
- **Neothesia** — https://github.com/PolyMeilex/Neothesia — polished falling-notes visualizer (Rust, GPL-3.0). UX reference only.
- **PianoSync** — https://github.com/clquwu/PianoSync — Android falling-notes app; no confirmed license, treat as reference only.
- **ktmidi** — https://github.com/atsushieno/ktmidi — Kotlin Multiplatform, **MIT**: SMF parsing + MIDI 1.0/2.0. ← **parser choice**
- **LeffelMania/android-midi-lib** — Java, no license file → **do not use**.

## NOT-safe recap
1. BitMidi / freemidi.org — no license, unknown provenance.
2. kunstderfuge — non-commercial only.
3. mfiles.co.uk — personal use only, paid license for apps.
4. MuseScore.com scores unless individually tagged PD/CC0/CC-BY.
5. Salamander "C5 Light" SF2 — NC variant (OK for hobby, blocks paid release).
6. android-midi-lib — no license file.

## Chosen stack (content side)
ktmidi (MIT) parsing + mididriver (Apache) sound v1 (FluidSynth + GeneralUser GS or
MuseScore General SF3 as upgrade path) + self-encoded comptine MIDIs + Mutopia/Beyer
pieces + PianoBooster CC-BY course as progression skeleton.
