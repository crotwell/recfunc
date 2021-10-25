/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/6.7/userguide/building_java_projects.html
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    eclipse
    id("project-report")
}

version = "0.1-20211022a"

repositories {
    mavenLocal()
    mavenCentral()
    
    maven(url = "https://www.seis.sc.edu/software/maven2")
}

dependencies {
    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")

    // Use JUnit Jupiter Engine for testing.
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // This dependency is used by the application.
    implementation("edu.sc.seis:seedCodec:1.1.1")
    implementation("edu.sc.seis:seisFile:2.0.0")
    implementation("edu.sc.seis:TauP:2.5.0")
    implementation("edu.sc.seis:sod:4.0.0-SNAPSHOT-WEBSTATUS")
    implementation("edu.sc.seis:sod-bag:4.0.0-SNAPSHOT")
    implementation("com.oregondsp.signalprocessing:oregondsp:1.0.1-alpha")
    implementation("org.json:json:20170516")
    implementation("com.martiansoftware:jsap:2.1")
    implementation( "org.slf4j:slf4j-api:1.7.30")
    implementation( "org.slf4j:slf4j-log4j12:1.7.30")
}

configurations.all {
    resolutionStrategy.dependencySubstitution {
        substitute(module("edu.sc.seis:sod-bag")).with(project(":sod-bag"))
        substitute(module("edu.sc.seis:seisFile")).with(project(":seisFile"))
        substitute(module("edu.sc.seis:seedCodec")).with(project(":seedCodec"))
        substitute(module("edu.sc.seis:TauP")).with(project(":TauP"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    // Define the main class for the application.
    mainClass.set("HKStack.App")
}

tasks.test {
    // Use junit platform for unit tests.
    useJUnitPlatform()
}