package com.photobox.feature.stream

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * ActivityResultLauncher 封装：每次系统弹窗返回时，把成功状态传给 onResult。
 * 配合 VM.onDeleteConfirmed(mediaId, success) 调用。
 */
@Composable
fun rememberDeleteRequestLauncher(
    onResult: (success: Boolean) -> Unit,
) = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartIntentSenderForResult(),
) { result ->
    onResult(result.resultCode == android.app.Activity.RESULT_OK)
}
