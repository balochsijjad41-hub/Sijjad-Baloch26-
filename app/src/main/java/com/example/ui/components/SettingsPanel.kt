package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisTheme
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun SettingsPanel(
    currentTheme: JarvisTheme,
    wakeWordEnabled: Boolean,
    preferredLanguage: String,
    userName: String,
    speechRate: Float,
    speechPitch: Float,
    onThemeChange: (JarvisTheme) -> Unit,
    onWakeWordToggle: (Boolean) -> Unit,
    onLanguageChange: (String) -> Unit,
    onUserNameChange: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onClearData: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    var tempUserName by remember { mutableStateOf(userName) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SYSTEM CONFIGURATION // SETTINGS",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accent
                )
                Text(
                    text = "Personalize voice, language, theme and protocols",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("close_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Settings",
                    tint = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Arc Reactor Theme Color
        SettingsSectionHeader("1. HUD & ARC REACTOR THEME")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            JarvisTheme.entries.forEach { theme ->
                val isSelected = theme == currentTheme
                val color = when (theme) {
                    JarvisTheme.CYAN -> Color(0xFF00E5FF)
                    JarvisTheme.CRIMSON -> Color(0xFFFF1744)
                    JarvisTheme.EMERALD -> Color(0xFF00E676)
                    JarvisTheme.VIOLET -> Color(0xFFB388FF)
                    JarvisTheme.AMBER -> Color(0xFFFFAB00)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onThemeChange(theme) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .testTag("theme_${theme.name.lowercase()}")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = theme.name.take(4),
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Wake Word Trigger Switch
        SettingsSectionHeader("2. WAKE WORD DETECTION")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Listen for \"Hey Jarvis\"",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Continuous wake word trigger while app is active",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = wakeWordEnabled,
                onCheckedChange = onWakeWordToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent,
                    checkedTrackColor = accent.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("wake_word_switch")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Language & Dialect
        SettingsSectionHeader("3. VOICE RECOGNITION LANGUAGE")
        val languages = listOf(
            "ur-PK" to "Roman Urdu (اردو / Roman Urdu) ★ Primary",
            "auto" to "Auto Detect (Multilingual)",
            "hi-IN" to "Hindi / हिन्दी",
            "en-US" to "English (US / UK)"
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            languages.forEach { (code, label) ->
                val isSelected = preferredLanguage == code
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface)
                        .border(1.dp, if (isSelected) accent else Color.Transparent, RoundedCornerShape(10.dp))
                        .clickable { onLanguageChange(code) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("language_$code")
                ) {
                    Text(
                        text = label,
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. User Honorific / Name
        SettingsSectionHeader("4. USER DESIGNATION (HOW JARVIS CALLS YOU)")
        OutlinedTextField(
            value = tempUserName,
            onValueChange = {
                tempUserName = it
                onUserNameChange(it)
            },
            placeholder = { Text("e.g. Sir, Boss, Tony") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("username_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Speech Rate & Pitch
        SettingsSectionHeader("5. VOCAL CADENCE")
        Text(
            text = "Speech Rate: ${"%.2f".format(speechRate)}x",
            fontFamily = RajdhaniFontFamily,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = speechRate,
            onValueChange = onSpeechRateChange,
            valueRange = 0.8f..1.4f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
            modifier = Modifier.testTag("speech_rate_slider")
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Clear Data Button
        Button(
            onClick = onClearData,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("clear_data_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear data", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CLEAR ALL ARCHIVED DATA & MEMORY",
                fontFamily = OrbitronFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontFamily = OrbitronFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
