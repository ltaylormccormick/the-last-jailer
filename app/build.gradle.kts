plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Empty image resources can package successfully but crash painterResource at runtime.
val validateDrawableAssets by tasks.registering {
    val drawables = fileTree("src/main/res") { include("drawable*/**/*") }
    inputs.files(drawables)
    doLast {
        val emptyFiles = drawables.files.filter { it.length() == 0L }
        check(emptyFiles.isEmpty()) {
            "Empty drawable resources: " + emptyFiles.joinToString { it.name }
        }
    }
}
tasks.named("preBuild") { dependsOn(validateDrawableAssets) }

android {
    namespace = "com.thelastjailer.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thelastjailer.app"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("playVersionCode").orNull?.let { value ->
            val code = value.toIntOrNull()
            require(code != null && code in 1..2100000000) {
                "playVersionCode must be an integer from 1 to 2100000000"
            }
            code
        } ?: 1
        versionName = "0.1.0"
        buildConfigField("boolean", "TESTER_UNLOCK_ENABLED", "false")
    }

    buildTypes {
        create("internalTest") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "TESTER_UNLOCK_ENABLED", "true")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.android.billingclient:billing:9.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
