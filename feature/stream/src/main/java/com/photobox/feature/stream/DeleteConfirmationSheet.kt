package com.photobox.feature.stream

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteConfirmationSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            Text("确定删除吗？", style = MaterialTheme.typography.headlineSmall)
            Text(
                "系统会弹出确认窗口。在那里点击删除后，本张照片将从你的相册消失，且无法恢复。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text("继续")
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("取消")
            }
        }
    }
}
