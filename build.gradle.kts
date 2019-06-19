import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "1.5.1"

application {
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

plugins {
    idea
    application
    kotlin("jvm") version "1.3.31"
    id("com.google.protobuf").version("0.8.8")
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.2.1")
    compileOnly("com.google.protobuf", "protobuf-gradle-plugin", "0.8.8")

    implementation("com.jagrosh", "jda-utilities", "2.1.5")
    implementation("net.dv8tion", "JDA", "3.8.3_463") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.jetbrains.exposed", "exposed", "0.14.1")
    implementation("joda-time", "joda-time", "2.10.2")
    implementation("com.github.kittinunf.fuel", "fuel", "2.0.1")
    implementation("com.google.protobuf", "protobuf-java", "3.7.0")
    implementation("io.github.microutils", "kotlin-logging", "1.6.24")
    implementation("org.quartz-scheduler", "quartz", "2.3.0")

    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.11.2")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.21.0.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
