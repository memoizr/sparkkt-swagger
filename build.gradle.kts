import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.10"
    maven
}

group = "spark-sagger"
version = "1.0"

repositories {
    mavenCentral()
    jcenter()
    maven("https://packagecloud.io/manusant/beerRepo/maven2")
    maven("https://jitpack.io")
    maven("https://jcenter.bintray.com")
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.10")
    implementation("ch.qos.logback:logback-classic:1.1.7")
    implementation("com.sparkjava:spark-core:2.9.3")
    implementation("com.beust:klaxon:5.4")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.4.10")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation("khttp:khttp:1.0.0")

    testImplementation("junit:junit:4.12")
    testImplementation("com.github.memoizr:assertk-core:1.0.0-beta.2")
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    withSourcesJar()
    withJavadocJar()
}
