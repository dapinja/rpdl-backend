import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "dev.reeve"
version = "1.0-SNAPSHOT"

project.setProperty("mainClassName", "dev.reeve.rpdl.backend.MainKt")

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.reeve.dev/repository/maven-releases/")
}

dependencies {
    implementation("com.github.gotson:sqlite-jdbc:3.32.3.8")
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.google.code.gson:gson:2.9.0")
    
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    
    implementation("io.ktor:ktor-server-netty:2.1.2")
    implementation("io.ktor:ktor-server-core:2.1.2")
    implementation("io.ktor:ktor-gson:1.6.8")
    implementation("io.ktor:ktor-serialization-gson:2.1.2")
    implementation("io.ktor:ktor-server-content-negotiation:2.1.2")
    
    implementation("dev.reeve:TorrustApiWrapper:latest")
    
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("dev.reeve.rpdl.backend.MainKt")
}