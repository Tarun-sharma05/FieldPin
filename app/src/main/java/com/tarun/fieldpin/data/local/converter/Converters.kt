package com.tarun.fieldpin.data.local.converter

import androidx.room.TypeConverter
import com.tarun.fieldpin.domain.model.LatLng
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import org.maplibre.compose.sources.rememberMbtilesUrl

class Converters {

    @TypeConverter
    fun fromLatLngList(value: List<LatLng>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toLatLngList(value: String): List<LatLng> {
        return if (value.isBlank()) {
            emptyList()
        }else {
            Json.decodeFromString(value)
        }
    }
}