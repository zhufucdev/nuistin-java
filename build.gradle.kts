plugins {
    id("java")
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
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType(Jar::class) {
    manifest {
        attributes("Main-Class" to "com.javamasters.Main")
    }
}
