package com.iptvwala.presentation.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Channels : Screen("channels")
    data object Favorites : Screen("favorites")
    data object Epg : Screen("epg")
    data object Settings : Screen("settings")
    data object Player : Screen("player/{channelId}") {
        fun createRoute(channelId: Long) = "player/$channelId"
    }
    data object PlainApp : Screen("plainapp")
    data object Search : Screen("search")
}

sealed class PlainAppScreen(val route: String) {
    data object Remote : Screen("plainapp/remote")
    data object Clipboard : Screen("plainapp/clipboard")
    data object Sources : Screen("plainapp/sources")
    data object Browser : Screen("plainapp/browser")
    data object Files : Screen("plainapp/files")
    data object Apps : Screen("plainapp/apps")
    data object Notifications : Screen("plainapp/notifications")
    data object Device : Screen("plainapp/device")
    data object Volume : Screen("plainapp/volume")
}
