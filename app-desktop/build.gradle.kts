plugins {
    kotlin("jvm")
    application
}

group = "ru.queuejw.space"
version = "0.0.1-desktop"


dependencies {
    // Kotlin stdlib
    implementation(libs.kotlin.stdlib.jdk7)
}

application {
    mainClass = "ru.queuejw.space.desktop.MainKt"
}

// Simple uber JAR task for JVM app
tasks.register<Jar>("packageUberJarForCurrentOS") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "ru.queuejw.space.desktop.MainKt"
    }

    from(sourceSets.main.get().output)

    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}
