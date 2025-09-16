import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
    maven("https://jitpack.io") // Додано для kotlin-onetimepassword
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("com.google.code.gson:gson:2.10.1")
    // Crypto - використовуємо kotlin-crypto замість turingcomplete
//    implementation("org.kotlincrypto:core:0.3.0")
//    implementation("org.kotlincrypto:hashes:0.3.0")
//    implementation("org.kotlincrypto:signatures:0.3.0")
//    implementation("org.kotlincrypto.signature:signature-ed25519:0.3.0")

    // TOTP генератор (альтернатива)
    implementation("com.eatthepath:java-otp:0.4.0")

    // JSON
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("org.json:json:20230227")
    // BouncyCastle
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("com.goterl:lazysodium-java:5.1.1")
    implementation("net.java.dev.jna:jna:5.13.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "HearProject"
            packageVersion = "1.0.0"
        }
    }
}