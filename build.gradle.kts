import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "dev.reeve"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.github.gotson:sqlite-jdbc:3.32.3.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.google.code.gson:gson:2.9.0")
    
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
	
    implementation("io.ktor:ktor-server-netty:2.1.1")
    implementation("io.ktor:ktor-server-core:2.1.1")
    implementation("io.ktor:ktor-gson:1.6.8")
    implementation("io.ktor:ktor-serialization-gson:2.1.1")
    implementation("io.ktor:ktor-server-content-negotiation:2.1.1")
    
    implementation("dev.reeve:TorrustApiWrapper:latest")
	implementation(kotlin("script-runtime"))
	
	testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}