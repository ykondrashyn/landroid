plugins {
    kotlin("jvm")
    application
}

group = "ru.queuejw.space"
version = "0.0.1-mcp"

dependencies {
    implementation(project(":core-sim"))
    implementation(libs.kotlin.stdlib.jdk7)
}

application {
    mainClass = "ru.queuejw.space.mcp.MainKt"
}

