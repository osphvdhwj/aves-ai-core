plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.osphvdhwj.aves.ai"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.osphvdhwj.aves.ai"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        aidl = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
            vendor.set(JvmVendorSpec.ADOPTIUM)
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
}
