import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.github.paulikauro.redstonetools"
version = "1.5.2-SNAPSHOT"

plugins {
    val kotlinVersion = "2.4.0"
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "9.4.2"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.google.re2j:re2j:1.8")
    implementation("net.kyori:adventure-extra-kotlin:4.16.0")

    compileOnly("de.tr7zw:item-nbt-api-plugin:2.15.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.11-SNAPSHOT")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

bukkit {
    main = "io.github.paulikauro.redstonetools.RedstoneTools"
    apiVersion = "1.21"
    depend = listOf("WorldEdit", "NBTAPI")
}

tasks.shadowJar {
    relocate("co.aikar.commands", "io.github.paulikauro.redstonetools.acf.commands")
    relocate("co.aikar.locales", "io.github.paulikauro.redstonetools.acf.locales")
}

java {
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters = true
    }
}

tasks.withType<Test> {
    failOnNoDiscoveredTests = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("getVersion") {
    println(version)
}
