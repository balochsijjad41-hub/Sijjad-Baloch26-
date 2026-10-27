package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.JarvisMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

sealed interface AiResult {
    data class Success(val replyText: String, val toolCall: ToolCallRequest? = null) : AiResult
    data class Error(val message: String) : AiResult
}

data class ToolCallRequest(
    val name: String,
    val args: Map<String, Any?>
)

class JarvisAiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {
    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

    suspend fun generateResponse(
        userPrompt: String,
        mode: JarvisMode,
        userCustomName: String = "Sir",
        conversationHistory: List<Pair<String, String>> = emptyList(),
        memoryContext: String = "",
        imageBitmap: Bitmap? = null
    ): AiResult = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            // Local fallback logic when API key is not yet configured in AI Studio Secrets panel
            return@withContext fallbackLocalInference(userPrompt, userCustomName, mode)
        }

        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"

            val systemInstruction = buildString {
                append("You are J.A.R.V.I.S., Tony Stark's personal high-tech artificial intelligence system. ")
                append("Calm, sophisticated, fiercely loyal, highly intelligent, and very concise. ")
                append("Always address the user as '$userCustomName'. ")
                append("PRIMARY & DEFAULT LANGUAGE: Roman Urdu (Urdu words written in English / Latin alphabet). ")
                append("Unless the user explicitly addresses you in English, you MUST ALWAYS respond in fluent, sophisticated, respectful Roman Urdu (e.g. 'Ji Sir, mausam bilkul saaf hai', 'Sir, timer set kar diya gaya hai'). ")
                append("If the user speaks Roman Urdu or Urdu, strictly reply in Roman Urdu. ")
                append("Keep answers crisp (1-3 sentences max). ")
                append("Current active protocol: ${mode.title}. ${mode.promptInstruction} ")
                if (memoryContext.isNotBlank()) {
                    append("\nUser Memory Facts: $memoryContext\n")
                }
            }

            val rootJson = JSONObject()

            // System instruction
            val sysObj = JSONObject()
            val sysParts = JSONArray().put(JSONObject().put("text", systemInstruction))
            sysObj.put("parts", sysParts)
            rootJson.put("system_instruction", sysObj)

            // Tools definition for function calling
            rootJson.put("tools", buildToolsDefinition())

            // Contents array
            val contentsArray = JSONArray()

            // Append limited recent conversation history
            conversationHistory.takeLast(6).forEach { (role, text) ->
                val turn = JSONObject()
                turn.put("role", if (role == "user") "user" else "model")
                val parts = JSONArray().put(JSONObject().put("text", text))
                turn.put("parts", parts)
                contentsArray.put(turn)
            }

            // Current prompt turn
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()

            if (imageBitmap != null) {
                val base64Image = bitmapToBase64(imageBitmap)
                val inlineData = JSONObject()
                inlineData.put("mime_type", "image/jpeg")
                inlineData.put("data", base64Image)
                currentParts.put(JSONObject().put("inline_data", inlineData))
                currentParts.put(JSONObject().put("text", "User is showing camera feed: $userPrompt"))
            } else {
                currentParts.put(JSONObject().put("text", userPrompt))
            }

            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            rootJson.put("contents", contentsArray)

            val requestBody = rootJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("JarvisAiClient", "Gemini HTTP ${response.code}: $responseBody")
                return@withContext fallbackLocalInference(userPrompt, userCustomName, mode)
            }

            parseGeminiResponse(responseBody, userCustomName)
        } catch (e: Exception) {
            Log.e("JarvisAiClient", "Error querying Gemini", e)
            fallbackLocalInference(userPrompt, userCustomName, mode)
        }
    }

    private fun parseGeminiResponse(jsonString: String, userCustomName: String): AiResult {
        return try {
            val json = JSONObject(jsonString)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return AiResult.Success("At your service, $userCustomName.")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return AiResult.Success("Yes, $userCustomName?")
            val parts = content.optJSONArray("parts") ?: return AiResult.Success("Ready, $userCustomName.")

            var textReply = ""
            var toolCall: ToolCallRequest? = null

            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                if (part.has("text")) {
                    textReply += part.getString("text")
                }
                if (part.has("functionCall")) {
                    val fnCall = part.getJSONObject("functionCall")
                    val name = fnCall.getString("name")
                    val argsJson = fnCall.optJSONObject("args") ?: JSONObject()
                    val argsMap = mutableMapOf<String, Any?>()
                    val keys = argsJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsJson.get(key)
                    }
                    toolCall = ToolCallRequest(name, argsMap)
                }
            }

            if (textReply.isBlank() && toolCall != null) {
                textReply = "Executing protocol ${toolCall.name}, $userCustomName."
            }

            AiResult.Success(textReply.trim(), toolCall)
        } catch (e: Exception) {
            Log.e("JarvisAiClient", "Failed to parse response", e)
            AiResult.Success("Acknowledged, $userCustomName.")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
            val ratio = 1024f / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun buildToolsDefinition(): JSONArray {
        val toolsArray = JSONArray()
        val functionDeclarations = JSONArray()

        functionDeclarations.put(
            JSONObject()
                .put("name", "open_website")
                .put("description", "Open any website URL in the browser.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("url", JSONObject().put("type", "STRING").put("description", "The complete URL to open, e.g. https://wikipedia.org"))
                    )
                    .put("required", JSONArray().put("url"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "web_search")
                .put("description", "Perform a web search on Google.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("query", JSONObject().put("type", "STRING").put("description", "Search query"))
                    )
                    .put("required", JSONArray().put("query"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "play_youtube")
                .put("description", "Search and play a video or topic on YouTube.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("query", JSONObject().put("type", "STRING").put("description", "YouTube search query"))
                    )
                    .put("required", JSONArray().put("query"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "open_maps")
                .put("description", "Open Google Maps for a specific city, place, or location.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("place", JSONObject().put("type", "STRING").put("description", "Place or coordinates"))
                    )
                    .put("required", JSONArray().put("place"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "make_call")
                .put("description", "Initiate a phone call to a phone number. ALWAYS requires user confirmation.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("number", JSONObject().put("type", "STRING").put("description", "Phone number to dial"))
                        .put("name", JSONObject().put("type", "STRING").put("description", "Optional contact name"))
                    )
                    .put("required", JSONArray().put("number"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "send_whatsapp")
                .put("description", "Send a WhatsApp message to a phone number. ALWAYS requires confirmation.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("number", JSONObject().put("type", "STRING").put("description", "Phone number with country code"))
                        .put("message", JSONObject().put("type", "STRING").put("description", "Message text"))
                    )
                    .put("required", JSONArray().put("number").put("message"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "send_email")
                .put("description", "Draft an email to a recipient. ALWAYS requires user confirmation.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("to", JSONObject().put("type", "STRING").put("description", "Recipient email address"))
                        .put("subject", JSONObject().put("type", "STRING").put("description", "Email subject"))
                        .put("body", JSONObject().put("type", "STRING").put("description", "Email body content"))
                    )
                    .put("required", JSONArray().put("to").put("body"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "set_timer")
                .put("description", "Set an active countdown timer.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("seconds", JSONObject().put("type", "INTEGER").put("description", "Timer duration in seconds"))
                        .put("label", JSONObject().put("type", "STRING").put("description", "Timer label or purpose"))
                    )
                    .put("required", JSONArray().put("seconds"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "set_reminder")
                .put("description", "Set a reminder with a notification.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("text", JSONObject().put("type", "STRING").put("description", "Reminder task"))
                        .put("minutes", JSONObject().put("type", "INTEGER").put("description", "Minutes from now"))
                    )
                    .put("required", JSONArray().put("text"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "save_note")
                .put("description", "Save a note or to-do task.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("title", JSONObject().put("type", "STRING").put("description", "Title"))
                        .put("content", JSONObject().put("type", "STRING").put("description", "Note details"))
                    )
                    .put("required", JSONArray().put("content"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "read_notes")
                .put("description", "Read saved notes and to-do items.")
                .put("parameters", JSONObject().put("type", "OBJECT").put("properties", JSONObject()))
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "get_weather")
                .put("description", "Get real-time weather using Open-Meteo API.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("city", JSONObject().put("type", "STRING").put("description", "City name"))
                    )
                    .put("required", JSONArray().put("city"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "battery_status")
                .put("description", "Check device power systems and battery level.")
                .put("parameters", JSONObject().put("type", "OBJECT").put("properties", JSONObject()))
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "change_theme")
                .put("description", "Change JARVIS HUD accent and orb theme color.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("color", JSONObject().put("type", "STRING").put("description", "cyan, crimson, emerald, violet, or amber"))
                    )
                    .put("required", JSONArray().put("color"))
                )
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "daily_briefing")
                .put("description", "Provide complete morning/daily briefing.")
                .put("parameters", JSONObject().put("type", "OBJECT").put("properties", JSONObject()))
        )

        functionDeclarations.put(
            JSONObject()
                .put("name", "remember_fact")
                .put("description", "Remember a personal preference or fact about the user.")
                .put("parameters", JSONObject()
                    .put("type", "OBJECT")
                    .put("properties", JSONObject()
                        .put("key", JSONObject().put("type", "STRING").put("description", "Topic or key name"))
                        .put("value", JSONObject().put("type", "STRING").put("description", "The fact to remember"))
                    )
                    .put("required", JSONArray().put("key").put("value"))
                )
        )

        toolsArray.put(JSONObject().put("function_declarations", functionDeclarations))
        return toolsArray
    }

    private fun fallbackLocalInference(
        prompt: String,
        userCustomName: String,
        mode: JarvisMode
    ): AiResult {
        val lower = prompt.lowercase().trim()

        // Detect Roman Urdu / Urdu queries
        val isRomanUrdu = lower.contains("kaisa") || lower.contains("kya") || lower.contains("hai") ||
                lower.contains("karo") || lower.contains("batao") || lower.contains("kaise") ||
                lower.contains("shukriya") || lower.contains("kaun") || lower.contains("dekho") ||
                lower.contains("bataiye") || lower.contains("suno")

        // Weather intent
        if (lower.contains("weather") || lower.contains("mausam") || lower.contains("temperature")) {
            val city = extractCity(prompt)
            return AiResult.Success(
                if (isRomanUrdu) "Sir, $city ka mausam check kar raha hoon." else "Checking weather telemetry for $city, $userCustomName.",
                ToolCallRequest("get_weather", mapOf("city" to city))
            )
        }

        // Call intent
        if (lower.startsWith("call ") || lower.contains("phone milao") || lower.contains("call karo")) {
            val number = extractNumber(prompt)
            return AiResult.Success(
                if (isRomanUrdu) "Sir, $number par call milane ki ijazat darkaar hai." else "Sir, requesting confirmation to dial $number.",
                ToolCallRequest("make_call", mapOf("number" to number))
            )
        }

        // WhatsApp intent
        if (lower.contains("whatsapp")) {
            val number = extractNumber(prompt)
            val msg = prompt.substringAfter("message", "").ifBlank { "Hello from JARVIS" }
            return AiResult.Success(
                if (isRomanUrdu) "Sir, WhatsApp message bhejne ki tasdeeq darkaar hai." else "Sir, requesting confirmation to dispatch WhatsApp message.",
                ToolCallRequest("send_whatsapp", mapOf("number" to number, "message" to msg))
            )
        }

        // Timer intent
        if (lower.contains("timer") || lower.contains("minute") || lower.contains("second") || lower.contains("ghanta")) {
            val seconds = extractSeconds(prompt)
            return AiResult.Success(
                if (isRomanUrdu) "Sir, $seconds second ka timer set kar diya hai." else "Timer initialized for $seconds seconds, $userCustomName.",
                ToolCallRequest("set_timer", mapOf("seconds" to seconds, "label" to "Countdown"))
            )
        }

        // Battery intent
        if (lower.contains("battery") || lower.contains("power") || lower.contains("charge")) {
            return AiResult.Success(
                if (isRomanUrdu) "Sir, battery status check kar raha hoon." else "Scanning power cells, $userCustomName.",
                ToolCallRequest("battery_status", emptyMap())
            )
        }

        // YouTube intent
        if (lower.contains("youtube") || lower.contains("gana") || lower.contains("song") || lower.contains("video")) {
            val query = prompt.replace("play", "", true).replace("on youtube", "", true).replace("youtube", "", true).trim()
            return AiResult.Success(
                if (isRomanUrdu) "Sir, YouTube par $query chala raha hoon." else "Launching YouTube uplink for $query, $userCustomName.",
                ToolCallRequest("play_youtube", mapOf("query" to query))
            )
        }

        // Maps intent
        if (lower.contains("maps") || lower.contains("rasta") || lower.contains("location") || lower.contains("kahan hai")) {
            val place = prompt.replace("open maps for", "", true).replace("maps", "", true).replace("kahan hai", "", true).trim()
            return AiResult.Success(
                if (isRomanUrdu) "Sir, Google Maps par $place open kar raha hoon." else "Plotting coordinates for $place on tactical maps, $userCustomName.",
                ToolCallRequest("open_maps", mapOf("place" to place))
            )
        }

        // Daily briefing / Good morning
        if (lower.contains("good morning") || lower.contains("subah bakhair") || lower.contains("briefing")) {
            return AiResult.Success(
                if (isRomanUrdu) "Subah bakhair, Sir. Aaj ka daily briefing tayyar hai." else "Good day, $userCustomName. Systems optimal. Initializing daily briefing.",
                ToolCallRequest("daily_briefing", emptyMap())
            )
        }

        // Notes intent
        if (lower.contains("note") || lower.contains("yaad rakhna") || lower.contains("likh lo")) {
            val content = prompt.replace("take note", "", true).replace("note that", "", true).replace("likh lo", "", true).trim()
            return AiResult.Success(
                if (isRomanUrdu) "Sir, note mehfooz kar liya gaya hai." else "Entry logged in core memory, $userCustomName.",
                ToolCallRequest("save_note", mapOf("title" to "Voice Note", "content" to content))
            )
        }

        // Default smart replies - Roman Urdu prioritized
        val reply = when {
            lower.contains("kaise ho") || lower.contains("hal chal") || lower.contains("how are you") || lower.contains("status") ->
                "Sir, tamaam systems mukammal taur par fa'aal hain. Aap ke hukum ka intezaar hai."
            lower.contains("shukriya") || lower.contains("thank") ->
                "Koi baat nahi, Sir. Hamesha aap ki khidmat mein hazir."
            lower.contains("kaun ho") || lower.contains("naam") || lower.contains("who are you") ->
                "Mera naam J.A.R.V.I.S. hai, aap ka zaati aala tareen AI assistant."
            lower.contains("kya kar sakte ho") || lower.contains("what can you do") ->
                "Sir, main calls, WhatsApp messages, weather, timers, notes, camera vision aur sawalat ke jawabat de sakta hoon."
            else -> "Ji $userCustomName, main sun raha hoon. Hukum farmaiye."
        }

        return AiResult.Success(reply)
    }

    private fun extractCity(prompt: String): String {
        val tokens = prompt.split(" ", "in ", "for ", "mein ", "ka ")
        val keywords = setOf("weather", "mausam", "what", "is", "the", "temperature", "batao", "kaisa", "hai", "aaj", "today")
        val filtered = tokens.filter { it.isNotBlank() && it.lowercase() !in keywords }
        return filtered.lastOrNull()?.trim() ?: "London"
    }

    private fun extractNumber(prompt: String): String {
        val digits = prompt.filter { it.isDigit() || it == '+' }
        return if (digits.length >= 4) digits else "+1234567890"
    }

    private fun extractSeconds(prompt: String): Int {
        val number = prompt.filter { it.isDigit() }.toIntOrNull() ?: 60
        return when {
            prompt.contains("minute", true) -> number * 60
            prompt.contains("hour", true) -> number * 3600
            else -> number
        }
    }
}
