package com.photobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.feature.day.DayAlbumPhotoViewer
import com.photobox.feature.day.DayAlbumRoute
import com.photobox.feature.onboarding.OnboardingRoute
import com.photobox.feature.profile.FavoritesListRoute
import com.photobox.feature.profile.LikesListRoute
import com.photobox.feature.profile.OnThisDayPhotoViewer
import com.photobox.feature.profile.OnThisDayScreen
import com.photobox.feature.profile.PhotoListSource
import com.photobox.feature.profile.PhotoViewerScreen
import com.photobox.feature.profile.ProfileRoute
import com.photobox.feature.profile.ViewedListRoute
import com.photobox.feature.settings.SettingsRoute
import com.photobox.feature.stream.StreamRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val ONBOARDING = "onboarding"
    const val STREAM = "stream"
    const val PROFILE = "profile"
    const val FAVORITES_LIST = "profile/favorites"
    const val LIKES_LIST = "profile/likes"
    const val VIEWED_LIST = "profile/viewed"
    const val ON_THIS_DAY = "profile/on_this_day"
    const val ON_THIS_DAY_VIEWER = "profile/on_this_day/viewer/{mediaId}"
    const val SETTINGS = "settings"
    const val DAY_ALBUM = "day_album/{dateMs}"
    const val DAY_ALBUM_VIEWER = "day_album/{dateMs}/viewer/{mediaId}"
    const val PHOTO_VIEWER = "photo_viewer/{source}/{mediaId}"

    fun dayAlbumRoute(dateMs: Long) = "day_album/$dateMs"
    fun dayAlbumViewerRoute(dateMs: Long, mediaId: Long) = "day_album/$dateMs/viewer/$mediaId"
    fun onThisDayViewerRoute(mediaId: Long) = "profile/on_this_day/viewer/$mediaId"
    fun photoViewerRoute(source: String, mediaId: Long) = "photo_viewer/$source/$mediaId"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    private val prefs: UserPreferencesDataSource,
) : ViewModel() {
    /**
     * 真正的"应用入口"标志：null = DataStore 还没发射第一次（启动那一帧）；
     * true/false = 用户是否已经完成 onboarding。
     *
     * 之前用 Eagerly + 初始值 ONBOARDING 的方案有个隐蔽 bug：
     * 如果 RootViewModel 是 ONBOARDING 启动，但 DataStore 里其实 onboardingDone=true
     * （OEM 清理了缓存、用户在第三页前退出、卸载重装后 DataStore 残留……），
     * NavHost 已经构建在 ONBOARDING 上，用户看到欢迎页就误以为"二次又显示了"。
     *
     * 现在用 nullable + 短窗口白屏 + 后续读取结果跳转：
     * - 第一次启动（onboardingDone=false）：nav 到 ONBOARDING
     * - 第 N 次启动（onboardingDone=true）：nav 到 STREAM
     */
    val startDestination: StateFlow<Boolean?> = prefs.userPreferences
        .map { it.onboardingDone }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )

    /** 兜底跳转 —— 用户在 ONBOARDING 上但 DataStore 显示已完成时立即跳走。 */
    private val _shouldRedirectToStream = MutableStateFlow(false)
    val shouldRedirectToStream: StateFlow<Boolean> = _shouldRedirectToStream.asStateFlow()

    init {
        // 监听 onboardingDone 变化：如果用户不知为何回到 ONBOARDING 流程，但
        // DataStore 已显示完成 → 立即标红，让 NavHost 跳转 STREAM。
        prefs.userPreferences
            .onEach { userPrefs ->
                if (userPrefs.onboardingDone) {
                    _shouldRedirectToStream.value = true
                }
            }
            .launchIn(viewModelScope)
    }
}

