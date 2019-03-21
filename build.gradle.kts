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
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.discord4j:discord4j-core:3.0.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
