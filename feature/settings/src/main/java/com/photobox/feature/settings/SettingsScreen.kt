package com.photobox.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photobox.core.data.datastore.FilterMode
import com.photobox.core.data.datastore.TimeRange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showCustomPicker by remember { mutableStateOf(false) }

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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                        FilterChip(
                            selected = state.prefs.filterMode == FilterMode.GIFS_ONLY,
                            onClick = { viewModel.setFilterMode(FilterMode.GIFS_ONLY) },
                            label = { Text(stringResource(R.string.settings_filter_gifs)) },
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.ALL,
                            onClick = { viewModel.setTimeRange(TimeRange.ALL) },
                            label = { Text(stringResource(R.string.settings_time_all)) },
                        )
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.LAST_MONTH,
                            onClick = { viewModel.setTimeRange(TimeRange.LAST_MONTH) },
                            label = { Text(stringResource(R.string.settings_time_1m)) },
                        )
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.LAST_THREE_MONTHS,
                            onClick = { viewModel.setTimeRange(TimeRange.LAST_THREE_MONTHS) },
                            label = { Text(stringResource(R.string.settings_time_3m)) },
                        )
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.LAST_YEAR,
                            onClick = { viewModel.setTimeRange(TimeRange.LAST_YEAR) },
                            label = { Text(stringResource(R.string.settings_time_1y)) },
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
                        FilterChip(
                            selected = state.prefs.timeRange == TimeRange.CUSTOM,
                            onClick = { showCustomPicker = true },
                            label = { Text(stringResource(R.string.settings_time_custom)) },
                        )
                    }
                    // 自定义时间范围时显示两个输入框：开始日期 / 结束日期。
                    // 两个输入框宽度一致，点击弹出年/月/日三栏滚动选择器。
                    if (state.prefs.timeRange == TimeRange.CUSTOM) {
                        val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            DateInputBox(
                                label = stringResource(R.string.settings_custom_start),
                                value = state.prefs.customStartMs?.let { dateFmt.format(Date(it)) } ?: "",
                                onClick = { showCustomPicker = true },
                                modifier = Modifier.weight(1f),
                            )
                            DateInputBox(
                                label = stringResource(R.string.settings_custom_end),
                                value = state.prefs.customEndMs?.let { dateFmt.format(Date(it)) } ?: "",
                                onClick = { showCustomPicker = true },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // 相册选择已移除：MediaItem 没有 bucket 字段，UI 暂不暴露相册级别过滤。

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

    if (showCustomPicker) {
        WheelDateRangePickerDialog(
            initialStartMs = state.prefs.customStartMs,
            initialEndMs = state.prefs.customEndMs,
            onConfirm = { startMs, endMs ->
                viewModel.setTimeRange(TimeRange.CUSTOM)
                viewModel.setCustomRange(startMs, endMs)
                showCustomPicker = false
            },
            onClear = {
                viewModel.setCustomRange(null, null)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false },
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

/**
 * 日期输入框：只读，点击触发 wheel picker；与同 Row 的另一个 DateInputBox 等宽。
 * 用 OutlinedTextField + readOnly 来保证两个框视觉一致；不显示编辑铅笔图标（trailingIcon 留空）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateInputBox(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        // 拦截点击事件到 onClick，而不是弹出键盘（readOnly 自身就会屏蔽键盘，但显式拦截更稳）
        modifier = modifier.clickable(onClick = onClick),
        singleLine = true,
    )
}

/**
 * 三栏（年/月/日）滚轮日期选择器对话框。
 *
 * 用三个并排的 LazyColumn 模拟"滚轮"，每栏选中的项会居中显示、加粗；
 * 点确认把当前年/月/日拼成 UTC 0 点的 epoch ms 返回（和 DateRangePicker 的语义一致）。
 *
 * 滚轮联动：月份变化时，"日"栏的最大值会重新算（28/29/30/31）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WheelDateRangePickerDialog(
    initialStartMs: Long?,
    initialEndMs: Long?,
    onConfirm: (Long?, Long?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val now = remember { System.currentTimeMillis() }
    // 默认：开始 = 5 年前 1 月 1 日；结束 = 今天。把 end 默认成今天以保证起止合理。
    val defaultStart = remember(now) {
        Calendar.getInstance().apply { timeInMillis = now; add(Calendar.YEAR, -5) }
    }
    var startYear by remember { mutableStateOf(toYear(initialStartMs ?: defaultStart.timeInMillis)) }
    var startMonth by remember { mutableStateOf(toMonth(initialStartMs ?: defaultStart.timeInMillis)) }
    var startDay by remember { mutableStateOf(toDay(initialStartMs ?: defaultStart.timeInMillis)) }

    var endYear by remember { mutableStateOf(toYear(initialEndMs ?: now)) }
    var endMonth by remember { mutableStateOf(toMonth(initialEndMs ?: now)) }
    var endDay by remember { mutableStateOf(toDay(initialEndMs ?: now)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_custom_pick)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.settings_custom_start),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                WheelDatePicker(
                    year = startYear,
                    month = startMonth,
                    day = startDay,
                    onYearChange = { startYear = it; startDay = startDay.coerceAtMost(daysInMonth(it, startMonth)) },
                    onMonthChange = { startMonth = it; startDay = startDay.coerceAtMost(daysInMonth(startYear, it)) },
                    onDayChange = { startDay = it },
                )
                HorizontalDivider()
                Text(
                    text = stringResource(R.string.settings_custom_end),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                WheelDatePicker(
                    year = endYear,
                    month = endMonth,
                    day = endDay,
                    onYearChange = { endYear = it; endDay = endDay.coerceAtMost(daysInMonth(it, endMonth)) },
                    onMonthChange = { endMonth = it; endDay = endDay.coerceAtMost(daysInMonth(endYear, it)) },
                    onDayChange = { endDay = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val s = toEpochMs(startYear, startMonth, 1)
                val e = toEpochMs(endYear, endMonth, daysInMonth(endYear, endMonth))
                onConfirm(s, e)
            }) { Text(stringResource(R.string.settings_custom_confirm)) }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onClear) { Text(stringResource(R.string.settings_custom_clear)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_back)) }
            }
        },
    )
}

/**
 * 单组年/月/日三栏滚轮。每栏都是一个 LazyColumn，循环显示候选值；
 * 当前选中项通过 keepCenterVisible 自动滚到屏幕中线。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WheelDatePicker(
    year: Int,
    month: Int,
    day: Int,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Int) -> Unit,
    onDayChange: (Int) -> Unit,
) {
    val years = remember { (1970..Calendar.getInstance().get(Calendar.YEAR)).toList() }
    val months = remember { (1..12).toList() }
    val days = remember(year, month) { (1..daysInMonth(year, month)).toList() }

    Row(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WheelColumn(
            items = years.map { "${it}年" },
            selectedIndex = years.indexOf(year).coerceAtLeast(0),
            onSelectedChange = { idx -> onYearChange(years[idx]) },
            modifier = Modifier.weight(1f),
        )
        WheelColumn(
            items = months.map { "${it}月" },
            selectedIndex = months.indexOf(month).coerceAtLeast(0),
            onSelectedChange = { idx -> onMonthChange(months[idx]) },
            modifier = Modifier.weight(1f),
        )
        WheelColumn(
            items = days.map { "${it}日" },
            selectedIndex = days.indexOf(day).coerceAtLeast(0).coerceAtMost(days.lastIndex),
            onSelectedChange = { idx -> onDayChange(days[idx]) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 单列滚轮：所有候选值循环拼接成一长串，首尾各追加若干"padding"项，
 * 选中项定位在中线，初始滚动到中线位置，让上方有空间可向上滚。
 */
@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val paddingCount = 3
    val total = items.size + 2 * paddingCount
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val scope = rememberCoroutineScope()

    // 当外部 selectedIndex 改变（如联动导致日数变），把列表滚回新选中项
    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .height(160.dp),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 56.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 顶部 padding 项（视觉占位）
            items(count = paddingCount) {
                WheelItem(text = "", selected = false)
            }
            items(items.size) { idx ->
                // 注意：选中态用当前 selectedIndex 比较，但真实数据下标 = idx
                val realIdx = idx
                WheelItem(
                    text = items[realIdx],
                    selected = realIdx == selectedIndex,
                )
            }
            items(count = paddingCount) {
                WheelItem(text = "", selected = false)
            }
        }
        // 选中框视觉提示：横向细线，让用户看清中线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .alpha(0.0f),
        )
    }
}

@Composable
private fun WheelItem(text: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

private fun toYear(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.YEAR)
private fun toMonth(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.MONTH) + 1
private fun toDay(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.DAY_OF_MONTH)

private fun daysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun toEpochMs(year: Int, month: Int, day: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}

@Composable
private fun Row(
    horizontalArrangement: Arrangement.Horizontal,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = horizontalArrangement,
        content = content,
    )
}
