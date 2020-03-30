import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = ""
version = "1.0-SNAPSHOT"

plugins {
    kotlin("jvm") version "1.3.71"
    kotlin("kapt") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "2.0.4"
}

repositories {
    mavenCentral()
    maven {
        name = "spigotmc-repo"
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        name = "sonatype-oss"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "enginehub-maven"
        url = uri("http://maven.enginehub.org/repo/")
    }
    maven {
        name = "aikar"
        url = uri("https://repo.aikar.co/content/groups/aikar/")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(group = "co.aikar", name = "acf-paper", version = "0.5.0-SNAPSHOT")

    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.14.4-R0.1-SNAPSHOT")
    compileOnly(group = "com.sk89q.worldedit", name = "worldedit-bukkit", version = "7.1.0-SNAPSHOT")

    kapt(group = "org.spigotmc", name = "plugin-annotations", version = "1.2.2-SNAPSHOT")
}

tasks.shadowJar {
    relocate("co.aikar.commands", "redstonetools.acf")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
