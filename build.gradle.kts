buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "de.tobiasgies.ootr"
version = "1.0-SNAPSHOT"

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
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("io.micrometer:micrometer-core:1.11.5")
    implementation("io.opentelemetry.instrumentation:opentelemetry-micrometer-1.5:1.30.0-alpha")
    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:1.30.0")
    implementation("io.opentelemetry:opentelemetry-extension-kotlin:1.31.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileKotlin {
    kotlinOptions {
        javaParameters = true
    }
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("de.tobiasgies.ootr.draftbot.MainKt")
}