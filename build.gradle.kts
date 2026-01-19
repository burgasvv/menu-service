
@file:Suppress("VulnerableLibrariesLocal")

val exposedVersion: String = "1.0.0-rc-4"
val kotlinVersion: String = "2.3.0"
val logbackVersion: String = "1.5.18"
val postgresVersion: String = "42.7.2"
val jbcryptVersion: String = "0.4"

plugins {
    kotlin("jvm") version "2.2.21"
    id("io.ktor.plugin") version "3.3.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
}

group = "org.burgas"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-host-common")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-jdbc:${exposedVersion}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${exposedVersion}")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("io.ktor:ktor-server-netty")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-serialization-gson:3.3.2")
    implementation("io.ktor:ktor-server-auth:3.3.2")
    implementation("io.ktor:ktor-server-auth:3.3.2")
    implementation("org.mindrot:jbcrypt:${jbcryptVersion}")
    implementation("io.ktor:ktor-server-sessions:3.3.2")
    implementation("io.ktor:ktor-server-csrf:3.3.2")
    implementation("io.ktor:ktor-server-cors:3.3.2")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
