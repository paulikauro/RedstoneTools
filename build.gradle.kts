import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "io.github.paulikauro.redstonetools"
version = "1.4.2-SNAPSHOT"

plugins {
    val kotlinVersion = "2.1.21"
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "8.3.6"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
}

repositories {
    mavenCentral()
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("com.google.re2j:re2j:1.6")
    implementation("net.kyori:adventure-extra-kotlin:4.16.0")

    compileOnly("de.tr7zw:item-nbt-api-plugin:2.14.1")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.0-SNAPSHOT")
}

bukkit {
    main = "io.github.paulikauro.redstonetools.RedstoneTools"
    apiVersion = "1.20"
    depend = listOf("WorldEdit", "NBTAPI")
}

tasks.shadowJar {
    relocate("co.aikar.commands", "io.github.paulikauro.redstonetools.acf.commands")
    relocate("co.aikar.locales", "io.github.paulikauro.redstonetools.acf.locales")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        javaParameters = true
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.register("getVersion") {
    println(version)
}
