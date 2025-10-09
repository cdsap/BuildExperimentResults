import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("io.github.cdsap.fatbinary") version "1.0"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

fatBinary {
    mainClass = "io.github.cdsap.compare.Main"
    name = "build-experiment-results"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("io.github.cdsap:geapi-data:0.3.3")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.jakewharton.picnic:picnic:0.6.0")
    implementation("com.github.ajalt.clikt:clikt:3.5.4")
    implementation("org.nield:kotlin-statistics:1.2.1")
    implementation("io.ktor:ktor-client-core:2.3.13")
    implementation("io.ktor:ktor-client-cio:2.3.9")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-serialization-gson:2.3.9")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}
