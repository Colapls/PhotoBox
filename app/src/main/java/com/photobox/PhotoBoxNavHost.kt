package com.photobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.photobox.core.data.datastore.UserPreferencesDataSource
import com.photobox.feature.onboarding.OnboardingRoute
import com.photobox.feature.profile.FavoritesListRoute
import com.photobox.feature.profile.LikesListRoute
import com.photobox.feature.profile.ProfileRoute
import com.photobox.feature.stream.StreamRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val ONBOARDING = "onboarding"
    const val STREAM = "stream"
    const val PROFILE = "profile"
    const val FAVORITES_LIST = "profile/favorites"
    const val LIKES_LIST = "profile/likes"
}

@HiltViewModel
class RootViewModel @Inject constructor(
    prefs: UserPreferencesDataSource,
) : ViewModel() {
    val startDestination: StateFlow<String> = prefs.userPreferences
        .map { if (it.onboardingDone) Routes.STREAM else Routes.ONBOARDING }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            Routes.ONBOARDING,
        )
}

@Composable
fun PhotoBoxNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: RootViewModel = hiltViewModel(),
) {
    val startDest by rootViewModel.startDestination.collectAsState()

    NavHost(
        navController = navController,
        startDestination = startDest,
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
            StreamRoute()
        }
        composable(Routes.PROFILE) {
            ProfileRoute(
                onBack = { navController.popBackStack() },
                onOpenFavorites = { navController.navigate(Routes.FAVORITES_LIST) },
                onOpenLikes = { navController.navigate(Routes.LIKES_LIST) },
            )
        }
        composable(Routes.FAVORITES_LIST) {
            FavoritesListRoute(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIKES_LIST) {
            LikesListRoute(onBack = { navController.popBackStack() })
        }
    }
}
