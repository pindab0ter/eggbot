import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "1.5.3"

application {
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

plugins {
    idea
    application
    kotlin("jvm") version "1.3.41"
    id("com.google.protobuf").version("0.8.10")
    id("com.github.ben-manes.versions").version("0.21.0")
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.0-M2")
    compileOnly("com.google.protobuf", "protobuf-gradle-plugin", "0.8.10")

    implementation("com.jagrosh", "jda-utilities-commons", "2.1.5")
    implementation("com.jagrosh", "jda-utilities-command", "2.1.5")
    implementation("net.dv8tion", "JDA", "3.8.3_464") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.jetbrains.exposed", "exposed", "0.16.2")
    implementation("joda-time", "joda-time", "2.10.3")
    implementation("com.github.kittinunf.fuel", "fuel", "2.1.0")
    implementation("com.google.protobuf", "protobuf-java", "3.9.0")
    implementation("io.github.microutils", "kotlin-logging", "1.6.26")
    implementation("org.quartz-scheduler", "quartz", "2.3.1")
    implementation(files("libs/BigDecimalMath.jar"))

    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.12.0")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.28.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
