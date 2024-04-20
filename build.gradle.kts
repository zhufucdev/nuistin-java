plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.javamasters"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.javakeyring:java-keyring:1.0.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes("Main-Class" to "com.javamasters.Main")
    }
}
