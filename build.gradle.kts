import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "2.0.0"

application {
    applicationName = "EggBot"
    mainClass.set("nl.pindab0ter.eggbot.MainKt")
}

plugins {
    idea
    application
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
    id("com.squareup.wire") version "4.3.0"
    id("com.github.ben-manes.versions") version "0.42.0"
}

wire {
    kotlin {}
}

repositories {
    mavenCentral()

    maven {
        // Required for Kord and Kord Extensions
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.6.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.1")
    implementation("joda-time", "joda-time", "2.10.14")
    implementation("ch.obermuhlner", "big-math", "2.3.0")


    // Configuration
    implementation("com.charleskorn.kaml", "kaml", "0.44.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed-core", "0.38.2")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.38.2")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.38.2")
    implementation("org.jetbrains.exposed", "exposed-jodatime", "0.38.2")
    runtimeOnly("org.postgresql", "postgresql", "42.3.5")
    implementation("org.flywaydb", "flyway-core", "8.5.11")

    // Networking
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    implementation("com.squareup.wire", "wire-gradle-plugin", "4.3.0")

    // Discord
    implementation("com.kotlindiscord.kord.extensions", "kord-extensions", "1.5.2-RC1")

    // Task scheduling
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    // Logging
    runtimeOnly("ch.qos.logback", "logback-classic", "1.2.11")
    implementation("io.github.microutils", "kotlin-logging-jvm", "2.1.21")
    implementation("io.sentry", "sentry", "6.0.0-beta.3")
}

tasks {
    withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
        }
    }

    withType<JavaCompile>().configureEach {
        enabled = false
        targetCompatibility = "1.8"
    }
}
