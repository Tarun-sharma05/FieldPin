package com.tarun.fieldpin.ui.sites

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tarun.fieldpin.domain.model.Site
import com.tarun.fieldpin.domain.model.SiteStatus
import com.tarun.fieldpin.ui.components.BottomNavBar
import com.tarun.fieldpin.ui.components.BottomNavTab
import com.tarun.fieldpin.ui.components.FilterChipGroup
import com.tarun.fieldpin.ui.components.SiteCard
import org.koin.androidx.compose.koinViewModel

@Composable
fun SitesRoute(
    onSiteClick: (Site) -> Unit,
    onNewSiteClick: () -> Unit,
    onMapTabClick: () -> Unit,
    onLogoutClick: () -> Unit,
    viewModel: SitesViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SitesScreen(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onStatusSelected = viewModel::onStatusSelected,
        onSiteClick = onSiteClick,
        onNewSiteClick = onNewSiteClick,
        onMapTabClick = onMapTabClick,
        onLogoutClick = onLogoutClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    uiState: SitesUiState,
    onQueryChanged: (String) -> Unit,
    onStatusSelected: (SiteStatus?) -> Unit,
    onSiteClick: (Site) -> Unit,
    onNewSiteClick: () -> Unit,
    onMapTabClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("FieldPin Sites") },
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomNavBar(
                selectedTab = BottomNavTab.LIST,
                onTabSelected = { tab ->
                    if (tab == BottomNavTab.MAP) {
                        onMapTabClick()
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewSiteClick) {
                Icon(Icons.Default.Add, contentDescription = "Add New Site")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search sites by name...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (uiState.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true
            )

            FilterChipGroup(
                selectedStatus = uiState.selectedStatus,
                onStatusSelected = onStatusSelected
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.sites.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No sites found",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (uiState.query.isNotEmpty() || uiState.selectedStatus != null) {
                                    "Try adjusting your search or status filter."
                                } else {
                                    "Tap '+' to add your first site."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = uiState.sites,
                                key = { site -> site.id }
                            ) { site ->
                                SiteCard(
                                    site = site,
                                    onSiteClick = onSiteClick
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
