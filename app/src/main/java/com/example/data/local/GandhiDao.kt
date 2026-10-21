package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GandhiDao {
    @Query("SELECT * FROM transcripts ORDER BY isPinned DESC, timestamp DESC")
    fun getAllTranscripts(): Flow<List<TranscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscript(transcript: TranscriptEntity): Long

    @Update
    suspend fun updateTranscript(transcript: TranscriptEntity)

    @Query("DELETE FROM transcripts WHERE id = :id")
    suspend fun deleteTranscriptById(id: Int)

    @Query("DELETE FROM transcripts")
    suspend fun wipeAllTranscripts()

    @Query("SELECT COUNT(*) FROM transcripts")
    suspend fun getTranscriptCount(): Int

    @Query("SELECT * FROM termux_scripts ORDER BY executionCount DESC, id ASC")
    fun getAllTermuxScripts(): Flow<List<TermuxScriptEntity>>

    @Query("SELECT * FROM termux_scripts")
    suspend fun getTermuxScriptsSnapshot(): List<TermuxScriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTermuxScript(script: TermuxScriptEntity): Long

    @Update
    suspend fun updateTermuxScript(script: TermuxScriptEntity)

    @Query("DELETE FROM termux_scripts WHERE id = :id")
    suspend fun deleteTermuxScriptById(id: Int)

    @Query("SELECT COUNT(*) FROM termux_scripts")
    suspend fun getTermuxScriptCount(): Int
}
