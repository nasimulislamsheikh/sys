package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.data.model.SubsystemState
import com.example.data.model.SubsystemType

object PermissionHelper {

  fun getAllRuntimePermissions(): Array<String> {
    val permissions = mutableSetOf<String>()
    SubsystemType.values().forEach { type ->
      permissions.addAll(type.getRequiredPermissions())
    }
    return permissions.toTypedArray()
  }

  fun checkSubsystemState(context: Context, type: SubsystemType): Pair<Boolean, Boolean> {
    val required = type.getRequiredPermissions()
    if (required.isEmpty()) {
      return Pair(true, false) // e.g. Network install-time permission
    }

    var grantedCount = 0
    for (perm in required) {
      if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
        grantedCount++
      }
    }

    val isAllGranted = grantedCount == required.size
    val isPartial = grantedCount in 1 until required.size
    return Pair(isAllGranted, isPartial)
  }

  fun getMissingPermissions(context: Context, type: SubsystemType): List<String> {
    return type.getRequiredPermissions().filter { perm ->
      ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
    }
  }

  fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
      data = Uri.fromParts("package", context.packageName, null)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    context.startActivity(intent)
  }
}
