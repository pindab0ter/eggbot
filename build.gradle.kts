import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "2.0.0"

application {
    applicationName = "EggBot"
    mainClass.set("nl.pindab0ter.eggbot.EggBot")
}

plugins {
    idea
    application
    kotlin("jvm") version "1.6.10"
    id("com.github.ben-manes.versions") version "0.41.0"
    id("com.toasttab.protokt") version "0.7.5"
}

repositories {
    mavenCentral()
    maven {
        name = "JCenter"
        url = uri("https://jcenter.bintray.com/")
    }
    maven {
        name = "m2-dv8tion"
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.6.10")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.6.0")
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.1.0")
    implementation("joda-time", "joda-time", "2.10.13")
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    implementation("org.jetbrains.exposed", "exposed", "0.17.14")
    implementation("com.jagrosh", "jda-utilities-command", "3.0.5")
    implementation("net.dv8tion", "JDA", "4.4.0_350") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.campagnelab.ext", "jsap", "3.0.0")
    implementation("ch.obermuhlner", "big-math", "2.3.0")
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    runtimeOnly("com.google.protobuf", "protobuf-java", "3.19.1")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.17.1")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.36.0.3")
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
        }
}
