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
    id("com.toasttab.protokt") version "0.8.0"
    id("com.github.ben-manes.versions") version "0.42.0"
}

repositories {
    mavenCentral()

    maven {
        // Required for Kord
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }

    maven {
        // Required for Kord Extensions
        name = "Sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.6.0")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.1")
    implementation("joda-time", "joda-time", "2.10.14") // TODO: Replace with Kotlin's time library
    implementation("ch.obermuhlner", "big-math", "2.3.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed", "0.17.14")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.36.0.3")
    implementation("org.flywaydb", "flyway-core", "8.5.9")

    // Networking
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    runtimeOnly("com.google.protobuf", "protobuf-java", "4.0.0-rc-2")

    // Discord
    implementation("dev.kord", "kord-core", "0.8.0-M9")
    implementation("com.kotlindiscord.kord.extensions", "kord-extensions", "1.5.2-RC1")

    // Task scheduling
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    // Logging
    runtimeOnly("ch.qos.logback", "logback-classic", "1.2.11")
    implementation("io.github.microutils", "kotlin-logging-jvm", "2.1.21")
    implementation("io.sentry", "sentry", "6.0.0-alpha.6")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.languageVersion = "1.6"
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs += listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
    )
}

tasks.withType<JavaCompile>().configureEach {
    enabled = false
}
