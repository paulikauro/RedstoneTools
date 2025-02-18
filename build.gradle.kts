import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = ""
version = "1.4"

plugins {
    val kotlinVersion = "2.1.10"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.3.0"
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
    implementation("co.aikar:acf-paper:0.5.0-SNAPSHOT")
    implementation("com.google.re2j:re2j:1.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.0")
    implementation("net.kyori:adventure-api:4.11.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.11.0")

    compileOnly("de.tr7zw:item-nbt-api-plugin:2.8.0")
    compileOnly("org.spigotmc:spigot-api:1.17-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

bukkit {
    main = "redstonetools.RedstoneTools"
    apiVersion = "1.17"
    depend = listOf("WorldEdit", "NBTAPI")
}

tasks.test {
    useJUnitPlatform()
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
