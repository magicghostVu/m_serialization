import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}

group = "org.magicghostvu"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))


    // import netty
    implementation("io.netty:netty-buffer:4.1.85.Final")


    // để có thể dùng các annotation trong project
    implementation(project(":ksp_logic"))

    // chạy code gen trong build time
    ksp(project(":ksp_logic"))
    implementation(project(":m_serialization_annotation"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}