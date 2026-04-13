# HasiTV — Android TV IPTV Player

**Platform:** Android TV / Fire TV Stick / Google TV  
**Min SDK:** 21 (Android 5.0)  
**Language:** Kotlin + Jetpack Compose  
**Player:** ExoPlayer (Media3)  
**Developer:** Hasi Elektronic

---

## Özellikler

- 📺 Canlı IPTV (HLS, MPEG-TS, RTSP, RTMP)
- 📋 M3U URL + Xtream Codes API
- 📡 EPG via XMLTV (12 saatlik önbellek)
- ⭐ Favoriler & kanal grupları
- 🕐 Son izlenenler (20 kanal)
- 🌙 Karanlık / Aydınlık tema
- 🔄 Otomatik yeniden bağlanma (3 deneme)
- 🎮 Android TV uzaktan kumanda desteği

---

## Android Studio ile Build

### Gereksinimler
- Android Studio Hedgehog veya üstü
- JDK 17+
- Android SDK API 35

### Adımlar

```bash
# 1. Projeyi aç
Android Studio → File → Open → HasiTV-Android klasörü

# 2. Sync
Gradle sync otomatik başlar (ilk açılışta birkaç dakika)

# 3. Çalıştır
Run → Seç: Android TV Emulator veya bağlı cihaz
```

### APK Build (test için)
```bash
./gradlew assembleDebug
# Çıktı: app/build/outputs/apk/debug/app-debug.apk
```

### AAB Build (Google Play için)
```bash
./gradlew bundleRelease
# Çıktı: app/build/outputs/bundle/release/app-release.aab
```

---

## Proje Yapısı

```
HasiTV-Android/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/de/hasi/hasitv/
│   │   ├── MainActivity.kt
│   │   ├── data/
│   │   │   ├── model/Models.kt          # Channel, Playlist, EpgProgram
│   │   │   ├── parser/
│   │   │   │   ├── M3UParser.kt
│   │   │   │   ├── XmltvParser.kt
│   │   │   │   └── XtreamService.kt
│   │   │   └── repository/
│   │   │       ├── Database.kt          # Room DB + DAOs
│   │   │       └── IptvRepository.kt
│   │   ├── viewmodel/
│   │   │   └── MainViewModel.kt
│   │   └── ui/
│   │       ├── theme/Theme.kt
│   │       └── screens/
│   │           ├── Navigation.kt
│   │           ├── HomeScreen.kt        # Kanal grid + sidebar
│   │           ├── PlayerScreen.kt      # ExoPlayer fullscreen
│   │           ├── EpgScreen.kt         # EPG timeline grid
│   │           ├── SettingsScreen.kt
│   │           └── AddPlaylistScreen.kt
│   └── res/
│       ├── values/themes.xml
│       └── drawable/
├── build.gradle.kts
├── settings.gradle.kts
└── gradle/libs.versions.toml
```

---

## Desteklenen Stream Formatları

| Format | ExoPlayer Desteği |
|--------|-------------------|
| HLS (.m3u8) | ✅ Tam |
| MPEG-TS (HTTP) | ✅ Tam |
| DASH | ✅ Tam |
| RTSP | ✅ Tam |
| RTMP | ✅ (extension) |

---

## Google Play'e Yükleme

1. `./gradlew bundleRelease` ile AAB üret
2. Keystore ile imzala:
```bash
jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
  -keystore my-release-key.jks \
  app-release.aab alias_name
```
3. Google Play Console → Production → AAB yükle

---

## Apple TV (v2 — Yakında)

SwiftUI + AVFoundation ile aynı özellikler.  
Bkz: `HasiTV-iOS/` klasörü (hazırlanıyor)

---

© 2026 Hasi Elektronic — Hamdi Güncavdi  
Grabenstraße 18, 71665 Vaihingen an der Enz
