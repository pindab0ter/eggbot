import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "0.2.1"

plugins {
    idea
    application
    kotlin("jvm") version "1.3.21"
    id("com.google.protobuf").version("0.8.8")
}

application {
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.jagrosh", "jda-utilities-command", "2.1.2")
    implementation("net.dv8tion", "JDA", "3.8.3_462") {
        exclude("opus-java")
    }
    implementation("org.jetbrains.exposed", "exposed", "0.13.4")
    implementation("org.xerial", "sqlite-jdbc", "3.21.0.1")
    implementation("com.github.kittinunf.fuel", "fuel", "2.0.1")
    implementation("com.google.protobuf", "protobuf-java", "3.7.0")
    implementation("com.google.protobuf", "protobuf-gradle-plugin", "0.8.8")
    implementation("io.github.microutils", "kotlin-logging", "1.6.24")
    implementation("org.quartz-scheduler", "quartz", "2.3.0")

    runtime("org.apache.logging.log4j", "log4j-slf4j-impl", "2.11.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
