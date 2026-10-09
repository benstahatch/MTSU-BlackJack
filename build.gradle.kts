plugins {
    java
    application

    kotlin("jvm") version "2.4.20"

    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}

javafx {
    version = "25"
    modules("javafx.controls")
}

application {
    mainClass.set("com.mtsu.table21.ui.MainAppKt")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}


tasks.register<JavaExec>("runServer") {
    group = "application"
    description = "Runs the Table21 casino server"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.mtsu.table21.server.CasinoServerKt")
}
