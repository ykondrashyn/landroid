plugins {
    kotlin("jvm")
}

group = "ru.queuejw.space"
version = "0.0.1-core"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk7)
    testImplementation(kotlin("test"))
}

