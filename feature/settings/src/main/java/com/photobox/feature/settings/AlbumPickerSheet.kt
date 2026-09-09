package com.photobox.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerSheet(
    albums: List<String>,
    currentSelection: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.padding(bottom = 16.dp)) {
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_album_all)) },
                    modifier = Modifier.clickable { onSelect(null) },
                )
            }
            items(albums, key = { it }) { album ->
                ListItem(
                    headlineContent = { Text(album) },
                    supportingContent = if (album == currentSelection) {
                        { Text("当前选择", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                    modifier = Modifier.clickable { onSelect(album) },
                )
            }
        }
    }
}
