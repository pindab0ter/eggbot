import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "3.0.0"

application {
    applicationName = "EggBot"
    mainClass.set("nl.pindab0ter.eggbot.MainKt")
}

plugins {
    idea
    application
    kotlin("jvm") version "1.8.0-RC"
    kotlin("plugin.serialization") version "1.8.0-RC"
    id("com.squareup.wire") version "4.4.3"
    id("com.github.ben-manes.versions") version "0.44.0"
}

wire {
    kotlin {}
}

repositories {
    mavenCentral()
    maven {
        // Required for Kord and Kord Extensions
        name = "Kord Extensions"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
    maven {
        name = "Kord Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.6.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.4")
    implementation("joda-time", "joda-time", "2.12.2")
    implementation("ch.obermuhlner", "big-math", "2.3.2")

    // Configuration
    implementation("com.charleskorn.kaml", "kaml", "0.49.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.41.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.41.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.41.1")
    implementation("org.jetbrains.exposed", "exposed-jodatime", "0.41.1")
    implementation("org.flywaydb", "flyway-core", "9.10.0")
    runtimeOnly("org.postgresql", "postgresql", "42.5.1")

    // Networking
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    implementation("com.squareup.wire", "wire-gradle-plugin", "4.4.3")

    // Discord
    implementation("com.kotlindiscord.kord.extensions", "kord-extensions", "1.5.6-SNAPSHOT")

    // Task scheduling
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    // Logging
    runtimeOnly("ch.qos.logback", "logback-classic", "1.4.5")
    implementation("io.github.microutils", "kotlin-logging-jvm", "3.0.4")
    implementation("io.sentry", "sentry", "6.9.2")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.withType<JavaCompile>().configureEach {
    enabled = false
}

