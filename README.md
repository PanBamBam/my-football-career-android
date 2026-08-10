# My Football Career 1.0 — Android

Natywny wrapper Android dla stabilnej wersji My Football Career 1.0.

## Funkcje
- gra działa lokalnie/offline w WebView,
- trwały localStorage pozostaje wewnątrz aplikacji,
- import `.oolsave`, `.oolbundle` i `.json` korzysta z systemowego selektora plików,
- eksport save'ów na Androidzie 10+ trafia do `Pobrane/My Football Career`,
- tryb immersive fullscreen,
- sprzętowo akcelerowany WebView,
- żadnego zewnętrznego serwera do działania gry.

## Automatyczny build
Workflow `.github/workflows/build-android.yml` buduje instalacyjny APK i publikuje go jako artefakt GitHub Actions.

## Parametry
- package: `com.myfootballcareer.game`
- versionName: `1.0.0`
- minSdk: 24 (Android 7.0)
- targetSdk: 35
