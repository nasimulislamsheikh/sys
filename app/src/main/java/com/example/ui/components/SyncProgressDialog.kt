package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SyncLog
import com.example.ui.theme.StatusDenied
import com.example.ui.theme.StatusGranted
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.SyncBlue

@Composable
fun SyncProgressDialog(
  isSyncing: Boolean,
  progress: Float,
  currentStep: String,
  lastLog: SyncLog?,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = { if (!isSyncing) onDismiss() }) {
    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("sync_progress_dialog"),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Icon header
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
              if (isSyncing) SyncBlue.copy(alpha = 0.12f)
              else StatusGranted.copy(alpha = 0.12f)
            ),
          contentAlignment = Alignment.Center
        ) {
          if (isSyncing) {
            CircularProgressIndicator(
              modifier = Modifier.size(36.dp),
              color = SyncBlue,
              strokeWidth = 3.dp
            )
          } else {
            Icon(
              imageVector = Icons.Filled.CheckCircle,
              contentDescription = null,
              tint = StatusGranted,
              modifier = Modifier.size(36.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = if (isSyncing) "Synchronizing Telemetry..." else "Data Sync Completed",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = if (isSyncing) currentStep else (lastLog?.summaryText ?: "Sync complete!"),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Progress indicator
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = SyncBlue,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          strokeCap = StrokeCap.Round
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "${(progress * 100).toInt()}% completed",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!isSyncing) {
          Spacer(modifier = Modifier.height(20.dp))

          lastLog?.let { log ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Subsystems Granted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Text(
                text = "${log.grantedCount} of ${log.totalCount} (${log.scorePercentage}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = if (log.scorePercentage == 100) StatusGranted else StatusWarning
              )
            }

            Spacer(modifier = Modifier.height(16.dp))
          }

          Button(
            onClick = onDismiss,
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("dialog_done_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = SyncBlue,
              contentColor = Color.White
            )
          ) {
            Text("Done", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }
  }
}
