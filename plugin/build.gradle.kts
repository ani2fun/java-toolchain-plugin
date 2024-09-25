import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.pluginpublish) // Publish plugins to the Gradle Plugin Portal
    `java-gradle-plugin`
    `maven-publish`
}

group = "eu.kakde.gradle"
version = "1.0.1"

gradlePlugin {
    website = "https://github.com/ani2fun/java-toolchain-plugin.git"
    vcsUrl = "https://github.com/ani2fun/java-toolchain-plugin.git"

    val sonatypeMavenCentralPublish by plugins.creating {
        id = "eu.kakde.gradle.java-toolchain-plugin"
        version = project.version
        implementationClass = "eu.kakde.JavaToolChainPlugin"
        displayName = "Java Tool Chain Management Plugin"
        description = "Gradle plugin for managing same java toolchain versions across microservices."
        tags = listOf("maven", "maven-central", "publish", "java")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}
