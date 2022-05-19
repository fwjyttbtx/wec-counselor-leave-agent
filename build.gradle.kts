import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val coroutines_version = "1.6.1"
val ktor_version = "2.0.1"

plugins {
    id("org.springframework.boot") version "2.6.7"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.spring") version "1.6.21"
    distribution
}

group = "com.wisedu.wec"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutines_version")

    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-client-auth:$ktor_version")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

distributions {
    main {
        contents {

            from("build/libs") {
                exclude("*-plain.jar")
            }
            from("src/main/bin") {
                fileMode = 755
            }
            from("README.md")
            into("/leave-agent")

            from("/src/main/resources") {
                into("/leave-agent/conf")
            }

        }
    }
}

tasks.withType<Tar> {
    dependsOn("bootJar")
    compression = Compression.GZIP
    archiveExtension.set("tar.gz")
    archiveFileName.set("${project.name}.tar.gz")
}