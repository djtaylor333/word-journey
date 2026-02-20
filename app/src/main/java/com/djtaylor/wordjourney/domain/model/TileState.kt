package com.djtaylor.wordjourney.domain.model

enum class TileState {
    EMPTY,       // no letter entered
    FILLED,      // letter entered, not yet submitted
    CORRECT,     // correct letter, correct position (green)
    PRESENT,     // correct letter, wrong position (yellow)
    ABSENT       // letter not in word (grey)
}
