plugins {
    kotlin("multiplatform") version "1.4.30"
}

repositories {
    mavenLocal()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val osName = System.getProperty("os.name")
val targetOs = when {
    osName == "Mac OS X" -> "macos"
    osName.startsWith("Win") -> "windows"
    osName.startsWith("Linux") -> "linux"
    else -> error("Unsupported OS: $osName")
}

val osArch = System.getProperty("os.arch")
var targetArch = when (osArch) {
    "x86_64", "amd64" -> "x64"
    "aarch64" -> "arm64"
    else -> error("Unsupported arch: $osArch")
}

val target = "${targetOs}-${targetArch}"

var version = "0.0.0-SNAPSHOT"
if (project.hasProperty("skiko.version")) {
    version = project.properties["skiko.version"] as String
}

kotlin {

    val nativeTarget = when {
        osName == "Mac OS X" -> macosX64()
        osName.startsWith("Win") -> mingwX64()
        osName.startsWith("Linux") -> linuxX64()
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            executable {
                entryPoint = "org.jetbrains.skiko.sample.native.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
            }
        }

        val macosX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation("org.jetbrains.skiko:skiko-native-runtime-$target:$version")
                implementation("org.jetbrains.skiko:skiko-native-skia-interop-$target:$version")

            }
        }
    }
}

/*
val additionalArguments = mutableMapOf<String, String>()

val casualRun = tasks.named<JavaExec>("run") {
    systemProperty("skiko.fps.enabled", "true")
    System.getProperties().entries
        .associate {
            (it.key as String) to (it.value as String)
        }
        .filterKeys { it.startsWith("skiko.") }
        .forEach { systemProperty(it.key, it.value) }
    additionalArguments.forEach { systemProperty(it.key, it.value) }
}

tasks.register("runSoftware") {
    additionalArguments += mapOf("skiko.renderApi" to "SOFTWARE")
    dependsOn(casualRun)
}

tasks.withType<Test> {
    systemProperty("skiko.test.screenshots.dir", File(project.projectDir, "src/test/screenshots").absolutePath)

    // Tests should be deterministic, so disable scaling.
    // On MacOs we need the actual scale, otherwise we will have aliased screenshots because of scaling.
    if (System.getProperty("os.name") != "Mac OS X") {
        systemProperty("sun.java2d.dpiaware", "false")
        systemProperty("sun.java2d.uiScale", "1")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
*/