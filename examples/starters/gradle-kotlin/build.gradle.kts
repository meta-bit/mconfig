plugins {
    java
    application
}

group = "com.example.starter"
version = "1.0-SNAPSHOT"

val mconfigVersion by extra("0.8.1")

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("org.metabit.platform.support.config:mConfigStandard:$mconfigVersion")
    implementation("org.metabit.platform.support.config:mConfigLoggingSlf4j:$mconfigVersion")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.example.starter.Main")
}

tasks.run {
    standardInput = System.in
}