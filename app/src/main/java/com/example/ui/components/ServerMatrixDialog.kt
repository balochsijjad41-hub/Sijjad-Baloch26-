package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ServerEndpoint
import com.example.data.model.ServerStatus
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun ServerMatrixDialog(
    servers: List<ServerEndpoint>,
    activeServer: ServerEndpoint,
    autoSwitchEnabled: Boolean,
    isTesting: Boolean,
    onAutoSwitchToggle: (Boolean) -> Unit,
    onFindBestServer: () -> Unit,
    onSelectServer: (ServerEndpoint) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "server_spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SERVER MATRIX // TELEMETRY",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accent
                )
                Text(
                    text = "Automatic lowest latency endpoint failover",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("close_server_matrix_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Auto Switch Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AUTOMATIC SERVER SWITCH",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Continuously routes to lowest ping relay",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoSwitchEnabled,
                onCheckedChange = onAutoSwitchToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accent,
                    checkedTrackColor = accent.copy(alpha = 0.4f)
                ),
                modifier = Modifier.testTag("auto_switch_toggle")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Find Best Server Action Button
        Button(
            onClick = onFindBestServer,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("benchmark_best_server_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent),
            enabled = !isTesting
        ) {
            Icon(
                imageVector = if (isTesting) Icons.Default.Refresh else Icons.Default.Speed,
                contentDescription = "Find Best",
                tint = Color.Black,
                modifier = Modifier
                    .size(20.dp)
                    .then(if (isTesting) Modifier.rotate(spinAngle) else Modifier)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTesting) "BENCHMARKING SERVERS..." else "FIND & CONNECT BEST SERVER",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AVAILABLE SATELLITE RELAYS",
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = accent,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Server list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(servers, key = { it.id }) { server ->
                val isSelected = server.id == activeServer.id
                val (statusColor, statusLabel) = when (server.status) {
                    ServerStatus.OPTIMAL -> Color(0xFF00E676) to "OPTIMAL"
                    ServerStatus.STABLE -> accent to "STABLE"
                    ServerStatus.HIGH_LATENCY -> Color(0xFFFFD600) to "SLOW"
                    ServerStatus.OFFLINE -> Color(0xFFFF5252) to "OFFLINE"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) accent.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            1.dp,
                            if (isSelected) accent else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onSelectServer(server) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ping indicator dot
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = server.name,
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "[ACTIVE]",
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                }
                            }
                            Text(
                                text = "${server.region} • ${statusLabel}",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (server.latencyMs > 0 && server.status != ServerStatus.OFFLINE) "${server.latencyMs}ms" else "--",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = statusColor
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
