package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.model.ConfirmationRequest
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

@Composable
fun ConfirmationCard(
    confirmation: ConfirmationRequest,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .border(1.5.dp, Color(0xFFFFD600), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (confirmation.actionType) {
                        ConfirmationRequest.ActionType.CALL -> Icons.Default.Call
                        ConfirmationRequest.ActionType.WHATSAPP -> Icons.Default.Message
                        ConfirmationRequest.ActionType.EMAIL -> Icons.Default.Email
                        ConfirmationRequest.ActionType.DELETE_DATA -> Icons.Default.Warning
                    },
                    contentDescription = "Warning",
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = confirmation.title.uppercase(),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFFFFD600)
                    )
                    Text(
                        text = "SAFETY PROTOCOL // CONFIRMATION REQUIRED",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = confirmation.description,
                fontFamily = RajdhaniFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = confirmation.onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("abort_action_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "ABORT",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        color = Color(0xFFFF5252)
                    )
                }

                Button(
                    onClick = confirmation.onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("authorize_action_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        text = "AUTHORIZE",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
