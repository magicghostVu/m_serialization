import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
}


repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/magicghostVu/m_serialization")
        credentials {
            username = "magicghostVu"
            password = property("github.pull.package") as String
        }
    }
}

dependencies {
    // import netty
    implementation("io.netty:netty-buffer:4.1.85.Final")


    // để có thể dùng các annotation trong project
    //implementation(project(":ksp_logic"))

    // chạy code gen trong build time
    ksp(project(":ksp_logic"))
    implementation(project(":m_serialization_annotation"))
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.register("cleanKsp") {
    doLast {
        delete("$buildDir/generated/ksp/main/kotlin")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("cleanKsp")
    kotlinOptions.jvmTarget = "1.8"
}