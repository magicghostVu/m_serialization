import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("maven-publish")
}

repositories {
    mavenCentral()

    // github package
    maven {
        url = uri("https://maven.pkg.github.com/magicghostVu/m_serialization")
        credentials {
            username = "magicghostVu"
            password = property("github.pull.package") as String
        }
    }


}

dependencies {
    //testImplementation(kotlin("test"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.10-1.0.9")
    //implementation("io.netty:netty-buffer:4.1.85.Final")

    // add graph lib
    implementation("org.jgrapht:jgrapht-core:1.4.0")
    implementation("org.jgrapht:jgrapht-io:1.4.0")

    // add kotlin poet
    runtimeOnly("com.squareup:kotlinpoet:1.13.2")
    implementation("com.squareup:kotlinpoet-ksp:1.13.2")

    //implementation(project(":m_serialization_annotation"))

    implementation("com.magicghostvu","m-serialization-runtime", "0.1.1")

}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

publishing {
    /*repositories {
        maven {
            credentials(HttpHeaderCredentials::class) {
                name = "Private-Token"
                value = project.properties.getValue("deploy.token") as String
            }

            // project id 74
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
            artifactId = "m-serialization-codegen"
            version = "0.1.2"
            from(components["kotlin"])
            artifact(tasks["sourcesJar"])
        }
    }
}
