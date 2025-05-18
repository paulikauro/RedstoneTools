import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = ""
version = "1.4.2-SNAPSHOT"

plugins {
    val kotlinVersion = "2.1.21"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.gradleup.shadow") version "8.3.6"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.google.re2j:re2j:1.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.0")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.11-SNAPSHOT")
}

bukkit {
    main = "redstonetools.RedstoneTools"
    apiVersion = "1.21"
    depend = listOf("WorldEdit", "NBTAPI")
}

tasks.shadowJar {
    relocate("co.aikar.commands", "redstonetools.acf")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters.set(true)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("getVersion") {
    println(version)
}
