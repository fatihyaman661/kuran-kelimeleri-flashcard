import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Google Play yükleme anahtarı: proje kökündeki keystore.properties (storeFile, storePassword,
// keyAlias, keyPassword). Depoda yoktur; dosya yoksa release çıktısı imzasız kalır.
val playKeystore = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

// Senkron ayarları local.properties'ten okunur; boşsa uygulama senkronu kapalı gösterir.
//   sync.url=https://<proje>.pages.dev
//   sync.googleClientId=<Google Cloud'daki Web istemci kimliği>.apps.googleusercontent.com
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}
fun localProp(key: String) = (localProps.getProperty(key) ?: providers.gradleProperty(key).orNull).orEmpty()

android {
    namespace = "com.mevlutcelik.kelimeler"
    compileSdk = 36

    defaultConfig {
        // Play paket kimliği; ilk yüklemeden sonra değiştirilemez.
        applicationId = "com.mevlutcelik.kelimeler"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "DECK_BASE_URL", "\"https://raw.githubusercontent.com/fatihyaman661/kuran-kelimeleri-flashcard/main/data/\"")
        buildConfigField("String", "SYNC_URL", "\"${localProp("sync.url").trimEnd('/')}\"")
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${localProp("sync.googleClientId")}\"")
    }

    signingConfigs {
        if (!playKeystore.isEmpty) {
            create("release") {
                storeFile = rootProject.file(playKeystore.getProperty("storeFile"))
                storePassword = playKeystore.getProperty("storePassword")
                keyAlias = playKeystore.getProperty("keyAlias")
                keyPassword = playKeystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        // Küçültülmüş release kodu, debug anahtarıyla imzalı: cihazda gerçek hızı denemek için.
        create("preview") {
            initWith(getByName("release"))
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Uygulamayla gelen deste: data/deck.db.gz derleme sırasında assets'e kopyalanır, depoda tek kopya kalır.
abstract class BundleDeckTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val deck: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        deck.get().asFile.copyTo(out.resolve("deck.db.gz"))
    }
}

val bundleDeck = tasks.register<BundleDeckTask>("bundleDeck") {
    deck.set(rootProject.layout.projectDirectory.file("data/deck.db.gz"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(bundleDeck, BundleDeckTask::outputDir)
    }
}

dependencies {
    implementation(project(":fsrs"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
}

kotlin { compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
