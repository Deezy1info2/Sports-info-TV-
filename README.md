# D-EXO PLAYER (Native Android IPTV Media Engine)

D-EXO Player is a lightweight, modern, and ultra-high-performance native Android IPTV Player application built strictly using native technologies. It is optimized for the low-latency play of high-definition streaming media protocols, including HTTP/HTTPS progressive files, Raw TS streams, and HLS (.m3u8) manifests over the internet.

Focusing completely on stream reliability and compliance, the application interacts directly with stream servers and does **not** rely on embedded WebViews, browser instances, portals, or server-side relays.

---

## 🎨 Visual Identity & Theme

D-EXO Player features a beautiful, cohesive **Slate-Cinema Dark Theme** styled on Material Design 3 guidelines:
*   **Deep Cinematic Contrast:** Utilizes a gradient of deep slates and midnight blue surfaces to maximize stream visibility and prevent eye strain.
*   **Spacious & Accessible Layouts:** Adheres to strict 8dp grid spacings with generous padding, safe notch inset exclusions, and safe drawing gesture overlays.
*   **Touch Targets & Usability:** Interactive controls conform to standard $48\text{dp}\times48\text{dp}$ touch profiles with immediate fluid visual ripple feedback.

---

## 🚀 Key Functional Modules

1.  **Cinema Stream Engine (`Media3 ExoPlayer`):**
    *   Dynamic media source factory automatically resolves progressive, HLS, or TS formats.
    *   Maintains custom request headers (`DefaultHttpDataSource`) per-channel or per-session.
    *   Features a custom Compose control layer including **Play/Pause overlaps, Seek Progress Sliders, Full-screen Mode, Aspect Scaling Toggles (Fit, Zoom, Stretch), Orientation Locks, and Playback Retries**.
    *   Full **Picture-in-Picture (PIP) Support** (Oreo API 26+) for seamless background multitasking.

2.  **Advanced Playback Options (Custom Request Headers):**
    *   Configure connection credentials on the fly, including: `User-Agent`, `Referer`, `Origin`, `Authorization Token`, and `Cookie`.
    *   Create, view, edit, and remove custom user-defined HTTP header keys.
    *   Restores the exact header set used for a stream when loaded from recents.

3.  **Local Playlist Sync & Caching (`Room SQLite Database` + `M3U Parser`):**
    *   Fast, local parser extracts Channel names, Logo URLs, and Group tags from remote HTTP links or clipboard pasted strings.
    *   Stores thousands of channels in a local SQLite Room DB instance for instantaneous offline search and group indexing.

4.  **Persistent History Storage (`Preferences DataStore`):**
    *   Maintains a reactive list of the last 10 successful streams (including custom request headers) and the last selected channel for immediate one-tap play.

5.  **Telemetry Console (`Developer Debug Panel`):**
    *   Full visual diagnostics console displaying ExoPlayer state transitions, connection errors, track formats, resolutions, bitrates, and hardware specifications.

---

## 🛠️ Technology Stack

*   **Language:** Kotlin  
*   **UI Framework:** Jetpack Compose (Material 3)  
*   **Media Core:** Google Android Media3 ExoPlayer & HLS Extensions (v1.5.0)  
*   **Database:** SQLite Room Database (v2.7.0)  
*   **State Engines:** Coroutines, StateFlow, Preferences DataStore  
*   **Network Client:** OkHttp 4 & Retrofit 2  
*   **Image Renderer:** Coil Compose  

---

## 📂 Project Structure

```text
/app/src/main/
├── AndroidManifest.xml (Internet permissions, Cleartext traffic, PIP locks)
├── java/com/example/
│   ├── MainActivity.kt (Entrypoint, Edge-to-Edge windowing binds)
│   ├── data/
│   │   ├── RecentStream.kt (Moshi-compatible recent streams entity)
│   │   ├── RecentStreamStore.kt (DataStore preferences repositories)
│   │   └── database/
│   │       ├── ChannelEntity.kt (SQLite Schema for list indexing)
│   │       ├── ChannelDao.kt (SQLite reactive queries)
│   │       └── IptvDatabase.kt (Thread-safe database instance)
│   │   └── parser/
│   │       └── M3uParser.kt (M3U lines parser)
│   └── ui/
│       ├── MainViewModel.kt (MVVM State, Importer triggers, Developer Logger)
│       ├── IptvDashboard.kt (Core Slate Slate-Cinema page framework)
│       └── components/
│           ├── VideoPlayer.kt (Media3 controller, Track quality loader)
│           ├── AdvancedHeadersSheet.kt (Header editor UI)
│           ├── PlaylistImportSection.kt (Remote & Pasted list downloader)
│           └── DeveloperDebugPanel.kt (Technical telemetry feed)
```

---

## 🎬 How to Use

1.  **Single Stream Playback:**
    *   Open **Live Console** (Tab 1), paste an `.m3u8` or stream URL.
    *   Optionally configure User-Agent, Referrer, Cookie parameters under **Advanced Playback Options**.
    *   Tap **PLAY STREAM**.
2.  **Importing an IPTV Playlist (M3U):**
    *   Open **Import Engine** (Tab 3).
    *   Paste a remote playlist HTTP link (e.g., `https://provider.com/playlist.m3u`) **OR** select **Paste M3U Content** and paste raw playlist text blocks.
    *   Tap **Download and Cache Playlists**.
3.  **Browsing Channels:**
    *   Open **Channel Playlist** (Tab 2).
    *   Type words in the search bar, or slide the group badges row to filter channels instantly (e.g. "News", "Sports").
    *   Tap any channel to begin smooth play.
4.  **Debugging streams:**
    *   Expand the **Developer Console & Technical Logs** at the bottom of the Live Console tab to read real-time stream status updates or detailed error codes (e.g., HTTP 403 Forbidden).
