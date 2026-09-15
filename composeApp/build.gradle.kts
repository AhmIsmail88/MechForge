import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

// Release signing credentials live outside the source tree. Resolution order:
//   1. signing/keystore.properties inside the project (backward compatible; gitignored), else
//   2. ${user.home}/.mechforge-signing/keystore.properties
// Without any properties file the release build stays unsigned.
val projectKeystoreProperties = rootProject.file("signing/keystore.properties")
val userKeystoreProperties = File(System.getProperty("user.home"), ".mechforge-signing/keystore.properties")
val keystorePropertiesFile = when {
    projectKeystoreProperties.exists() -> projectKeystoreProperties
    userKeystoreProperties.exists() -> userKeystoreProperties
    else -> null
}
val keystoreProperties = Properties().apply {
    keystorePropertiesFile?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.sqldelight.android.driver)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.sqldelight.driver)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.sqldelight.driver)
            }
        }
    }
}

android {
    namespace = "com.mechforge.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mechforge.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
    }

    signingConfigs {
        if (keystorePropertiesFile != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            // Sharing build: keep R8 off so the packaged Compose/SQLDelight/serialization
            // stack cannot be broken by keep-rule gaps.
            isMinifyEnabled = false
            isShrinkResources = false
            if (keystorePropertiesFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

sqldelight {
    databases {
        create("MechForgeDatabase") {
            packageName.set("com.mechforge.db")
            schemaOutputDirectory.set(file("schemas"))
            // Static chain verification (verifyMigrations) requires the
            // deriveSchemaFromMigrations layout. The pilot instead proves the chain with a
            // real v1 -> v2 upgrade test on a fixture database (MigrationTest), which is the
            // mandatory requirement in README v2 §21 / §7.1 criterion 3.
            verifyMigrations.set(false)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.mechforge.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "MechForge"
            packageVersion = "0.2.0"
            description = "MechForge — Mechanical Engineering Toolkit"
            vendor = "MechForge"

            // The app persists through SQLite over JDBC: the trimmed runtime jpackage
            // builds must carry java.sql (and jdk.unsupported, which the driver touches),
            // otherwise the packaged app dies at startup with
            // NoClassDefFoundError: java/sql/DriverManager.
            modules("java.sql", "jdk.unsupported")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
