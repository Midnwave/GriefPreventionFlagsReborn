plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta12"
}

group = "com.blockforge"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress:GriefPrevention:16.18.4")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.jetbrains:annotations:26.0.1")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    // Paper provides adventure natively — use compileOnly to avoid bundling
    // old versions in the shadow JAR which would shadow Paper's newer classes
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-api:4.17.0")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.bstats", "com.blockforge.griefpreventionflagsreborn.libs.bstats")
        // net.kyori is NOT relocated — Paper provides it natively and relocating it
        // causes NoSuchMethodError when passing shaded Components to Paper's API.
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
