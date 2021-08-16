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
    kotlin("jvm") version "1.5.30-M1"
    id("com.github.ben-manes.versions") version "0.39.0"
    id("com.toasttab.protokt") version "0.6.4"
}

repositories {
    mavenCentral()
    maven {
        name = "Sonatype"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.5.30-M1")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.5.1")
    implementation("joda-time", "joda-time", "2.10.10") // TODO: Replace with Kotlin's time library
    implementation("ch.obermuhlner", "big-math", "2.3.0")

    // Database
    implementation("org.jetbrains.exposed", "exposed", "0.17.13")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.36.0.1")

    // Networking
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    runtimeOnly("com.google.protobuf", "protobuf-java", "4.0.0-rc-2")

    // Discord
    implementation("dev.kord", "kord-core", "0.8.0-M3")
    implementation("com.kotlindiscord.kord.extensions", "kord-extensions", "1.4.4-RC2")

    // Task scheduling
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    // Logging
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.14.1")
    implementation("io.github.microutils", "kotlin-logging-jvm", "2.0.10")
    implementation("io.sentry", "sentry", "5.0.1")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.5"
    jvmTarget = "11"
    freeCompilerArgs = freeCompilerArgs.plus(
        listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
        )
    )
}

val compileJava: JavaCompile by tasks
compileJava.enabled = false

configurations.forEach { configuration ->
    // Workaround the Gradle bug resolving multi-platform dependencies.
    // https://github.com/square/okio/issues/647
    if (configuration.name.contains("proto", ignoreCase = true)) {
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "java-runtime"))
    }
}
