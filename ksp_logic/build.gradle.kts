import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"

}

group = "org.magicghostvu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    implementation("io.netty:netty-buffer:4.1.85.Final")

    // add graph lib
    implementation("org.jgrapht:jgrapht-core:1.5.2")
    implementation("org.jgrapht:jgrapht-io:1.5.2")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}