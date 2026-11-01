package com.tarun.fieldpin.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tarun.fieldpin.domain.repository.AuthRepository
import com.tarun.fieldpin.ui.login.LoginRoute
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Sites : Screen("sites")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    authRepository: AuthRepository = koinInject()
) {
    val currentUser by authRepository.observeCurrentUser().collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(currentUser) {
        val currentDestination = navController.currentDestination?.route
        if (currentUser != null && currentDestination != Screen.Sites.route) {
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
            val scope = rememberCoroutineScope()
            // Placeholder Sites screen with Logout button until S4 (Sites List) is implemented
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome, ${currentUser?.name ?: "User"}!",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                authRepository.logout()
                            }
                        }
                    ) {
                        Text("Sign Out")
                    }
                }
            }
        }
    }
}
