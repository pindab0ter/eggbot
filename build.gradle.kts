import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "1.8.0"

application {
    applicationName = "EggBot"
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

plugins {
    idea
    application
    kotlin("jvm") version "1.3.72"
    id("com.github.ben-manes.versions") version "0.28.0"
    id("com.toasttab.protokt") version "0.4.3"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.2-1.3.60")
    implementation("io.github.microutils", "kotlin-logging", "1.7.10")
    implementation("joda-time", "joda-time", "2.10.6")
    implementation("com.github.kittinunf.fuel", "fuel", "2.2.3")
    implementation("org.jetbrains.exposed", "exposed", "0.17.7")
    implementation("com.jagrosh", "jda-utilities-command", "3.0.4")
    implementation("net.dv8tion", "JDA", "4.1.1_165") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.campagnelab.ext", "jsap", "3.0.0")
    implementation("ch.obermuhlner", "big-math", "2.3.0")
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    runtimeOnly("com.google.protobuf", "protobuf-java", "3.12.2")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.13.3")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.32.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    enabled = false
}
