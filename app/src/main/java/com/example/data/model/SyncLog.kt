package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_logs")
data class SyncLog(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val status: String, // "COMPLETED", "PARTIAL", "BLOCKED"
  val grantedCount: Int,
  val totalCount: Int,
  val scorePercentage: Int,
  val summaryText: String,
  val detailsJson: String = ""
)
