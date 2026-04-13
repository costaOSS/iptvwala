package com.iptvwala.presentation

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iptvwala.core.utils.DeviceUtils
import com.iptvwala.presentation.navigation.PlainAppScreen
import com.iptvwala.presentation.navigation.Screen
import com.iptvwala.presentation.ui.mobile.home.MobileHomeScreen
import com.iptvwala.presentation.ui.mobile.settings.MobileSettingsScreen
import com.iptvwala.presentation.ui.shared.theme.IPTVwalaTheme
import com.iptvwala.presentation.ui.tv.channel.TvChannelBrowserScreen
import com.iptvwala.presentation.ui.tv.epg.TvEpgScreen
import com.iptvwala.presentation.ui.tv.home.TvHomeScreen
import com.iptvwala.presentation.ui.tv.player.TvPlayerScreen
import com.iptvwala.presentation.viewmodel.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var deviceUtils: DeviceUtils
    
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val isTv = deviceUtils.isTv()
        val isTablet = deviceUtils.isTablet()
        
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isWideScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            
            IPTVwalaTheme(darkTheme = isSystemInDarkTheme()) {
                MainContent(
                    isTv = isTv || isWideScreen,
                    onEnterPip = { enterPipMode() }
                )
            }
        }
    }
    
    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPipMode()
        }
    }
}

@Composable
fun MainContent(
    isTv: Boolean,
    onEnterPip: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.state.collectAsState()
    
    Scaffold(
        bottomBar = {
            if (!isTv && currentRoute in listOf(
                    Screen.Home.route,
                    Screen.Channels.route,
                    Screen.Favorites.route,
                    Screen.Settings.route
                )
            ) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Home.route,
                        onClick = { navController.navigate(Screen.Home.route) },
                        icon = { Icon(painterResource(id = com.iptvwala.R.drawable.ic_home), "Home") },
                        label = { Text("Home") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Channels.route,
                        onClick = { navController.navigate(Screen.Channels.route) },
                        icon = { Icon(painterResource(id = com.iptvwala.R.drawable.ic_channels), "Channels") },
                        label = { Text("Channels") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Favorites.route,
                        onClick = { navController.navigate(Screen.Favorites.route) },
                        icon = { Icon(painterResource(id = com.iptvwala.R.drawable.ic_favorite), "Favorites") },
                        label = { Text("Favorites") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Settings.route,
                        onClick = { navController.navigate(Screen.Settings.route) },
                        icon = { Icon(painterResource(id = com.iptvwala.R.drawable.ic_settings), "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    if (isTv) {
                        TvHomeScreen(
                            onChannelClick = { channel ->
                                playerViewModel.playChannel(channel.id)
                                navController.navigate(Screen.Player.createRoute(channel.id))
                            },
                            onChannelLongClick = { },
                            onNavigateToChannels = { navController.navigate(Screen.Channels.route) },
                            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                            onNavigateToEpg = { navController.navigate(Screen.Epg.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onNavigateToPlainApp = { navController.navigate(Screen.PlainApp.route) }
                        )
                    } else {
                        MobileHomeScreen(
                            onChannelClick = { channel ->
                                playerViewModel.playChannel(channel.id)
                                navController.navigate(Screen.Player.createRoute(channel.id))
                            },
                            onNavigateToChannels = { navController.navigate(Screen.Channels.route) },
                            onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    }
                }
                
                composable(Screen.Channels.route) {
                    TvChannelBrowserScreen(
                        onChannelClick = { channel ->
                            playerViewModel.playChannel(channel.id)
                            navController.navigate(Screen.Player.createRoute(channel.id))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                composable(Screen.Favorites.route) {
                    TvChannelBrowserScreen(
                        onChannelClick = { channel ->
                            playerViewModel.playChannel(channel.id)
                            navController.navigate(Screen.Player.createRoute(channel.id))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                composable(Screen.Epg.route) {
                    TvEpgScreen(
                        onChannelClick = { channel ->
                            playerViewModel.playChannel(channel.id)
                            navController.navigate(Screen.Player.createRoute(channel.id))
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                
                composable(
                    route = Screen.Player.route,
                    arguments = listOf(navArgument("channelId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getLong("channelId") ?: 0L
                    
                    TvPlayerScreen(
                        channel = playerState.currentChannel,
                        currentProgram = playerState.currentProgram,
                        nextProgram = playerState.nextProgram,
                        isPlaying = playerState.isPlaying,
                        isBuffering = playerState.isBuffering,
                        position = playerState.position,
                        duration = playerState.duration,
                        playbackSpeed = playerState.playbackSpeed,
                        onBackClick = { 
                            playerViewModel.stop()
                            navController.popBackStack() 
                        },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onSeek = { playerViewModel.seekTo(it) },
                        onSpeedChange = { playerViewModel.setPlaybackSpeed(it) },
                        onPipClick = onEnterPip,
                        onPreviousChannel = { playerViewModel.playPreviousChannel() },
                        onNextChannel = { playerViewModel.playNextChannel() }
                    )
                }
                
                composable(Screen.Settings.route) {
                    MobileSettingsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPlainApp = { navController.navigate(Screen.PlainApp.route) }
                    )
                }
                
                composable(Screen.PlainApp.route) {
                    com.iptvwala.plainapp.panel.PlainAppPanelScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
