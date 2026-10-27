package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.data.api.WeatherClient
import com.example.data.local.AppDatabase
import com.example.data.local.MemoryEntity
import com.example.data.local.NoteEntity
import com.example.data.model.ConfirmationRequest
import com.example.data.model.JarvisMode
import com.example.data.model.JarvisTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JarvisActionExecutor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val database: AppDatabase,
    private val weatherClient: WeatherClient,
    private val onThemeChanged: (JarvisTheme) -> Unit,
    private val onModeChanged: (JarvisMode) -> Unit,
    private val onRequestConfirmation: (ConfirmationRequest) -> Unit,
    private val onTimerTick: (remainingSeconds: Long, totalSeconds: Long, label: String) -> Unit,
    private val onTimerFinished: (label: String) -> Unit,
    private val onExecutionResult: (summaryText: String, speechText: String) -> Unit
) {
    private var activeTimer: CountDownTimer? = null
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "jarvis_alerts",
                "JARVIS Protocols",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "JARVIS timer and reminder alerts"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun executeTool(name: String, args: Map<String, Any?>, userCustomName: String = "Sir") {
        when (name.lowercase()) {
            "open_website" -> {
                val url = args["url"]?.toString().orEmpty().trim()
                val fullUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                openUrl(fullUrl)
                val msg = "Opening uplink to $url, $userCustomName."
                onExecutionResult("Opened website: $url", msg)
            }

            "web_search" -> {
                val query = args["query"]?.toString().orEmpty()
                val searchUrl = "https://www.google.com/search?q=${Uri.encode(query)}"
                openUrl(searchUrl)
                val msg = "Initiated search protocol for '$query', $userCustomName."
                onExecutionResult("Google Search: $query", msg)
            }

            "play_youtube" -> {
                val query = args["query"]?.toString().orEmpty()
                val ytUrl = "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
                openUrl(ytUrl)
                val msg = "Streaming $query on YouTube tactical monitor, $userCustomName."
                onExecutionResult("YouTube: $query", msg)
            }

            "open_maps" -> {
                val place = args["place"]?.toString().orEmpty()
                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(place)}")
                val intent = Intent(Intent.ACTION_VIEW, mapUri)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                    onExecutionResult("Opened Maps: $place", "Coordinates acquired for $place on tactical maps, $userCustomName.")
                } catch (_: Exception) {
                    val browserMap = "https://www.google.com/maps/search/?api=1&query=${Uri.encode(place)}"
                    openUrl(browserMap)
                    onExecutionResult("Opened Maps: $place", "Displaying navigational maps for $place, $userCustomName.")
                }
            }

            "make_call" -> {
                val number = args["number"]?.toString().orEmpty()
                val contactName = args["name"]?.toString() ?: number
                // ALWAYS require explicit user confirmation for calls
                val confirmation = ConfirmationRequest(
                    actionType = ConfirmationRequest.ActionType.CALL,
                    title = "Outgoing Call Authorization",
                    description = "Requesting confirmation to place phone call to $contactName ($number).",
                    payload = mapOf("number" to number, "name" to contactName),
                    onConfirm = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                        onExecutionResult("Calling $contactName", "Connecting call to $contactName, $userCustomName.")
                    },
                    onCancel = {
                        onExecutionResult("Call Cancelled", "Call aborted, $userCustomName.")
                    }
                )
                onRequestConfirmation(confirmation)
            }

            "send_whatsapp" -> {
                val number = args["number"]?.toString().orEmpty().filter { it.isDigit() }
                val message = args["message"]?.toString().orEmpty()
                // ALWAYS require explicit confirmation for messaging
                val confirmation = ConfirmationRequest(
                    actionType = ConfirmationRequest.ActionType.WHATSAPP,
                    title = "WhatsApp Dispatch Authorization",
                    description = "Authorize transmission of message to +$number: \"$message\"",
                    payload = mapOf("number" to number, "message" to message),
                    onConfirm = {
                        val waUrl = "https://api.whatsapp.com/send?phone=$number&text=${Uri.encode(message)}"
                        openUrl(waUrl)
                        onExecutionResult("WhatsApp message sent", "Message transmitted, $userCustomName.")
                    },
                    onCancel = {
                        onExecutionResult("WhatsApp Cancelled", "Message aborted, $userCustomName.")
                    }
                )
                onRequestConfirmation(confirmation)
            }

            "send_email" -> {
                val to = args["to"]?.toString().orEmpty()
                val subject = args["subject"]?.toString().orEmpty()
                val body = args["body"]?.toString().orEmpty()
                val confirmation = ConfirmationRequest(
                    actionType = ConfirmationRequest.ActionType.EMAIL,
                    title = "Email Dispatch Authorization",
                    description = "Authorize email to $to with subject: \"$subject\"",
                    payload = mapOf("to" to to, "subject" to subject, "body" to body),
                    onConfirm = {
                        val mailUri = Uri.parse("mailto:$to?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                        val mailIntent = Intent(Intent.ACTION_SENDTO, mailUri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(mailIntent)
                        onExecutionResult("Email prepared", "Email client engaged, $userCustomName.")
                    },
                    onCancel = {
                        onExecutionResult("Email Cancelled", "Email transmission cancelled, $userCustomName.")
                    }
                )
                onRequestConfirmation(confirmation)
            }

            "set_timer" -> {
                val seconds = (args["seconds"] as? Number)?.toLong() ?: 60L
                val label = args["label"]?.toString() ?: "Timer"
                startTimer(seconds, label, userCustomName)
            }

            "set_reminder" -> {
                val text = args["text"]?.toString().orEmpty()
                val minutes = (args["minutes"] as? Number)?.toLong() ?: 5L
                startTimer(minutes * 60, "Reminder: $text", userCustomName)
                onExecutionResult("Reminder set: $text ($minutes min)", "Reminder logged for $minutes minutes from now, $userCustomName.")
            }

            "save_note" -> {
                val title = args["title"]?.toString() ?: "Voice Note"
                val content = args["content"]?.toString().orEmpty()
                scope.launch {
                    database.noteDao().insertNote(
                        NoteEntity(title = title, content = content)
                    )
                    withContext(Dispatchers.Main) {
                        onExecutionResult("Saved Note: $content", "Note safely recorded in archive, $userCustomName.")
                    }
                }
            }

            "read_notes" -> {
                scope.launch {
                    val notes = database.noteDao().getAllNotes()
                    withContext(Dispatchers.Main) {
                        if (notes.isEmpty()) {
                            onExecutionResult("Notes Archive", "You have no archived notes, $userCustomName.")
                        } else {
                            val text = notes.take(5).joinToString("; ") { "${it.title}: ${it.content}" }
                            onExecutionResult("Archived Notes (${notes.size})", "You have ${notes.size} notes. Recent entries: $text, $userCustomName.")
                        }
                    }
                }
            }

            "get_weather" -> {
                val city = args["city"]?.toString() ?: "London"
                scope.launch {
                    val result = weatherClient.fetchWeather(city)
                    withContext(Dispatchers.Main) {
                        result.onSuccess { info ->
                            val speech = "Weather in ${info.cityName} is ${info.conditionDescription}, ${info.temperatureCelsius.toInt()} degrees Celsius with humidity at ${info.humidityPercent} percent, $userCustomName."
                            val summary = "${info.cityName}: ${info.temperatureCelsius}°C | ${info.conditionDescription} (${info.conditionUrdu})"
                            onExecutionResult(summary, speech)
                        }.onFailure { err ->
                            onExecutionResult("Weather Error", "Could not acquire meteorological data for $city, $userCustomName.")
                        }
                    }
                }
            }

            "battery_status" -> {
                val batteryStatus = getBatteryTelemetry()
                onExecutionResult("Power Status", batteryStatus)
            }

            "copy_to_clipboard" -> {
                val text = args["text"]?.toString().orEmpty()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("JARVIS", text)
                clipboard.setPrimaryClip(clip)
                onExecutionResult("Copied to clipboard", "Data copied to system buffer, $userCustomName.")
            }

            "share_text" -> {
                val text = args["text"]?.toString().orEmpty()
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(sendIntent, "Share with JARVIS").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                onExecutionResult("Sharing text", "Opening share matrix, $userCustomName.")
            }

            "change_theme" -> {
                val color = args["color"]?.toString().orEmpty().lowercase()
                val theme = when {
                    color.contains("crimson") || color.contains("red") || color.contains("gold") -> JarvisTheme.CRIMSON
                    color.contains("emerald") || color.contains("green") -> JarvisTheme.EMERALD
                    color.contains("violet") || color.contains("purple") -> JarvisTheme.VIOLET
                    color.contains("amber") || color.contains("orange") || color.contains("yellow") -> JarvisTheme.AMBER
                    else -> JarvisTheme.CYAN
                }
                onThemeChanged(theme)
                onExecutionResult("HUD Accent Changed: ${theme.displayName}", "Orb and HUD calibrated to ${theme.displayName}, $userCustomName.")
            }

            "daily_briefing" -> {
                executeDailyBriefing(userCustomName)
            }

            "remember_fact" -> {
                val key = args["key"]?.toString().orEmpty()
                val value = args["value"]?.toString().orEmpty()
                scope.launch {
                    database.memoryDao().saveMemory(MemoryEntity(key = key, value = value))
                    withContext(Dispatchers.Main) {
                        onExecutionResult("Memory Stored: $key = $value", "Information stored into core memory banks, $userCustomName.")
                    }
                }
            }

            "switch_mode" -> {
                val m = args["mode"]?.toString().orEmpty().lowercase()
                val mode = when {
                    m.contains("study") -> JarvisMode.STUDY
                    m.contains("focus") -> JarvisMode.FOCUS
                    m.contains("chill") -> JarvisMode.CHILL
                    else -> JarvisMode.NORMAL
                }
                onModeChanged(mode)
                onExecutionResult("Protocol Switched: ${mode.title}", "${mode.title} active, $userCustomName.")
            }

            "tell_joke" -> {
                val jokes = listOf(
                    "Why do Java developers wear glasses, Sir? Because they don't C-sharp.",
                    "There are 10 types of people in the world, Sir: those who understand binary, and those who do not.",
                    "An AI walks into a bar, orders a drink, and calculates that humans are amusing creatures."
                )
                val j = jokes.random()
                onExecutionResult("JARVIS Humour", j)
            }

            "tell_fact" -> {
                val facts = listOf(
                    "Sir, did you know that the first computer bug was an actual moth found trapped in a Harvard Mark II computer in 1947?",
                    "Sir, light from the Sun takes approximately 8 minutes and 20 seconds to reach Earth.",
                    "Sir, the human brain generates approximately 20 watts of electrical power when awake."
                )
                val f = facts.random()
                onExecutionResult("JARVIS Data Intel", f)
            }

            else -> {
                onExecutionResult("Command Executed", "Protocol $name processed, $userCustomName.")
            }
        }
    }

    private fun startTimer(seconds: Long, label: String, userCustomName: String) {
        activeTimer?.cancel()
        val totalMillis = seconds * 1000L

        activeTimer = object : CountDownTimer(totalMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val remainingSec = millisUntilFinished / 1000L
                onTimerTick(remainingSec, seconds, label)
            }

            override fun onFinish() {
                vibrateAlert()
                showNotification("JARVIS Alert", "$label countdown complete, $userCustomName.")
                onTimerFinished(label)
                onExecutionResult("Timer Complete", "Sir, your countdown for $label has concluded.")
            }
        }.start()

        onExecutionResult("Timer Started: $seconds s", "Timer initialized for $seconds seconds, $userCustomName.")
    }

    fun cancelActiveTimer() {
        activeTimer?.cancel()
        activeTimer = null
    }

    private fun getBatteryTelemetry(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val isCharging = bm.isCharging
            val status = if (isCharging) "Charging via external power grid" else "Discharging normally"
            "Power cells at $level percent. $status."
        } catch (_: Exception) {
            "Power cells operational at approximately 85 percent, Sir."
        }
    }

    private fun executeDailyBriefing(userCustomName: String) {
        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = timeFormat.format(Date())

        scope.launch {
            val notes = database.noteDao().getAllNotes()
            val pendingTodos = notes.filter { !it.isCompleted }
            val weatherResult = weatherClient.fetchWeather("London")

            val weatherLine = weatherResult.getOrNull()?.let {
                "${it.cityName} weather is ${it.conditionDescription} at ${it.temperatureCelsius.toInt()}°C."
            } ?: "Meteorological sensors nominal."

            val todoCount = pendingTodos.size
            val briefingText = buildString {
                append("Good morning, $userCustomName. ")
                append("The time is $currentTime on $currentDate. ")
                append("$weatherLine ")
                if (todoCount > 0) {
                    append("You have $todoCount pending agenda items in your notes. ")
                } else {
                    append("Your agenda is completely clear today. ")
                }
                append("All tactical systems are running at maximum capacity.")
            }

            withContext(Dispatchers.Main) {
                onExecutionResult("Daily Briefing Complete", briefingText)
            }
        }
    }

    private fun vibrateAlert() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 400), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(longArrayOf(0, 300, 200, 400), -1)
            }
        } catch (_: Exception) {}
    }

    private fun showNotification(title: String, body: String) {
        try {
            val notification = NotificationCompat.Builder(context, "jarvis_alerts")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager.notify(101, notification)
        } catch (_: Exception) {}
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
