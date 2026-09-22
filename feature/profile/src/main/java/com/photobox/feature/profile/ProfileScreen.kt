package com.photobox.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.photobox.core.data.repository.ProfileStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenLikes: () -> Unit,
    onOpenViewed: () -> Unit,
    onOpenOnThisDay: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading || state.stats == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        ProfileContent(
            stats = state.stats!!,
            onThisDayCount = state.onThisDayCount,
            onOpenFavorites = onOpenFavorites,
            onOpenLikes = onOpenLikes,
            onOpenViewed = onOpenViewed,
            onOpenOnThisDay = onOpenOnThisDay,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun ProfileContent(
    stats: ProfileStats,
    onThisDayCount: Int,
    onOpenFavorites: () -> Unit,
    onOpenLikes: () -> Unit,
    onOpenViewed: () -> Unit,
    onOpenOnThisDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                icon = Icons.Default.Image,
                label = stringResource(R.string.profile_stat_total),
                value = stats.totalMediaCount.toString(),
                onClick = null,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Default.Visibility,
                label = stringResource(R.string.profile_stat_viewed),
                value = stats.viewedCount.toString(),
                onClick = onOpenViewed,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                icon = Icons.Default.Favorite,
                label = stringResource(R.string.profile_stat_liked),
                value = stats.likeCount.toString(),
                onClick = onOpenLikes,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                icon = Icons.Default.Star,
                label = stringResource(R.string.profile_stat_favorites),
                value = stats.favoriteCount.toString(),
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f),
            )
        }
        // 那年今日入口：仅当往年今天有照片时才显示
        if (onThisDayCount > 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenOnThisDay() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = stringResource(R.string.profile_section_recall),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.profile_open_on_this_day),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val baseModifier = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = baseModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Text(text = value, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
