package com.aistudio.ecotobacco.kfzqw.ui.screens.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.ecotobacco.kfzqw.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    progress: Float,
    statusText: String,
    onFinished: () -> Unit
) {
    // Animasi progress bar halus mengikuti progres inisialisasi nyata
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "appInitProgress"
    )

    LaunchedEffect(progress) {
        if (progress >= 1f) {
            delay(250) // Jeda halus sebelum transisi ke dashboard
            onFinished()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121815)), // Menyesuaikan warna window background splash
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.kulakan_eco_tobacco),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // App Name / Version
            Text(
                text = "PENCATAT KULAKAN",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF4CAF50),
                letterSpacing = 2.sp
            )
            Text(
                text = "Eco Tobacco",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Real Loading Bar
            Column(
                modifier = Modifier.width(220.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = Color(0xFF2E7D32),
                    trackColor = Color(0xFF1E2E22),
                )
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE0E0E0),
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }
        }

        // Footer / Powered by
        Text(
            text = "Powered by ekofebriarianto",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF616161)
        )
    }
}
