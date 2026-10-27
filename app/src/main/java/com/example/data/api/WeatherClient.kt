package com.example.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WeatherInfo(
    val cityName: String,
    val country: String,
    val temperatureCelsius: Double,
    val humidityPercent: Int,
    val windSpeedKmh: Double,
    val conditionDescription: String,
    val conditionUrdu: String
)

class WeatherClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchWeather(city: String): Result<WeatherInfo> = withContext(Dispatchers.IO) {
        try {
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${java.net.URLEncoder.encode(city.trim(), "UTF-8")}&count=1&language=en&format=json"
            val geoRequest = Request.Builder().url(geoUrl).build()
            val geoResponse = client.newCall(geoRequest).execute()
            if (!geoResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Geocoding failed HTTP ${geoResponse.code}"))
            }

            val geoBody = geoResponse.body?.string() ?: return@withContext Result.failure(Exception("Empty geocoding response"))
            val geoJson = JSONObject(geoBody)
            val results = geoJson.optJSONArray("results")
            if (results == null || results.length() == 0) {
                return@withContext Result.failure(Exception("City '$city' not found."))
            }

            val firstLoc = results.getJSONObject(0)
            val lat = firstLoc.getDouble("latitude")
            val lon = firstLoc.getDouble("longitude")
            val name = firstLoc.getString("name")
            val country = firstLoc.optString("country", "")

            val weatherUrl = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
            val weatherRequest = Request.Builder().url(weatherUrl).build()
            val weatherResponse = client.newCall(weatherRequest).execute()
            if (!weatherResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Forecast failed HTTP ${weatherResponse.code}"))
            }

            val weatherBody = weatherResponse.body?.string() ?: return@withContext Result.failure(Exception("Empty weather response"))
            val weatherJson = JSONObject(weatherBody)
            val current = weatherJson.getJSONObject("current")

            val temp = current.getDouble("temperature_2m")
            val humidity = current.optInt("relative_humidity_2m", 50)
            val wind = current.optDouble("wind_speed_10m", 0.0)
            val code = current.optInt("weather_code", 0)

            val (descEn, descUrdu) = interpretWeatherCode(code)

            Result.success(
                WeatherInfo(
                    cityName = name,
                    country = country,
                    temperatureCelsius = temp,
                    humidityPercent = humidity,
                    windSpeedKmh = wind,
                    conditionDescription = descEn,
                    conditionUrdu = descUrdu
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun interpretWeatherCode(code: Int): Pair<String, String> {
        return when (code) {
            0 -> Pair("Clear Sky", "Bilkul saaf aasmaan (صاف آسمان)")
            1, 2 -> Pair("Mainly Clear to Partly Cloudy", "Halka badal (جزوی ابر آلود)")
            3 -> Pair("Overcast", "Aasmaan par ghane baadal (ابر آلود)")
            45, 48 -> Pair("Foggy", "Dhundh (دھند)")
            51, 53, 55 -> Pair("Drizzle", "Halki boondabaandi (ہلکی بونداباندی)")
            61, 63, 65 -> Pair("Rain", "Baarish (بارش)")
            71, 73, 75 -> Pair("Snow", "Barfbari (برف باری)")
            80, 81, 82 -> Pair("Rain Showers", "Tez baarish (تیز بارش)")
            95, 96, 99 -> Pair("Thunderstorm", "Toofan aur garaj chamak (طوفان اور گرج چمک)")
            else -> Pair("Fair", "Mausam theek hai (موسم ٹھیک ہے)")
        }
    }
}
