package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.JarvisMode
import com.example.data.model.ServerEndpoint
import com.example.data.model.ServerStatus
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun HudHeader(
    currentMode: JarvisMode,
    batteryPercent: Int,
    isCharging: Boolean,
    activeServer: ServerEndpoint?,
    onServerClick: () -> Unit,
    onModeClick: () -> Unit,
    onHelpClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Title & Tactical Designation
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "J.A.R.V.I.S.",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 1.2.sp,
                color = accent
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "v8.5",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Server Pill + Mode Pill + Battery + Actions
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Live Server Relay Pill
            activeServer?.let { srv ->
                val statusDotColor = when (srv.status) {
                    ServerStatus.OPTIMAL -> Color(0xFF00E676)
                    ServerStatus.STABLE -> accent
                    ServerStatus.HIGH_LATENCY -> Color(0xFFFFD600)
                    ServerStatus.OFFLINE -> Color(0xFFFF5252)
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, statusDotColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onServerClick() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("server_status_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(statusDotColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (srv.latencyMs > 0) "${srv.latencyMs}ms" else "SRV",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusDotColor
                    )
                }
            }

            // Mode Selector Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable { onModeClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("mode_selector_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentMode.name.take(4),
                    fontFamily = OrbitronFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accent
                )
            }

            // Battery Tag
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = "Battery Status",
                    modifier = Modifier.size(13.dp),
                    tint = if (batteryPercent < 20) Color(0xFFFF5252) else accent
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "$batteryPercent%",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Command Matrix Button
            IconButton(
                onClick = onHelpClick,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("command_matrix_button")
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Command Guide",
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Settings Button
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Jarvis Settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
