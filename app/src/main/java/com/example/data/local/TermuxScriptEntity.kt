package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "termux_scripts")
data class TermuxScriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val aliasHindi: String,
    val aliasEnglish: String,
    val scriptFileName: String,
    val defaultArgs: String = "",
    val description: String,
    val runInBackground: Boolean = true,
    val lastExecutedAt: Long? = null,
    val executionCount: Int = 0
)
