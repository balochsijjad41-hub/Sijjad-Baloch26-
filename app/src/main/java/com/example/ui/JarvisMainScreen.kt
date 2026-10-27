package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AssistantState
import com.example.data.model.JarvisMode
import com.example.ui.components.ArcReactorOrb
import com.example.ui.components.CameraScannerOverlay
import com.example.ui.components.CommandCheatSheet
import com.example.ui.components.ConfirmationCard
import com.example.ui.components.HudHeader
import com.example.ui.components.LiveTranscriptView
import com.example.ui.components.SettingsPanel
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.viewmodel.JarvisViewModel

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val accent = MaterialTheme.colorScheme.primary

    var typedInputText by remember { mutableStateOf("") }

    // Audio & Camera Permission Launchers
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.toggleVoiceListening()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleCamera(true)
        }
    }

    // Surface errors via snackbar
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Ripple pulse animation for the active mic button
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val micPulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Background subtle holographic grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )

            // Main Content Layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Top HUD Status Bar
                HudHeader(
                    currentMode = uiState.currentMode,
                    batteryPercent = uiState.batteryPercent,
                    isCharging = uiState.isCharging,
                    activeServer = uiState.activeServer,
                    onServerClick = { viewModel.toggleServerMatrix(true) },
                    onModeClick = {
                        val nextMode = when (uiState.currentMode) {
                            JarvisMode.NORMAL -> JarvisMode.STUDY
                            JarvisMode.STUDY -> JarvisMode.FOCUS
                            JarvisMode.FOCUS -> JarvisMode.CHILL
                            JarvisMode.CHILL -> JarvisMode.NORMAL
                        }
                        viewModel.setMode(nextMode)
                    },
                    onHelpClick = { viewModel.toggleCheatSheet(true) },
                    onSettingsClick = { viewModel.toggleSettings(true) }
                )

                // Active Timer HUD Indicator
                uiState.activeTimer?.let { timer ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(alpha = 0.2f))
                            .border(1.dp, accent, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Active Timer",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val mins = timer.remainingSeconds / 60
                        val secs = timer.remainingSeconds % 60
                        Text(
                            text = "${timer.label}: %02d:%02d".format(mins, secs),
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel timer",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.cancelActiveTimer() }
                        )
                    }
                }

                // 2. Center: Majestic Glowing Arc Reactor Orb
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ArcReactorOrb(
                        state = uiState.state,
                        audioLevel = uiState.audioLevel,
                        accentColor = accent,
                        glowColor = MaterialTheme.colorScheme.secondary
                    )
                }

                // 3. Safety Confirmation Dialog (for calls/messages)
                uiState.confirmationRequest?.let { req ->
                    ConfirmationCard(
                        confirmation = req,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // 4. Live Dialog Transcript
                LiveTranscriptView(
                    messages = uiState.messages,
                    livePartialText = uiState.livePartialText,
                    onSpeakMessage = { viewModel.speakOutLoud(it) },
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Optional Keyboard Input Box
                AnimatedVisibility(visible = uiState.isTextInputVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = typedInputText,
                            onValueChange = { typedInputText = it },
                            placeholder = { Text("Type query for JARVIS...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("text_query_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (typedInputText.isNotBlank()) {
                                    viewModel.processUserQuery(typedInputText)
                                    typedInputText = ""
                                    viewModel.toggleTextInput(false)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(accent)
                                .testTag("send_text_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.Black
                            )
                        }
                    }
                }

                // 5. Bottom HUD Controls Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Camera Vision Mode Button
                    IconButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                viewModel.toggleCamera(true)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
                            .testTag("camera_vision_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Visual Vision Scanner",
                            tint = accent
                        )
                    }

                    // Central Big Glowing Mic Trigger Button
                    val isListening = uiState.state == AssistantState.LISTENING
                    val isSpeaking = uiState.state == AssistantState.SPEAKING

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(86.dp)
                            .clickable {
                                if (isSpeaking) {
                                    viewModel.stopSpeaking()
                                } else if (hasMicPermission) {
                                    viewModel.toggleVoiceListening()
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                            .testTag("big_mic_button")
                    ) {
                        // Glowing pulse ring when listening
                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(micPulseScale)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.25f))
                            )
                        }

                        // Core Button
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isListening -> accent
                                        isSpeaking -> Color(0xFFFF5252)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isListening) Color.White else accent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isSpeaking -> Icons.Default.Stop
                                    else -> Icons.Default.Mic
                                },
                                contentDescription = if (isSpeaking) "Stop speech" else "Voice input",
                                tint = if (isListening || isSpeaking) Color.Black else accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Keyboard input toggle
                    IconButton(
                        onClick = { viewModel.toggleTextInput(!uiState.isTextInputVisible) },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
                            .testTag("keyboard_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keyboard input",
                            tint = if (uiState.isTextInputVisible) accent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Voice Speech Mute Toggle
                    IconButton(
                        onClick = { viewModel.toggleTtsMute() },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
                            .testTag("tts_mute_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isTtsMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Toggle voice speech",
                            tint = if (uiState.isTtsMuted) Color(0xFFFF5252) else accent
                        )
                    }
                }
            }

            // 6. Camera Vision Scanner Overlay
            AnimatedVisibility(
                visible = uiState.isCameraActive,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                CameraScannerOverlay(
                    onCaptureImage = { bitmap, prompt ->
                        viewModel.toggleCamera(false)
                        viewModel.processUserQuery(prompt, bitmap)
                    },
                    onClose = { viewModel.toggleCamera(false) }
                )
            }

            // 7. Command Cheat Sheet Matrix
            AnimatedVisibility(
                visible = uiState.isCheatSheetOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                CommandCheatSheet(
                    onSelectCommand = { cmd ->
                        viewModel.toggleCheatSheet(false)
                        viewModel.processUserQuery(cmd)
                    },
                    onClose = { viewModel.toggleCheatSheet(false) }
                )
            }

            // 8. Settings Panel
            AnimatedVisibility(
                visible = uiState.isSettingsOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                SettingsPanel(
                    currentTheme = uiState.currentTheme,
                    wakeWordEnabled = uiState.wakeWordEnabled,
                    preferredLanguage = uiState.preferredLanguage,
                    userName = uiState.userName,
                    speechRate = uiState.speechRate,
                    speechPitch = uiState.speechPitch,
                    onThemeChange = { viewModel.setTheme(it) },
                    onWakeWordToggle = { viewModel.setWakeWord(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onUserNameChange = { viewModel.setUserName(it) },
                    onSpeechRateChange = { viewModel.setSpeechRate(it) },
                    onSpeechPitchChange = { viewModel.setSpeechPitch(it) },
                    onClearData = { viewModel.clearAllData() },
                    onClose = { viewModel.toggleSettings(false) }
                )
            }

            // 9. Server Matrix & Auto-Switch Dialog
            AnimatedVisibility(
                visible = uiState.isServerMatrixOpen,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                com.example.ui.components.ServerMatrixDialog(
                    servers = uiState.servers,
                    activeServer = uiState.activeServer ?: (uiState.servers.firstOrNull() ?: com.example.data.model.ServerEndpoint("asia-se1", "Stark Relay", "Singapore", "")),
                    autoSwitchEnabled = uiState.autoSwitchEnabled,
                    isTesting = uiState.isTestingServers,
                    onAutoSwitchToggle = { viewModel.toggleAutoSwitch(it) },
                    onFindBestServer = { viewModel.findBestServer() },
                    onSelectServer = {
                        viewModel.selectServer(it)
                        viewModel.toggleServerMatrix(false)
                    },
                    onClose = { viewModel.toggleServerMatrix(false) }
                )
            }
        }
    }
}
