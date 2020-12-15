import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "nl.pindab0ter"
version = "2.0.0"

application {
    applicationName = "EggBot"
    mainClassName = "nl.pindab0ter.eggbot.EggBot"
}

plugins {
    idea
    application
    kotlin("jvm") version "1.4.21"
    id("com.github.ben-manes.versions") version "0.36.0"
    id("com.toasttab.protokt") version "0.5.4"
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.4.2")
    implementation("org.apache.logging.log4j", "log4j-api-kotlin", "1.0.0")
    implementation("joda-time", "joda-time", "2.10.8")
    implementation("com.github.kittinunf.fuel", "fuel", "2.3.1")
    implementation("org.jetbrains.exposed", "exposed", "0.17.7")
    implementation("com.jagrosh", "jda-utilities-command", "3.0.5")
    implementation("net.dv8tion", "JDA", "4.2.0_222") {
        exclude("club.minnced", "opus-java")
    }
    implementation("org.campagnelab.ext", "jsap", "3.0.0")
    implementation("ch.obermuhlner", "big-math", "2.3.0")
    implementation("org.quartz-scheduler", "quartz", "2.3.2")

    runtimeOnly("com.google.protobuf", "protobuf-java", "4.0.0-rc-2")
    runtimeOnly("org.apache.logging.log4j", "log4j-slf4j-impl", "2.14.0")
    runtimeOnly("org.xerial", "sqlite-jdbc", "3.34.0")

    testImplementation("io.kotest", "kotest-runner-junit5-jvm", "4.3.2")
    implementation("io.kotest", "kotest-assertions-core-jvm", "4.3.2")
    testImplementation("io.kotest", "kotest-property-jvm", "4.3.2")
    testImplementation("io.mockk", "mockk", "1.10.3")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    languageVersion = "1.4"
    jvmTarget = "1.8"
    freeCompilerArgs = freeCompilerArgs.plus(listOf(
        "-Xopt-in=kotlin.RequiresOptIn",
        "-Xopt-in=kotlin.ExperimentalStdlibApi"
    ))
}

val compileJava: JavaCompile by tasks
compileJava.enabled = false

val test: Test by tasks
test.useJUnitPlatform()

configurations.forEach { configuration ->
    // Workaround the Gradle bug resolving multi platform dependencies.
    // https://github.com/square/okio/issues/647
    if (configuration.name.contains("proto", ignoreCase = true)) {
        configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, "java-runtime"))
    }
}
