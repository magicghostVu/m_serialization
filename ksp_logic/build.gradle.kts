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

    // add kotlin poet
    runtimeOnly("com.squareup:kotlinpoet:1.13.2")
    implementation("com.squareup:kotlinpoet-ksp:1.13.2")

    implementation(project(":m_serialization_annotation"))

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

task("copyDependencies", Copy::class) {
    configurations
        .compileClasspath
        .get()
        .filter {
            it.extension == "jar"
        }
        .forEach {
            from(it.absolutePath).into("$buildDir/all_ksp_libs")
        }
}