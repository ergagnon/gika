import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "1.9.20"
    id("com.google.protobuf") version "0.9.4"
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
    implementation("com.google.protobuf:protobuf-kotlin:3.25.1")
    implementation("io.grpc:grpc-protobuf:1.59.1")
    implementation("io.grpc:grpc-protobuf-lite:1.59.1")
    implementation("io.grpc:grpc-services:1.59.1")
    implementation("io.grpc:grpc-stub:1.59.1")
    implementation("io.grpc:grpc-netty:1.59.1")
    implementation("io.grpc:grpc-kotlin-stub:1.4.1")
}

kotlin {
    jvmToolchain(19)
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.22.2"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.59.1"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.4.1:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
                id("grpckt")
            }
            it.builtins {
                id("kotlin")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("MainKt")
}