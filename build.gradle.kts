import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("com.google.devtools.ksp") version "2.1.20-2.0.0"
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


tasks.register("cleanKsp") {
    doLast {
        delete("${layout.buildDirectory.get()}/generated/ksp/main/kotlin")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("cleanKsp")
}