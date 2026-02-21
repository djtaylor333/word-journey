package com.djtaylor.wordjourney.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.djtaylor.wordjourney.ui.theme.*

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate away when completed
    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { viewModel.skipOnboarding() }) {
                    Text(
                        "Skip",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Center: Tutorial card
            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300))
                },
                label = "tutorialStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Emoji icon in circle
                    Surface(
                        shape = CircleShape,
                        color = Primary.copy(alpha = 0.15f),
                        shadowElevation = 8.dp,
                        modifier = Modifier.size(100.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                state.dialogEmoji,
                                fontSize = 48.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Title
                    Text(
                        text = state.dialogTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    // Dialog card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = state.dialogText ?: "",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Step indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                repeat(state.totalSteps) { index ->
                    Surface(
                        shape = CircleShape,
                        color = if (index == state.currentStep) Primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(if (index == state.currentStep) 12.dp else 8.dp)
                    ) {}
                }
            }

            // Bottom navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Back button
                if (state.canGoBack) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text("← Back", fontSize = 16.sp)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Next / Finish button
                Button(
                    onClick = {
                        if (state.isLastStep) {
                            viewModel.completeOnboarding()
                        } else {
                            viewModel.nextStep()
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text(
                        if (state.isLastStep) "Start Playing! 🚀" else "Next →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
