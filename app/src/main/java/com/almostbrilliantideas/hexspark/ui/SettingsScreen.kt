package com.almostbrilliantideas.hexspark.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.almostbrilliantideas.hexspark.audio.SettingsManager

@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var soundEnabled by remember { mutableStateOf(settingsManager.soundEnabled) }
    var hapticsEnabled by remember { mutableStateOf(settingsManager.hapticsEnabled) }
    var showTutorialOnStartup by remember { mutableStateOf(settingsManager.showTutorialOnStartup) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            color = Color(0xFF252540),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Sound toggle
                SettingsToggleRow(
                    label = "Sound",
                    checked = soundEnabled,
                    onCheckedChange = { enabled ->
                        soundEnabled = enabled
                        settingsManager.soundEnabled = enabled
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Haptics toggle
                SettingsToggleRow(
                    label = "Haptics",
                    checked = hapticsEnabled,
                    onCheckedChange = { enabled ->
                        hapticsEnabled = enabled
                        settingsManager.hapticsEnabled = enabled
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tutorial on startup toggle
                SettingsToggleRow(
                    label = "Show tutorial",
                    checked = showTutorialOnStartup,
                    onCheckedChange = { enabled ->
                        showTutorialOnStartup = enabled
                        settingsManager.showTutorialOnStartup = enabled
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Privacy policy link
                Text(
                    text = "Privacy Policy",
                    fontSize = 13.sp,
                    color = Color(0xFF8B84D4),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://almostbrilliantideas.com/privacy_policies/hexspark-privacy-policy.html"))
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Close button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF8B84D4))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "\u2715", // X symbol
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1A1A2E),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B84D4),
                uncheckedThumbColor = Color(0xFFB8B4C4),
                uncheckedTrackColor = Color(0xFF3A3A50),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
