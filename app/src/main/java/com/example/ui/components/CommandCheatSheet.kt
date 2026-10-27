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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RajdhaniFontFamily

data class CommandCategory(
    val title: String,
    val items: List<CommandItem>
)

data class CommandItem(
    val commandEn: String,
    val commandUrdu: String,
    val description: String
)

@Composable
fun CommandCheatSheet(
    onSelectCommand: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary

    val categories = listOf(
        CommandCategory(
            title = "1. VOICE & CORE CONVERSATION",
            items = listOf(
                CommandItem("How are you JARVIS?", "Sir, aap kaise hain?", "Check system status and friendly greeting."),
                CommandItem("Good morning", "Subah bakhair", "Trigger comprehensive daily briefing with weather & agenda."),
                CommandItem("Who are you?", "Aap kaun ho?", "Learn about JARVIS identity and capabilities."),
                CommandItem("Tell me a joke", "Koi joke sunao", "Calm Iron Man style wit and humor."),
                CommandItem("Tell me an interesting fact", "Koi dilchasp fact batao", "Receive curated technical or scientific intel.")
            )
        ),
        CommandCategory(
            title = "2. REAL-TIME TOOLS & NAVIGATION",
            items = listOf(
                CommandItem("Open YouTube Iron Man theme", "YouTube par gana chalao", "Stream video directly on YouTube."),
                CommandItem("Google search Quantum Computing", "Quantum Computing search karo", "Search web with Google grounding."),
                CommandItem("Open maps for Tokyo", "Tokyo kahan hai maps par dikhao", "View navigational coordinates in Google Maps."),
                CommandItem("Open website wikipedia.org", "wikipedia.org open karo", "Open any internet website instantly.")
            )
        ),
        CommandCategory(
            title = "3. CALLS & MESSAGING (CONFIRMED)",
            items = listOf(
                CommandItem("Call +1234567890", "+1234567890 par call milao", "Initiate phone call with voice authorization."),
                CommandItem("WhatsApp +1234567890 message I am ready", "WhatsApp par message bhejo", "Draft and send instant WhatsApp transmission."),
                CommandItem("Send email to sir@stark.com subject Meeting", "sir@stark.com ko email draft karo", "Prepare email draft with authorization.")
            )
        ),
        CommandCategory(
            title = "4. VISION SENSOR (CAMERA MODE)",
            items = listOf(
                CommandItem("Look at this, what is it?", "dekho yeh kya hai", "Activate camera and analyze scene with Gemini Multimodal."),
                CommandItem("Read the text in this document", "Ye text padho", "OCR text extraction and summarization."),
                CommandItem("Translate this sign to Urdu", "Is ka Urdu mein tarjuma karo", "Instant camera-based multilingual translation.")
            )
        ),
        CommandCategory(
            title = "5. TIMERS, NOTES & MEMORY",
            items = listOf(
                CommandItem("Set a timer for 45 seconds", "45 second ka timer lagao", "Active countdown timer with audio vibration alert."),
                CommandItem("Take note buy arc reactor parts", "Note likho ke arc reactor parts lene hain", "Save persistent note in local database."),
                CommandItem("Read my notes", "Mere notes parh kar sunao", "Retrieve and vocalize stored agenda."),
                CommandItem("Remember that my favorite city is Tokyo", "Yaad rakhna mera favorite city Tokyo hai", "Store persistent memory fact for future context.")
            )
        ),
        CommandCategory(
            title = "6. WEATHER, BATTERY & SYSTEM",
            items = listOf(
                CommandItem("Weather in London", "Karachi ka mausam kaisa hai?", "Real-time weather via Open-Meteo API."),
                CommandItem("Find best server", "Best server switch karo", "Benchmark latency across relays and auto-switch to fastest."),
                CommandItem("Battery status", "Power status check karo", "Real hardware battery capacity and charging."),
                CommandItem("Change theme to crimson", "Theme crimson karo", "Switch orb and HUD accents to Mark VII Red."),
                CommandItem("Switch to study mode", "Study mode activate karo", "Activate structured learning & quiz mode.")
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "COMMAND MATRIX // PROTOCOLS",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = accent
                )
                Text(
                    text = "Tap any command to test voice execution",
                    fontFamily = RajdhaniFontFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("close_cheatsheet_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Guide",
                    tint = accent
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories) { cat ->
                Column {
                    Text(
                        text = cat.title,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp,
                        color = accent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        cat.items.forEach { item ->
                            CommandCard(item = item, onClick = { onSelectCommand(item.commandUrdu) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandCard(
    item: CommandItem,
    onClick: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "\"${item.commandUrdu}\"",
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = accent
            )
            Text(
                text = "English: \"${item.commandEn}\"",
                fontFamily = RajdhaniFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.description,
                fontFamily = RajdhaniFontFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Run",
            tint = accent,
            modifier = Modifier.size(20.dp)
        )
    }
}
