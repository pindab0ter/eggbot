import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "1.7.0"

application {
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

plugins {
    idea
    application
    kotlin("jvm") version "1.3.61"
    id("com.google.protobuf") version "0.8.10"
    id("com.github.ben-manes.versions") version "0.27.0"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.2-1.3.60")
    compileOnly("com.google.protobuf", "protobuf-gradle-plugin", "0.8.10")

    implementation("com.jagrosh", "jda-utilities-commons", "3.0.2")
    implementation("com.jagrosh", "jda-utilities-command", "3.0.2")
    implementation("net.dv8tion", "JDA", "4.0.0_73") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.jetbrains.exposed", "exposed", "0.17.7")
    implementation("joda-time", "joda-time", "2.10.5")
    implementation("com.github.kittinunf.fuel", "fuel", "2.2.1")
    implementation("com.google.protobuf", "protobuf-java", "3.11.1")
    implementation("io.github.microutils", "kotlin-logging", "1.7.8")
    implementation("org.quartz-scheduler", "quartz", "2.3.2")
    implementation("ch.obermuhlner", "big-math", "2.3.0")

    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.12.1")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.28.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
