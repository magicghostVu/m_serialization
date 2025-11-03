import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.20"
    id("maven-publish")
}


repositories {
    mavenCentral()
}

dependencies{
    api("io.netty:netty-buffer:4.1.85.Final")
}

java {
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    compilerOptions.freeCompilerArgs.set(
        listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xno-call-assertions",
            "-Xno-receiver-assertions",
            "-Xno-param-assertions"
        )
    )
}
tasks.register<Jar>("sourceJar"){
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    /*repositories {
        maven {
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = ""
            }
            // gitlab id 73
            url = uri("")
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }*/


    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/magicghostVu/m_serialization")
            credentials() {
                username = "magicghostVu"
                password = project.properties.getValue("github.deploy.token") as String
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "com.magicghostvu"
            artifactId = "m-serialization-runtime"
            version = "0.1.1"
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
        }
    }
}