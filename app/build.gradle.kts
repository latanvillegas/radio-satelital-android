plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
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
	implementation("androidx.compose.material:material-icons-extended")
	implementation("androidx.media3:media3-exoplayer:1.3.1")
	implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
	implementation("androidx.media3:media3-session:1.3.1")
	implementation("com.google.firebase:firebase-auth")
	implementation("com.google.firebase:firebase-firestore")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

	debugImplementation("androidx.compose.ui:ui-tooling")
	debugImplementation("androidx.compose.ui:ui-test-manifest")
}
