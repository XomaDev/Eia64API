plugins {
    id("java")
    kotlin("jvm")
}

group = "space.themelon.eia64"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.1.0")
    implementation(files("eialib/Eia64.jar"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes("Main-Class" to "space.themelon.eia64.Initiator")
    }
    from({
        configurations.compileClasspath.get().filter {
            it.exists()
        }.map {
            if (it.isDirectory) it else project.zipTree(it)
        }
    })
    with(tasks.jar.get())
    duplicatesStrategy = DuplicatesStrategy.WARN
}