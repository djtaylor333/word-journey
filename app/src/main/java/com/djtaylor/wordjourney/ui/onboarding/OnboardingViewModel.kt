package com.djtaylor.wordjourney.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tutorial step content for the onboarding experience.
 */
data class TutorialStep(
    val title: String,
    val dialogText: String,
    val emoji: String,
    val highlightArea: String? = null // Optional: "keyboard", "grid", "items", "lives"
)

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 0,
    val dialogText: String? = null,
    val dialogTitle: String = "",
    val dialogEmoji: String = "📖",
    val isCompleted: Boolean = false,
    val canGoBack: Boolean = false,
    val isLastStep: Boolean = false,
    val highlightArea: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val tutorialSteps = listOf(
        TutorialStep(
            title = "Welcome!",
            dialogText = "Welcome to Word Journeys! 🎉 Let me teach you how to play. Your goal is to guess the hidden word in 6 tries or fewer.",
            emoji = "🗺️",
            highlightArea = "grid"
        ),
        TutorialStep(
            title = "How to Guess",
            dialogText = "Type a word using the keyboard below and press ENTER to submit your guess. Each guess must be a valid English word.",
            emoji = "⌨️",
            highlightArea = "keyboard"
        ),
        TutorialStep(
            title = "Tile Colors",
            dialogText = "After each guess, tiles change color:\n🟩 Green = correct letter, correct position\n🟨 Yellow = correct letter, wrong position\n⬛ Gray = letter not in the word",
            emoji = "🎨",
            highlightArea = "grid"
        ),
        TutorialStep(
            title = "Use the Clues",
            dialogText = "Use the colored clues to narrow down the word. The keyboard also shows which letters you've tried — green, yellow, or gray.",
            emoji = "🔍",
            highlightArea = "keyboard"
        ),
        TutorialStep(
            title = "Power-Up Items",
            dialogText = "Stuck? Use items to help! ➕ adds an extra guess, 🚫 removes a wrong letter, 📖 shows the definition, and 💡 reveals a letter.",
            emoji = "⚡",
            highlightArea = "items"
        ),
        TutorialStep(
            title = "Lives & Hearts",
            dialogText = "Each new level costs one ❤️ life. Lives regenerate over time, and you earn bonus lives by completing levels! Keep your streak going for extra rewards.",
            emoji = "❤️",
            highlightArea = "lives"
        ),
        TutorialStep(
            title = "Difficulties",
            dialogText = "Choose your challenge:\n🌿 Easy = 4-letter words\n⚔️ Regular = 5-letter words\n🔥 Hard = 6-letter words\nEach has its own level progression!",
            emoji = "🏔️"
        ),
        TutorialStep(
            title = "Ready to Play!",
            dialogText = "You're ready to start your word adventure! Earn coins 🪙, collect diamonds 💎, and conquer the lexicon. Good luck! 🍀",
            emoji = "🚀"
        )
    )

    init {
        _uiState.update {
            it.copy(
                totalSteps = tutorialSteps.size,
                dialogText = tutorialSteps[0].dialogText,
                dialogTitle = tutorialSteps[0].title,
                dialogEmoji = tutorialSteps[0].emoji,
                highlightArea = tutorialSteps[0].highlightArea,
                canGoBack = false,
                isLastStep = tutorialSteps.size == 1
            )
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < tutorialSteps.size - 1) {
            val next = current + 1
            val step = tutorialSteps[next]
            _uiState.update {
                it.copy(
                    currentStep = next,
                    dialogText = step.dialogText,
                    dialogTitle = step.title,
                    dialogEmoji = step.emoji,
                    highlightArea = step.highlightArea,
                    canGoBack = true,
                    isLastStep = next == tutorialSteps.size - 1
                )
            }
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            val prev = current - 1
            val step = tutorialSteps[prev]
            _uiState.update {
                it.copy(
                    currentStep = prev,
                    dialogText = step.dialogText,
                    dialogTitle = step.title,
                    dialogEmoji = step.emoji,
                    highlightArea = step.highlightArea,
                    canGoBack = prev > 0,
                    isLastStep = false
                )
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.first().let { progress ->
                val updated = progress.copy(hasCompletedOnboarding = true)
                playerRepository.saveProgress(updated)
            }
            _uiState.update { it.copy(isCompleted = true) }
        }
    }

    fun skipOnboarding() {
        completeOnboarding()
    }
}
