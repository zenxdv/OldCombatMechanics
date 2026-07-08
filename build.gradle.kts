plugins {
    java
}

group = "net.mosspad"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

val spigotApiVersion = "26.1-R0.1-SNAPSHOT"

dependencies {
    // Compile against the earliest supported 26.1 API line. The plugin only uses
    // public Bukkit/Spigot APIs that are also present on Paper.
    compileOnly("org.spigotmc:spigot-api:$spigotApiVersion")

    testImplementation("org.spigotmc:spigot-api:$spigotApiVersion")
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Gradle 9 no longer adds the JUnit Platform launcher automatically.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

// plugin.yml intentionally contains the release version as literal text. This
// avoids task-time Groovy expansion, which Gradle 9 configuration cache rejects.
tasks.jar {
    archiveBaseName.set("OldCombatMechanics")
}
