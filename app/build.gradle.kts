import java.util.Properties
import org.gradle.api.GradleException

plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
	id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
	val localPropsFile = rootProject.file("local.properties")
	if (localPropsFile.exists()) {
		localPropsFile.inputStream().use { load(it) }
	}
}

val adminEmailFromGradle = providers.gradleProperty("ADMIN_EMAIL").orNull
val adminEmailFromLocalProperties = sequenceOf(
	localProperties.getProperty("ADMIN_EMAIL"),
	localProperties.getProperty("admin.email"),
).firstOrNull { !it.isNullOrBlank() }
val adminEmailFromEnvironment = System.getenv("ADMIN_EMAIL")

val adminEmailRaw = sequenceOf(
	adminEmailFromGradle,
	adminEmailFromLocalProperties,
	adminEmailFromEnvironment,
).firstOrNull { !it.isNullOrBlank() }

val adminEmail = adminEmailRaw?.trim().orEmpty()
val adminEmailSource = when {
	!adminEmailFromGradle.isNullOrBlank() -> "gradleProperty:ADMIN_EMAIL"
	!adminEmailFromLocalProperties.isNullOrBlank() -> "local.properties:ADMIN_EMAIL"
	!adminEmailFromEnvironment.isNullOrBlank() -> "env:ADMIN_EMAIL"
	else -> "empty"
}

if (adminEmail.isBlank()) {
	throw GradleException(
		"ADMIN_EMAIL no esta configurado. Definelo en ~/.gradle/gradle.properties, local.properties o variable de entorno ADMIN_EMAIL antes de compilar.",
	)
}

android {
	namespace = "com.app.radiosatelital"
	compileSdk = 34

	defaultConfig {
		applicationId = "com.app.radiosatelital"
		minSdk = 24
		targetSdk = 34
		versionCode = 1
		versionName = "1.0"
		buildConfigField("String", "ADMIN_EMAIL", "\"$adminEmail\"")
		buildConfigField("String", "ADMIN_EMAIL_SOURCE", "\"$adminEmailSource\"")
		manifestPlaceholders["ADMIN_EMAIL"] = adminEmail

		testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
	}

	buildTypes {
		release {
			isMinifyEnabled = false
			proguardFiles(
				getDefaultProguardFile("proguard-android-optimize.txt"),
				"proguard-rules.pro"
			)
		}
	}

	buildFeatures {
		compose = true
		buildConfig = true
	}

	composeOptions {
		kotlinCompilerExtensionVersion = "1.5.8"
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
	implementation(platform("androidx.compose:compose-bom:2024.02.00"))
	implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
	implementation("androidx.activity:activity-compose:1.8.2")
	implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
	implementation("androidx.navigation:navigation-compose:2.7.7")
	implementation("androidx.compose.ui:ui")
	implementation("androidx.compose.ui:ui-tooling-preview")
	implementation("androidx.compose.material3:material3")
	implementation("io.coil-kt:coil-compose:2.6.0")
	implementation("com.github.bumptech.glide:glide:4.16.0")
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.media3:media3-exoplayer:1.3.1")
	implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
	implementation("androidx.media3:media3-session:1.3.1")
	implementation("com.google.firebase:firebase-auth")
	implementation("com.google.firebase:firebase-firestore")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
	debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
