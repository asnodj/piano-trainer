package dev.asnodj.pianotrainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.midi.MidiInputManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * App-level state holder: owns the MIDI input manager and folds its raw note
 * events into UI-ready state (currently-pressed keys).
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val midiInputManager = MidiInputManager(application)

    /** Keyboard connection status for the UI. */
    val connectionState: StateFlow<MidiConnectionState> = midiInputManager.connectionState

    /** MIDI note numbers currently held down on the physical keyboard. */
    val pressedNotes: StateFlow<Set<Int>> = midiInputManager.noteEvents
        .scan(emptySet<Int>()) { currentlyPressed, noteEvent ->
            if (noteEvent.isNoteOn) {
                currentlyPressed + noteEvent.note
            } else {
                currentlyPressed - noteEvent.note
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init {
        midiInputManager.start()
    }

    override fun onCleared() {
        midiInputManager.stop()
    }
}