@Composable
fun PhotoBoxNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val startDest by rootViewModel.startDestination.collectAsState()
    val shouldRedirect by rootViewModel.shouldRedirectToStream.collectAsState()

    // 兜底跳转：用户在 ONBOARDING 上但 DataStore 显示已完成 → 立即跳走。
    LaunchedEffect(shouldRedirect) {
        if (shouldRedirect && navController.currentDestination?.route == Routes.ONBOARDING) {
            navController.navigate(Routes.STREAM) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    // DataStore 还没发射第一次（极短窗口）：显示空白背景避免黑屏闪烁；
    // 发射后根据 onboardingDone 决定入口。
    val resolvedStart = when (startDest) {
        null -> Routes.ONBOARDING // DataStore 还没发射，第一次显示空白期用 ONBOARDING 占位
        true -> Routes.STREAM
        false -> Routes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = resolvedStart,
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingRoute(
                onCompleted = {
                    navController.navigate(Routes.STREAM) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.STREAM) {
            StreamRoute(
                onSwipeToDayAlbum = { dateMs ->
                    navController.navigate(Routes.dayAlbumRoute(dateMs))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onOpenOnThisDay = { navController.navigate(Routes.ON_THIS_DAY) },
            )
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES_LIST) },
                onOpenLikes = { navController.navigate(Routes.LIKES_LIST) },
                onOpenViewed = { navController.navigate(Routes.VIEWED_LIST) },
                onOpenOnThisDay = { navController.navigate(Routes.ON_THIS_DAY) },
            )
        }
        composable(Routes.FAVORITES_LIST) {
            FavoritesListRoute(
                onBack = { navController.popBackStack() },
                onPhotoClick = { mediaId ->
                    navController.navigate(Routes.photoViewerRoute("favorites", mediaId))
                },
            )
        }
        composable(Routes.LIKES_LIST) {
            LikesListRoute(
                onBack = { navController.popBackStack() },
                onPhotoClick = { mediaId ->
                    navController.navigate(Routes.photoViewerRoute("likes", mediaId))
                },
            )
        }
        composable(Routes.VIEWED_LIST) {
            ViewedListRoute(
                onBack = { navController.popBackStack() },
                onPhotoClick = { mediaId ->
                    navController.navigate(Routes.photoViewerRoute("viewed", mediaId))
                },
            )
        }
        composable(Routes.ON_THIS_DAY) {
            OnThisDayScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { mediaId ->
                    navController.navigate(Routes.onThisDayViewerRoute(mediaId))
                },
            )
        }
        composable(
            route = Routes.ON_THIS_DAY_VIEWER,
            arguments = listOf(navArgument("mediaId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            OnThisDayPhotoViewer(
                initialMediaId = mediaId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PHOTO_VIEWER,
            arguments = listOf(
                navArgument("source") { type = NavType.StringType },
                navArgument("mediaId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val sourceStr = backStackEntry.arguments?.getString("source") ?: "viewed"
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            val source = runCatching { PhotoListSource.valueOf(sourceStr.uppercase()) }
                .getOrDefault(PhotoListSource.VIEWED)
            PhotoViewerScreen(
                source = source,
                initialMediaId = mediaId,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DAY_ALBUM,
            arguments = listOf(navArgument("dateMs") { type = NavType.LongType }),
        ) { backStackEntry ->
            val dateMs = backStackEntry.arguments?.getLong("dateMs") ?: System.currentTimeMillis()
            DayAlbumRoute(
                dateMs = dateMs,
                onBack = { navController.popBackStack() },
                onPhotoClick = { mediaId ->
                    navController.navigate(Routes.dayAlbumViewerRoute(dateMs, mediaId))
                },
            )
        }
        composable(
            route = Routes.DAY_ALBUM_VIEWER,
            arguments = listOf(
                navArgument("dateMs") { type = NavType.LongType },
                navArgument("mediaId") { type = NavType.LongType },
            ),
        ) { backStackEntry ->
            val dateMs = backStackEntry.arguments?.getLong("dateMs") ?: System.currentTimeMillis()
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: 0L
            DayAlbumPhotoViewer(
                dateMs = dateMs,
                initialMediaId = mediaId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}