import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

group = ""
version = "1.2"

plugins {
    val kotlinVersion = "1.4.21"
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "2.0.4"
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
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(group = "co.aikar", name = "acf-paper", version = "0.5.0-SNAPSHOT")
    implementation(group = "com.google.re2j", name = "re2j", version = "1.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.8")

    compileOnly(group = "de.tr7zw", name = "item-nbt-api-plugin", version = "2.5.0")
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.15-R0.1-SNAPSHOT")
    compileOnly(group = "com.sk89q.worldedit", name = "worldedit-bukkit", version = "7.2.0-SNAPSHOT")
    compileOnly(group = "com.comphenix.protocol", name = "ProtocolLib", version = "4.5.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
}

bukkit {
    main = "redstonetools.RedstoneTools"
    apiVersion = "1.15"
    depend = listOf("WorldEdit")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    relocate("co.aikar.commands", "redstonetools.acf")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        javaParameters = true
        freeCompilerArgs = listOf(
            "-Xopt-in=kotlin.ExperimentalStdlibApi",
            "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("getVersion") {
    println(version)
}

