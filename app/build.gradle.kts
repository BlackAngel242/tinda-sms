plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

import java.util.Properties

android {
    namespace = "tech.dmampro.tinda"
    compileSdk = 36
    // Build Tools installés localement : 36.0.0 (le défaut d'AGP 8.10.x est 35.0.0).
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "tech.dmampro.tinda"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // 4 ABI -> 2 : x86/x86_64 ne servent qu'aux émulateurs. -10 Mo d'APK,
        // crucial pour les connexions lentes (testeur sous réseau faible).
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // BuildConfig requis par TindaApp (StrictMode) et TindaLogger.
    buildFeatures {
        buildConfig = true
    }

    // Les artefacts Kotlin multiplatform embarquent des métadonnées en double.
    packaging {
        resources {
            excludes += setOf(
                "commonMain/**",
                "nativeMain/**",
                "META-INF/kotlin-project-structure-metadata.json"
            )
        }
    }

    // Signature debug permanente : le keystore auto-généré d'AGP ne survit
    // pas aux redémarrages de la machine (clé perdue le 19 oct. 2026 ->
    // INSTALL_FAILED_UPDATE_INCOMPATIBLE). Clé versionnée hors git dans
    // keystore/, mots de passe dans keystore.properties (gitignoré).
    //
    // La signature permanente n'est appliquée que si la clé est
    // réellement présente sur la machine (poste de SmokeSama et cette
    // machine de développement). En CI, la clé permanente ne doit
    // jamais être publiée : repli sur la signature debug standard
    // d'AGP, générée sur le runner et jetable. La signature installée
    // sur le téléphone ne change donc pas.
    val keystoreProps = Properties()
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
    }
    val permanentStoreFile = rootProject.file(
        keystoreProps.getProperty("storeFile", "keystore/tinda-debug.keystore"),
    )
    val permanentKeystorePresent =
        keystorePropsFile.exists() && permanentStoreFile.exists()
    signingConfigs {
        if (permanentKeystorePresent) {
            create("tindaDebug") {
                storeFile = permanentStoreFile
                storePassword = keystoreProps.getProperty("storePassword", "")
                keyAlias = keystoreProps.getProperty("keyAlias", "tinda-debug")
                keyPassword = keystoreProps.getProperty("keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = if (permanentKeystorePresent) {
                signingConfigs.getByName("tindaDebug")
            } else {
                // CI : signature debug standard d'AGP (clé du runner).
                signingConfigs.getByName("debug")
            }
            // Minification aussi en debug : l'APK passe de ~32 Mo à ~15 Mo,
            // téléchargeable sur connexion lente. R8 full mode : Hilt, Room
            // et Compose fournissent leurs règles via leurs AAR.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

// Schémas Room exportés et versionnés (P0-5) : app/schemas/.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// MigrationTestHelper lit les schémas exportés depuis les assets du
// test instrumenté : voir le sourceSets androidTest dans android {}.

dependencies {
    val bom = platform(libs.androidx.compose.bom)
    implementation(bom)
    androidTestImplementation(bom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.sqlcipher)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)

    ksp(libs.androidx.room.compiler)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    // Uniquement pour les tests instrumentés exécutés par la CI ou
    // sur appareil ; ces artefacts ne sont pas requis par l'app.
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
