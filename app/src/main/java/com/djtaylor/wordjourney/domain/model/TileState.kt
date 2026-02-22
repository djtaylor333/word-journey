package com.djtaylor.wordjourney.domain.model

enum class TileState {
    EMPTY,       // no letter entered
    FILLED,      // letter entered, not yet submitted
    HINT,        // letter revealed by "Show Letter" item (pre-filled, highlighted)
    CORRECT,     // correct letter, correct position (green)
    PRESENT,     // correct letter, wrong position (yellow)
    ABSENT       // letter not in word (grey)
}
