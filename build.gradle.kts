buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("io.sentry.jvm.gradle") version "3.13.0"
    id("co.uzzu.dotenv.gradle") version "2.0.0"
}

group = "de.tobiasgies.ootr"
version = "1.0-SNAPSHOT"

sentry {
    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
    // This enables source context, allowing you to see your source
    // code as part of your stack traces in Sentry.
    includeSourceContext = true

    org = "tobiasgies"
    projectName = "ootr-draftbot"
    authToken = env.SENTRY_AUTH_TOKEN.value
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("net.dv8tion:JDA:5.0.0-beta.15")
    implementation("com.github.minndevelopment:jda-ktx:9370cb13cc64646862e6f885959d67eb4b157e4a")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.11")
    implementation("com.squareup.okhttp3:okhttp-coroutines:5.0.0-alpha.11")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("de.tobiasgies.ootr.draftbot.MainKt")
}