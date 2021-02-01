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
    kotlin("jvm") version "1.5.0-RC"
    id("com.github.ben-manes.versions") version "0.38.0"
    id("com.toasttab.protokt") version "0.5.4"
    id("org.openjfx.javafxplugin") version "0.0.9"
}

repositories {
    mavenCentral()
    jcenter()
    maven(url = "https://kotlin.bintray.com/kotlinx/")
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "1.5.0-RC")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.4.3")
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.0.0")
    implementation("joda-time", "joda-time", "2.10.10")
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    implementation("org.jetbrains.exposed", "exposed", "0.17.13")
    implementation("com.jagrosh", "jda-utilities-command", "3.0.5")
    implementation("net.dv8tion", "JDA", "4.2.0_247") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.campagnelab.ext", "jsap", "3.0.0")
    implementation("ch.obermuhlner", "big-math", "2.3.0")
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    implementation("org.openjfx", "javafx-base", "16-ea+6")
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-javafx", "1.4.2")
    implementation("org.jetbrains.kotlinx", "kotlinx-datetime", "0.1.1")
    implementation("io.data2viz.d2v", "core-jvm", "0.8.10")
    implementation("io.data2viz.d2v", "color-jvm", "0.8.10")
    implementation("io.data2viz.d2v", "axis", "0.8.10")
    implementation("io.data2viz.d2v", "scale-jvm", "0.8.10")
    implementation("io.data2viz.d2v", "viz", "0.8.10")
    implementation("io.data2viz.d2v", "viz-jfx", "0.8.10")

    runtimeOnly("com.google.protobuf", "protobuf-java", "4.0.0-rc-2")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.14.1")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.34.0")
}

javafx {
    modules("javafx.fxml", "javafx.swing")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
    jvmTarget = "15"
    freeCompilerArgs = freeCompilerArgs.plus(listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi",
        "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
    ))
}

val compileJava: JavaCompile by tasks
compileJava.enabled = false

configurations.forEach { configuration ->
    // Workaround the Gradle bug resolving multi platform dependencies.
    // https://github.com/square/okio/issues/647
    if (configuration.name.contains("proto", ignoreCase = true)) {
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "java-runtime"))
    }
}
