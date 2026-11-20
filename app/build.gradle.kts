plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":test-infra"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

tasks.test {
    useJUnitPlatform()
    // strict ordering, one JVM process for the whole run
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
    maxHeapSize = "1g"
}
