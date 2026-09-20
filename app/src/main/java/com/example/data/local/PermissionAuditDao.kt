package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PermissionAuditEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface PermissionAuditDao {
  @Query("SELECT * FROM permission_audit_events ORDER BY timestamp DESC")
  fun getAllAuditEvents(): Flow<List<PermissionAuditEvent>>

  @Query("SELECT * FROM permission_audit_events ORDER BY timestamp DESC")
  suspend fun getAllAuditEventsList(): List<PermissionAuditEvent>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvent(event: PermissionAuditEvent)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEvents(events: List<PermissionAuditEvent>)

  @Query("DELETE FROM permission_audit_events")
  suspend fun clearAllEvents()

  @Query("SELECT COUNT(*) FROM permission_audit_events")
  suspend fun getCount(): Int
}
