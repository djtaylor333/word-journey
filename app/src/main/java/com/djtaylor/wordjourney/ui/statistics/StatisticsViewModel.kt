package com.djtaylor.wordjourney.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djtaylor.wordjourney.data.db.StarRatingDao
import com.djtaylor.wordjourney.data.repository.DailyChallengeRepository
import com.djtaylor.wordjourney.data.repository.PlayerRepository
import com.djtaylor.wordjourney.domain.model.PlayerProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsUiState(
    val progress: PlayerProgress = PlayerProgress(),
    val totalStars: Int = 0,
    val perfectLevels: Int = 0,
    val easyStars: Int = 0,
    val regularStars: Int = 0,
    val hardStars: Int = 0,
    val dailyWins: Int = 0,
    val dailyPlayed: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val starRatingDao: StarRatingDao,
    private val dailyChallengeRepository: DailyChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playerRepository.playerProgressFlow.collectLatest { progress ->
                val totalStars = starRatingDao.totalStars()
                val perfectLevels = starRatingDao.countPerfectLevels()
                val easyStars = starRatingDao.totalStarsForDifficulty("easy")
                val regularStars = starRatingDao.totalStarsForDifficulty("regular")
                val hardStars = starRatingDao.totalStarsForDifficulty("hard")
                val dailyWins = dailyChallengeRepository.totalWins()
                val dailyPlayed = dailyChallengeRepository.totalPlayed()

                _uiState.update {
                    it.copy(
                        progress = progress,
                        totalStars = totalStars,
                        perfectLevels = perfectLevels,
                        easyStars = easyStars,
                        regularStars = regularStars,
                        hardStars = hardStars,
                        dailyWins = dailyWins,
                        dailyPlayed = dailyPlayed,
                        isLoading = false
                    )
                }
            }
        }
    }
}
