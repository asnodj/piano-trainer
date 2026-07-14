# Market survey — piano-learning apps on Android (2026-07-14)

Goal: identify the simplest systems for (a) a 7-year-old beginner, (b) an adult
beginner, and distill the UX patterns to copy in our falling-notes MIDI app.

## Per-app summary

### Simply Piano (Hello Simply, ex-JoyTunes)
- Sheet-music-first (scrolling staff, letter labels fading with progress). No falling notes.
- Mic by default (weak on chords/octaves), MIDI USB supported and recommended.
- Wait mode: yes (recent).
- 28 courses, two paths after common core: Soloist (classical) / Chords (pop). Songs
  chunked into short exercises; failed segments auto-repeat at reduced demand;
  personalized 5-min workouts. 5,000+ songs, each at multiple difficulty levels.
- Most game-like of the notation apps: green/red instant feedback, progress bars,
  celebrations, family accounts. Documented kids 6–9 practicing daily alone.
- ~$170/yr subscription. Complaints: "note-checker", no technique feedback, caps at
  early-intermediate, aggressive marketing, mic accuracy.

### Flowkey
- Hybrid split-screen: video of real hands + synced scrolling sheet music. Most
  polished interface on the market.
- MIDI (excellent) + mic. Wait mode: reference implementation (freezes until correct key).
- Song-first: per song, hands separate (L/R/both), slow 50/75/100%, draggable loop
  region over any bars. 1,500+ songs. Almost zero gamification (deliberately adult).
- ~$120/yr. Weak structured curriculum; thin free tier. Low kid-solo usability.

### Yousician (piano track)
- Falling-notes highway with optional color/letter labels (crutch criticized for
  delaying notation reading); notation display possible.
- Mic + MIDI. Strongest gamification: points, stars, streaks, weekly challenges,
  leaderboards. Kids like it but usually need an adult alongside.
- Levels 0–9, Guided Lessons / Missions / Workouts / Songs.
- Free tier w/ daily limits; ~$120–180/yr. Piano track shallower than dedicated apps.

### Synthesia
- Pure falling notes — the archetype. Optional sheet-music overlay toggle.
- Excellent MIDI USB/Bluetooth on Android; no mic detection. Touch possible but weak.
- Wait mode ("melody practice"): the original reference — accompaniment pauses until
  the correct key.
- No curriculum: practice tool. Hands separate, looping, any speed, **plays any MIDI
  file you load**. One-time ~$25 unlock (150 songs incl.), 20+ free.
- Complaints: teaches imitation not musicianship, no notation transfer, dated UI.

### Piano Academy (Yokee)
- Video lessons + highlighted keys → interactive sheet music. Built-in touch keyboard
  (zero-setup start). Touch + mic + MIDI. Skill mini-games (ear/rhythm/coordination).
- Friendly onboarding, kid-oriented. ~$150/yr. Mic detection flaky on real pianos.

### Piano Maestro (Simply/JoyTunes) — iPad only, NOT on Android
- Historic gold standard for kid gamification: chapter "Journey" map, stars per song,
  practice vs performance modes, scrolling notation over backing tracks. Pattern
  reference only.

### Skoove
- Sheet music from day one above an on-screen keyboard; played notes light up on both
  simultaneously (best staff↔key mapping device).
- Mic + MIDI (MIDI recommended). Wait mode: yes, standout.
- Fixed loop per lesson: **Listen → Learn → Play**. 500+ lessons. Minimal gamification.
- ~$120/yr. Android-specific bugs reported (freezes, mis-scored notes, MIDI octave-
  mapping bug). Adult-oriented, low kid-solo usability.

### La Touche Musicale (French)
- Pure falling notes onto a virtual piano — closest commercial analogue to our app.
- MIDI USB core mode + mic mode; 3,000+ songs. Wait mode is the default way it works.
- Per-song toolkit: hands separate, loop, speed, metronome, error stats. No curriculum,
  no notation path. Light gamification, very simple "video-game style" UI.
- Freemium; ~€48–90/yr. Occasional note-detection bugs; parent must curate songs.

### Perfect Piano (Revontulet) & keyboard-simulator apps
- 88-key touch simulator with falling notes/waterfall/sheet modes and **three assist
  levels: auto-play / semi-auto / note-pause (wait)** — a surprisingly good taxonomy.
- Toys, not learning systems. Free w/ ads.

### Others
- **Hoffman Academy**: top pedagogy for ages 5–10 (story-based 10–15-min video lessons,
  characters); video method, not interactive falling notes. Large free core.
- **Piano Marvel** (~$15/mo): MIDI-only, serious/teacher-adjacent, SASR sight-reading.
- **Playground Sessions**, **Pianote**, **OnlinePianist**: niche/adult.
- 2025–26 rankings still dominated by Simply Piano / Flowkey / Yousician.

## Conclusions

### Best for a 7-year-old
1. **Simply Piano** — chunked micro-exercises, instant feedback, family profiles.
2. **Yousician** — best game loop, but adult needed nearby.
3. **Piano Academy** — friendliest zero-setup onboarding.

### Best for an adult beginner
1. **Flowkey** — polished, wait mode + hands-separate + loop on any song.
2. **Skoove** — best structured curriculum (Listen→Learn→Play).
3. **Simply Piano** / **La Touche Musicale** (budget MIDI-first).

### UX patterns to copy (the distilled "system")
1. **Wait mode as DEFAULT learning state**: song frozen, next note(s) glow, nothing
   advances until the correct key; wrong keys give feedback but never restart.
   Tempo mode is the graduation.
2. **Canonical practice ladder**: listen → RH alone → LH alone → both (wait) →
   slow 50/75% → full speed with backing.
3. **Section chunking**: 4–8-bar segments, draggable/selectable loop region (Flowkey
   reference); failed segment auto-repeats 2–3× at lower demand (Simply Piano).
4. **Immediate binary color feedback per note**, on the falling note AND the key
   simultaneously (Skoove's dual lighting transposed to falling notes).
5. **Three assist levels** (Perfect Piano taxonomy): auto-play (watch), semi-auto
   (app plays one hand), note-pause (wait mode).
6. **Post-run % score + 1–3 stars per segment**; stars soft-gate (never hard-block).
7. **Kid specifics**: 5–10 min sessions, one big "Continue" button, celebration per
   segment, streak calendar, per-child profile, **journey-map progression view**
   (Piano Maestro) rather than a list.
8. **Notation bridge** (the gap all falling-notes apps fail): mini-staff fading in
   above the highway + note-name labels that disappear at higher levels (avoid
   Yousician's forever-labels mistake).
9. **MIDI-first, no mic**: mic misdetection is the #1 complaint cluster across the
   whole market; MIDI sidesteps it entirely.
10. **Multiple difficulty arrangements per song**: melody-only → melody + bass note →
    full arrangement (Simply Piano).
