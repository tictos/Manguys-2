package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaType
import com.example.ui.MediaViewModel
import com.example.ui.theme.ThemePreferences

private data class MainNavigationItem(
    val index: Int,
    val title: String,
    val compactTitle: String,
    val icon: ImageVector,
    val contentDescription: String
)

@Composable
fun MainScreen(
    viewModel: MediaViewModel,
    themePreferences: ThemePreferences,
    onAddClick: () -> Unit,
    onEditClick: (Int) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategoryFilter by remember { mutableStateOf<MediaType?>(null) }

    val navItems = remember {
        listOf(
            MainNavigationItem(0, "Accueil", "Accueil", Icons.Filled.Home, "Accueil"),
            MainNavigationItem(1, "Actus", "Actus", Icons.Filled.Newspaper, "Actus"),
            MainNavigationItem(2, "Catégories", "Genres", Icons.Filled.Category, "Catégories"),
            MainNavigationItem(3, "Stats", "Stats", Icons.Filled.BarChart, "Statistiques"),
            MainNavigationItem(4, "Réglages", "Options", Icons.Filled.Settings, "Réglages")
        )
    }

    Scaffold(
        bottomBar = {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val screenWidth = maxWidth
                // Adaptive font size & text handling based on available screen width
                val labelFontSize = when {
                    screenWidth < 340.dp -> 9.sp
                    screenWidth < 380.dp -> 10.5.sp
                    else -> 12.sp
                }
                val useCompactTitle = screenWidth < 360.dp
                val alwaysShowLabel = screenWidth >= 300.dp

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    navItems.forEach { item ->
                        val isSelected = selectedTab == item.index
                        val displayTitle = if (useCompactTitle) item.compactTitle else item.title

                        NavigationBarItem(
                            selected = isSelected,
                            alwaysShowLabel = alwaysShowLabel,
                            onClick = {
                                selectedTab = item.index
                                if (item.index == 0) {
                                    selectedCategoryFilter = null
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.contentDescription
                                )
                            },
                            label = {
                                Text(
                                    text = displayTitle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = labelFontSize,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    initialCategory = selectedCategoryFilter,
                    onAddClick = onAddClick,
                    onEditClick = onEditClick
                )
                1 -> NewsScreen(
                    viewModel = viewModel,
                    onAddMediaClick = onAddClick
                )
                2 -> CategoriesScreen(
                    viewModel = viewModel,
                    onCategorySelect = { category ->
                        selectedCategoryFilter = category
                        selectedTab = 0
                    }
                )
                3 -> StatsScreen(
                    viewModel = viewModel
                )
                4 -> SettingsScreen(
                    themePreferences = themePreferences,
                    viewModel = viewModel
                )
            }
        }
    }
}

