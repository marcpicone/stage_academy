# Stage Academy — Mon premier quiz

Une petite application Android en Kotlin et Jetpack Compose, pour découvrir la
programmation pendant un stage de troisième.

Pour le moment, elle affiche un accueil statique. Les questions, les réponses et
le score seront ajoutés progressivement avec le stagiaire.

## Ouvrir le projet

1. Ouvrir ce dossier dans Android Studio (version compatible avec AGP 8.10).
2. Utiliser le JDK intégré à Android Studio (JDK 17 minimum ; JDK 21 convient).
3. Installer Android SDK Platform 36 et accepter les licences via le SDK Manager.
4. Laisser Android Studio synchroniser Gradle, puis lancer `app` sur un émulateur
   ou un téléphone Android 8.0 (API 26) ou supérieur.

Android Studio configure le chemin du SDK dans `local.properties`, qui reste local
à chaque ordinateur. En ligne de commande, on peut aussi définir `ANDROID_HOME`.
Le premier lancement nécessite Internet pour télécharger les dépendances.

## Compiler et vérifier

À la racine du projet, avec `JAVA_HOME` pointant vers le JDK :

```sh
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Sous Windows, utiliser `gradlew.bat` à la place de `./gradlew`.
L’APK de développement se trouve dans `app/build/outputs/apk/debug/app-debug.apk`.

## Repérer les fichiers

- `settings.gradle.kts` : déclare le seul module du projet, `app`.
- `build.gradle.kts` : fixe les versions des outils Android et Kotlin.
- `app/build.gradle.kts` : configure l’application et ses bibliothèques Compose.
- `app/src/main/AndroidManifest.xml` : déclare l’activité de lancement.
- `app/src/main/java/fr/stageacademy/quiz/MainActivity.kt` : ouvre l’application et
  décrit son écran d’accueil. `WelcomeScreenPreview` permet de le prévisualiser.
- `app/src/main/java/fr/stageacademy/quiz/ui/theme/Theme.kt` : applique le thème
  clair ou sombre.
- `app/src/main/res/values/strings.xml` : contient les textes affichés.
- `.gitignore` : écarte de Git les builds, les fichiers IDE locaux et les clés.
- `gradlew`, `gradlew.bat`, `gradle/wrapper/` : lancent la même version de Gradle
  pour tout le monde ; ces fichiers doivent être conservés dans Git.

## Versions retenues

Gradle 8.11.1, plugin Android 8.10.1, Kotlin et compilateur Compose 2.3.21,
Compose BOM 2025.05.01, Activity Compose 1.10.1, SDK de compilation/cible 36.
Les bibliothèques sont fixées à des versions stables ; il ne s’agit pas d’une
sélection automatique des dernières versions.

Références de compatibilité : [plugin Android et Gradle](https://developer.android.com/build/releases/agp-8-10-0-release-notes),
[Kotlin et Gradle](https://kotlinlang.org/docs/gradle-configure-project.html),
[compilateur Compose](https://developer.android.com/develop/ui/compose/compiler),
[Compose BOM de mai 2025](https://android-developers.googleblog.com/2025/05/whats-new-in-jetpack-compose.html).
