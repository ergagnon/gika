plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "com.ginder"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.apache.tika:tika-core:2.8.0")
    implementation("org.apache.tika:tika-parsers-standard-package:2.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}