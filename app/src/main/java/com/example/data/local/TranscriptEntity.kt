package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "transcripts")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val detectedLanguage: String, // "HI" or "EN"
    val userQuery: String,
    val assistantResponse: String,
    val executedAction: String? = null, // e.g., "Termux: backup.py" or "App: Settings"
    val processingMode: String = "STRICT_OFFLINE", // "STRICT_OFFLINE" or "HYBRID_GEMINI"
    val latencyMs: Long = 140L,
    val isPinned: Boolean = false
)
