import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.github.paulikauro.redstonetools"
version = "1.5.1"

plugins {
    val kotlinVersion = "2.4.21"
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "9.6.1"
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
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
    implementation("net.kyori:adventure-extra-kotlin:4.26.1")

    compileOnly("de.tr7zw:item-nbt-api-plugin:2.16.1")
    compileOnly("io.papermc.paper:paper-api:26.3.build.206-stable")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.4.6")
}

bukkit {
    main = "io.github.paulikauro.redstonetools.RedstoneTools"
    apiVersion = "1.21"
    depend = listOf("WorldEdit", "NBTAPI")
}

kotlin {
    jvmToolchain(25)
}

tasks.shadowJar {
    relocate("co.aikar.commands", "io.github.paulikauro.redstonetools.acf.commands")
    relocate("co.aikar.locales", "io.github.paulikauro.redstonetools.acf.locales")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        javaParameters = true
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("getVersion") {
    println(version)
}
