plugins {
    id("com.android.library")
    kotlin("android")

    id("org.jetbrains.kotlin.plugin.serialization")
    id("mvn-publish")
}

val VERSION_NAME: String by project

android {
    compileSdk = 31
    buildToolsVersion = "31.0.0"
    testOptions.unitTests.isIncludeAndroidResources = true


    defaultConfig {
        multiDexEnabled = true
        minSdk = 21
        targetSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-consumer-rules.pro")

        buildConfigField("String", "VERSION_NAME", "\"$VERSION_NAME\"")
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
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("com.segment.analytics.kotlin:android:1.10.3")
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")

    implementation("androidx.lifecycle:lifecycle-process:2.4.1")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.1")

}
// Partner Dependencies
dependencies {
    implementation("com.facebook.android:facebook-android-sdk:16.0.1")
}

// Test Dependencies
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.4")

    // Add JUnit5 dependencies.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")

    // Add JUnit4 legacy dependencies.
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")

    // Add Roboelectric dependencies.
    testImplementation("org.robolectric:robolectric:4.7.3")
    testImplementation("androidx.test:core:1.5.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// required for mvn-publish
// too bad we can't move it into mvn-publish plugin because `android`is only accessible here
tasks {
    val sourceFiles = android.sourceSets.getByName("main").java.srcDirs

    register<Javadoc>("withJavadoc") {
        isFailOnError = false

        setSource(sourceFiles)

        // add Android runtime classpath
        android.bootClasspath.forEach { classpath += project.fileTree(it) }

        // add classpath for all dependencies
        android.libraryVariants.forEach { variant ->
            variant.javaCompileProvider.get().classpath.files.forEach { file ->
                classpath += project.fileTree(file)
            }
        }
    }

    register<Jar>("withJavadocJar") {
        archiveClassifier.set("javadoc")
        dependsOn(named("withJavadoc"))
        val destination = named<Javadoc>("withJavadoc").get().destinationDir
        from(destination)
    }

    register<Jar>("withSourcesJar") {
        archiveClassifier.set("sources")
        from(sourceFiles)
    }
}