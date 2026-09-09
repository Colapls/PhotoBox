package com.photobox.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photobox.core.data.datastore.FilterMode
import com.photobox.core.data.datastore.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showAlbumPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.toast) {
        state.toast?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeToast()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { SectionHeader(stringResource(R.string.settings_section_filter)) }

            // 筛选模式
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_filter_mode),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.prefs.filterMode == FilterMode.MIXED,
                            onClick = { viewModel.setFilterMode(FilterMode.MIXED) },
                            label = { Text(stringResource(R.string.settings_filter_mixed)) },
                        )
                        FilterChip(
                            selected = state.prefs.filterMode == FilterMode.PHOTOS_ONLY,
                            onClick = { viewModel.setFilterMode(FilterMode.PHOTOS_ONLY) },
                            label = { Text(stringResource(R.string.settings_filter_photos)) },
                        )
                        FilterChip(
                            selected = state.prefs.filterMode == FilterMode.VIDEOS_ONLY,
                            onClick = { viewModel.setFilterMode(FilterMode.VIDEOS_ONLY) },
                            label = { Text(stringResource(R.string.settings_filter_videos)) },
                        )
                    }
                }
            }

            // 时间范围
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        stringResource(R.string.settings_time_range),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.ALL,
                            onClick = { viewModel.setTimeRange(TimeRange.ALL) },
                            label = { Text(stringResource(R.string.settings_time_all)) },
                        )
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.THREE_YEARS_AGO,
                            onClick = { viewModel.setTimeRange(TimeRange.THREE_YEARS_AGO) },
                            label = { Text(stringResource(R.string.settings_time_3y)) },
                        )
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.FIVE_YEARS_AGO,
                            onClick = { viewModel.setTimeRange(TimeRange.FIVE_YEARS_AGO) },
                            label = { Text(stringResource(R.string.settings_time_5y)) },
                        )
                    }
                }
            }

            // 相册选择
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_album)) },
                    supportingContent = { Text(state.prefs.albumFilter ?: stringResource(R.string.settings_album_all)) },
                    modifier = Modifier.clickable { showAlbumPicker = true },
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_privacy)) }

            // 应用锁
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_app_lock)) },
                    supportingContent = { Text(stringResource(R.string.settings_app_lock_desc)) },
                    trailingContent = {
                        Switch(
                            checked = state.prefs.appLockEnabled,
                            onCheckedChange = { viewModel.toggleAppLock(it) },
                        )
                    },
                )
            }

            item { HorizontalDivider() }
            item { SectionHeader(stringResource(R.string.settings_section_maintenance)) }

            // 清除缓存
            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.settings_clear_cache,
                                formatBytes(state.cacheSizeBytes),
                            ),
                        )
                    },
                    modifier = Modifier.clickable { viewModel.clearCache() },
                )
            }

            // 重置随机序列
            item {
                OutlinedButton(
                    onClick = { viewModel.reshuffle() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(R.string.settings_reshuffle))
                }
            }
        }
    }

    if (showAlbumPicker) {
        AlbumPickerSheet(
            albums = state.availableAlbums,
            currentSelection = state.prefs.albumFilter,
            onSelect = {
                viewModel.setAlbumFilter(it)
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
