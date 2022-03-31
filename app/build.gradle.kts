repositories {
    mavenCentral()
    google()
    maven("https://linphone.org/maven_repository/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")

}

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 23
        targetSdk = compileSdk
        applicationId = "test.android.linphone"
        versionCode = 3
        versionName = "0.$versionCode"
    }

    sourceSets["main"].java.srcDir("src/main/kotlin")

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".$name"
            versionNameSuffix = "-$name"
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    applicationVariants.all {
        outputs.forEach { output ->
            check(output is com.android.build.gradle.internal.api.ApkVariantOutputImpl)
            output.versionCodeOverride = versionCode
            output.outputFileName = "$applicationId-$versionName-$versionCode.apk"
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.4.1")
    debugImplementation("org.linphone:linphone-sdk-android-debug:5.0.71")
    implementation("com.github.kepocnhh:KotlinExtension.Functional:0.3-SNAPSHOT")
}
