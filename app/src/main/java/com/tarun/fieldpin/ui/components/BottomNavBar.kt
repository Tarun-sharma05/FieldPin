package com.tarun.fieldpin.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class BottomNavTab {
    LIST,
    MAP
}

@Composable
fun BottomNavBar(
    selectedTab: BottomNavTab,
    onTabSelected: (BottomNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.LIST,
            onClick = { onTabSelected(BottomNavTab.LIST) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List") },
            label = { Text("List") }
        )
        NavigationBarItem(
            selected = selectedTab == BottomNavTab.MAP,
            onClick = { onTabSelected(BottomNavTab.MAP) },
            icon = { Icon(Icons.Default.Map, contentDescription = "Map") },
            label = { Text("Map") }
        )
    }
}
