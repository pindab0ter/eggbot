import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "0.0.1"

plugins {
    application
    kotlin("jvm") version "1.3.21"
}

application {
    mainClassName = "nl.pindab0ter.eggbot.MainKt"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("net.dv8tion", "JDA", "3.8.3_462") {
        exclude("opus-java")
    }
    implementation("org.jetbrains.exposed", "exposed", "0.13.4")
    implementation("org.xerial", "sqlite-jdbc", "3.21.0.1")
    runtime("org.slf4j", "slf4j-simple", "1.7.26")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
