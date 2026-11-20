plugins {
    kotlin("jvm")
}

val junit = "5.14.4"

dependencies {
    api("org.junit.jupiter:junit-jupiter-api:$junit")
    api("org.junit.jupiter:junit-jupiter-params:$junit")
    api("org.springframework:spring-test:6.2.11")
    api("org.springframework:spring-context:6.2.11")
}
