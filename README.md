# IPTVwala

A production-ready Android IPTV player app for Android TV (leanback / TV Compose, D-pad only) and Android Mobile (phone + tablet, touch). Lightweight APK, modern Compose UI, no Firebase, no analytics, no ads.

## Features

### IPTV Core
- **Source Management**: M3U URL, local M3U/M3U8 file picker, Xtream Codes API support
- **Channel Browser**: Group channels by category, instant debounced search, favorites, recently watched
- **Video Player**: Media3 ExoPlayer with HLS, DASH, SmoothStreaming, progressive HTTP support
- **EPG**: XMLTV parsing with auto-refresh, timeline grid view (TV), bottom sheet (Mobile)

### PlainApp Integration
A complete in-app control panel accessible via D-pad on Android TV:

| Screen | Features |
|--------|----------|
| **Remote Control** | Virtual D-pad, shortcut buttons (HOME, BACK, MENU, etc.) |
| **Clipboard** | Clipboard history, paste from browser via WebSocket sync |
| **Source Manager** | Add/Edit/Delete/Refresh M3U sources |
| **Channel Browser** | Quick channel picker |
| **File Manager** | Browse downloads, play/delete files |
| **App Launcher** | Grid of installed apps |
| **Notifications** | View recent Android notifications |
| **Device Info** | IP, version, QR code for web access |
| **Volume & Display** | Media volume, brightness, wake screen |

### Web UI
Built-in HTTP server (NanoHTTPD) on port 8080 with:
- REST API for all operations
- WebSocket for real-time updates
- Browser-based channel browser and remote control
- QR code for quick access from desktop

## Architecture

```
com.iptvwala
├── core          → DI, base classes, utils, network
├── data          → Room, Retrofit, parsers, repos
├── domain        → models, repo interfaces
├── presentation  → viewmodels, TV/mobile screens, shared UI
├── server        → NanoHTTPD server, REST API, WebSocket
└── plainapp      → Panel screens, viewmodels, services
```

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose + AndroidX TV Compose
- **Min SDK**: 21 | **Target SDK**: 35
- **DI**: Hilt
- **Async**: Coroutines + StateFlow
- **Media**: Media3 ExoPlayer
- **Database**: Room with WAL mode
- **Network**: Retrofit + OkHttp
- **Image Loading**: Coil
- **Server**: NanoHTTPD (~150KB)
- **EPG**: XMLTV parsing

## Building

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Clean
./gradlew clean
```

## Permissions

- `INTERNET` - Network access
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_VIDEO` - Local file access
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` - Background playback
- `WAKE_LOCK` - Keep device awake during playback
- `RECEIVE_BOOT_COMPLETED` - Auto-start server on boot
- `BIND_NOTIFICATION_LISTENER_SERVICE` - Notification access
- `BIND_ACCESSIBILITY_SERVICE` - D-pad key injection

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `DPAD` | Navigate |
| `OK/ENTER` | Select/Play |
| `BACK` | Go back |
| `HOME` | Return to home |
| `MENU` | Open PlainApp panel |
| `SEARCH` | Open search |

## Web API Endpoints

```
GET  /api/status              → Playback state
GET  /api/channels            → Channel list
GET  /api/channels/search?q=  → Search channels
POST /api/play                → Play channel by ID
POST /api/play/url            → Play arbitrary URL
GET  /api/sources            → Source list
POST /api/sources/add         → Add source
POST /api/sources/:id/refresh → Force refresh
GET  /api/favorites           → Favorite channels
POST /api/favorites/toggle     → Toggle favorite
GET  /api/epg/:channelId      → EPG for channel
GET  /api/clipboard           → Clipboard content
POST /api/clipboard           → Set clipboard
GET  /api/device             → Device info
GET  /api/apps               → Installed apps
POST /api/apps/launch         → Launch app
POST /api/remote/key          → Inject D-pad key
POST /api/volume              → Set volume
WS   /ws                     → Real-time events
```

## License

MIT License
