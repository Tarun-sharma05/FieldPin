package com.tarun.fieldpin.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tarun.fieldpin.domain.repository.AuthRepository
import com.tarun.fieldpin.ui.login.LoginRoute
import com.tarun.fieldpin.ui.sites.SitesRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Sites : Screen("sites")
    object Map : Screen("map")
    object NewSite : Screen("new_site")
    object Inspection : Screen("inspection/{siteId}") {
        fun createRoute(siteId: String) = "inspection/$siteId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository = koinInject()
) {
    val currentUser by authRepository.observeCurrentUser().collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentUser) {
        val currentDestination = navController.currentDestination?.route
        if (currentUser != null && (currentDestination == null || currentDestination == Screen.Login.route)) {
            navController.navigate(Screen.Sites.route) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        } else if (currentUser == null && currentDestination != Screen.Login.route && currentDestination != null) {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginRoute(
                onLoginSuccess = {
                    navController.navigate(Screen.Sites.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Sites.route) {
            SitesRoute(
                onSiteClick = { site ->
                    navController.navigate(Screen.Inspection.createRoute(site.id))
                },
                onNewSiteClick = {
                    navController.navigate(Screen.NewSite.route)
                },
                onMapTabClick = {
                    navController.navigate(Screen.Map.route)
                },
                onLogoutClick = {
                    scope.launch {
                        authRepository.logout()
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Map Screen Placeholder (S5)",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        composable(Screen.NewSite.route) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "New Site Screen Placeholder (S6)",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        composable(Screen.Inspection.route) { backStackEntry ->
            val siteId = backStackEntry.arguments?.getString("siteId") ?: ""
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Inspection Screen Placeholder for Site: $siteId",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }
    }
}
