package com.cooper.wheellog.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONObject
import java.sql.*

@Entity(indices = [Index(value = ["fileName"], unique = true)])
data class TripEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Int = 0,
        var ecId: Int = -1,
        val fileName: String,
        var transportId: Int = -1,
        var mac: String = "",
        var profileName: String = "",
        var startTime: Int = 0,
        var url: String = "",
        var urlImage: String = "",
        var urlImage2: String = "",
        var duration: Int = -1,
        var maxSpeed: Float = -1f,
        var additionalJson: String? = null
)