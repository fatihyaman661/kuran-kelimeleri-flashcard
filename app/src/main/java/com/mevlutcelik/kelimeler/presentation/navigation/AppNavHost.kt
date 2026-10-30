package com.mevlutcelik.kelimeler.presentation.navigation

import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mevlutcelik.kelimeler.R
import com.mevlutcelik.kelimeler.presentation.common.HeroBackground
import com.mevlutcelik.kelimeler.presentation.home.HomeScreen
import com.mevlutcelik.kelimeler.presentation.settings.SettingsScreen
import com.mevlutcelik.kelimeler.presentation.study.StudyScreen
import com.mevlutcelik.kelimeler.presentation.theme.OnSurface
import com.mevlutcelik.kelimeler.presentation.theme.OnSurfaceMuted
import com.mevlutcelik.kelimeler.presentation.theme.PrimaryMint
import com.mevlutcelik.kelimeler.presentation.theme.SurfaceDim
import com.mevlutcelik.kelimeler.presentation.words.WordDetailScreen
import com.mevlutcelik.kelimeler.presentation.words.WordListScreen
import kotlinx.coroutines.launch

object Routes {
    const val TODAY = "today"
    const val WORDS = "words"
    const val SETTINGS = "settings"
    const val STUDY = "study"
    const val WORD_ARG = "key"
    const val WORD = "word/{$WORD_ARG}"

    // Anki GUID'leri '/', '%', '?' gibi karakterler içerebilir; rotada URL-güvenli Base64 taşınır.
    private const val FLAGS = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

    fun word(id: String) = "word/" + Base64.encodeToString(id.toByteArray(), FLAGS)
    fun decodeWordId(key: String) = String(Base64.decode(key, FLAGS))
}

private enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    Today(Routes.TODAY, R.string.nav_today, Icons.Filled.Today),
    Words(Routes.WORDS, R.string.nav_words, Icons.AutoMirrored.Filled.MenuBook),
    Settings(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
}

/** [KelimelerApp] içinde sağlanır; sekme başlıklarındaki hamburger butonu çekmeceyi bununla açar. */
val LocalOpenNavigationDrawer = compositionLocalOf<() -> Unit> { {} }

@Composable
fun NavigationDrawerMenuButton(modifier: Modifier = Modifier) {
    val open = LocalOpenNavigationDrawer.current
    IconButton(modifier = modifier, onClick = open) {
        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_open), tint = LocalContentColor.current)
    }
}

@Composable
fun KelimelerApp(navController: NavHostController = rememberNavController()) {
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalOpenNavigationDrawer provides { scope.launch { drawerState.open() } }) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            // Çalışırken kenardan kaydırma yanlışlıkla menüyü açmasın.
            gesturesEnabled = drawerState.isOpen || Destination.entries.any { it.route == route },
            drawerContent = {
                DrawerContent(route) { destination ->
                    scope.launch { drawerState.close() }
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
        ) {
            Box(Modifier.fillMaxSize()) {
                NavHost(navController, startDestination = Routes.TODAY, modifier = Modifier.fillMaxSize()) {
                    composable(Routes.TODAY) { HomeScreen(onStudy = { navController.navigate(Routes.STUDY) }) }
                    composable(Routes.WORDS) { WordListScreen(onOpenWord = { navController.navigate(Routes.word(it)) }) }
                    composable(Routes.SETTINGS) { SettingsScreen() }
                    composable(Routes.STUDY) { StudyScreen(onBack = { navController.popBackStack() }) }
                    composable(Routes.WORD, arguments = listOf(navArgument(Routes.WORD_ARG) { type = NavType.StringType })) {
                        WordDetailScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
            // Menü açıkken geri tuşu önce menüyü kapatsın.
            BackHandler(enabled = drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
        }
    }
}

@Composable
private fun DrawerContent(route: String?, onSelect: (Destination) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = SurfaceDim, modifier = Modifier.fillMaxHeight()) {
        Column(Modifier.fillMaxHeight()) {
            HeroBackground(RoundedCornerShape(24.dp), Modifier.padding(12.dp).fillMaxWidth(), skylineBias = 1f) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorResource(R.color.ic_launcher_background)),
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, color = OnSurface)
                        Text(stringResource(R.string.nav_menu_subtitle), style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Destination.entries.forEach { destination ->
                val selected = route == destination.route || (destination == Destination.Words && route == Routes.WORD)
                NavigationDrawerItem(
                    icon = { Icon(destination.icon, contentDescription = null, tint = if (selected) PrimaryMint else OnSurfaceMuted) },
                    label = { Text(stringResource(destination.label), color = if (selected) PrimaryMint else OnSurface) },
                    selected = selected,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = PrimaryMint.copy(alpha = 0.12f),
                        unselectedContainerColor = SurfaceDim,
                    ),
                )
            }
        }
    }
}
