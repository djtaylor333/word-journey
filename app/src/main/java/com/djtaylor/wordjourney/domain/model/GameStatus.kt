package com.djtaylor.wordjourney.domain.model

enum class GameStatus {
    IN_PROGRESS,
    WON,
    LOST,              // daily challenge: no retries allowed
    WAITING_FOR_LIFE   // ran out of guesses, waiting on player to use a life
}
