package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PermissionAuditEvent
import com.example.data.model.SyncLog

@Database(entities = [SyncLog::class, PermissionAuditEvent::class], version = 2, exportSchema = false)
abstract class SyncDatabase : RoomDatabase() {
  abstract fun syncLogDao(): SyncLogDao
  abstract fun permissionAuditDao(): PermissionAuditDao

  companion object {
    @Volatile
    private var INSTANCE: SyncDatabase? = null

    fun getDatabase(context: Context): SyncDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          SyncDatabase::class.java,
          "sync_hub_database"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
