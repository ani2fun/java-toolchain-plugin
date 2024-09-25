package eu.kakde

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Locale

val CUSTOM_GROUP = "Toolchain Verification Task"

open class JavaToolchainExtension(
    // Default values for the JDK version and vendor
    var version: Int = 21,
    // "amazon" | "amazon corretto" | "adoptium" | "temurin" | "eclipse" | "eclipse temurin"
    var vendor: String = "Amazon Corretto",
)

class JavaToolChainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        val extension = project.extensions.create("javaToolchainConfig", JavaToolchainExtension::class.java)

        project.afterEvaluate {
            println("Java Toolchain Config: version=${extension.version}, vendor=${extension.vendor}")
            configureJavaToolchain(project, extension)
            configureKotlinToolchain(project, extension)
            project.tasks.register("checkMajorJavaVersionForCompiledClasses", CheckVersionForCompiledClasses::class.java)
        }
    }

    private fun configureJavaToolchain(
        project: Project,
        extension: JavaToolchainExtension,
    ) {
        val toolchainService =
            project.extensions.findByType(JavaToolchainService::class.java)
                ?: throw IllegalStateException("Java Toolchain Service is not available in this project.")

        // Apply the toolchain configuration globally
        project.extensions.configure(JavaPluginExtension::class.java) { javaPluginExtension ->
            javaPluginExtension.toolchain.languageVersion.set(JavaLanguageVersion.of(extension.version))
            javaPluginExtension.toolchain.vendor.set(getVendorSpec(extension.vendor))
        }

        // Apply the toolchain to all JavaCompile tasks
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            val toolchain =
                toolchainService
                    .compilerFor {
                        it.languageVersion.set(JavaLanguageVersion.of(extension.version))
                        it.vendor.set(getVendorSpec(extension.vendor))
                    }.orNull

            if (toolchain != null) {
                println("Setting toolchain for JavaCompile task: version=${extension.version}, vendor=${extension.vendor}")
                task.javaCompiler.set(toolchain)
            } else {
                throw IllegalStateException(
                    "Failed to resolve the Java toolchain with vendor '${extension.vendor}' and version '${extension.version}'. Ensure the correct JDK is installed.",
                )
            }
        }
    }

    private fun configureKotlinToolchain(
        project: Project,
        extension: JavaToolchainExtension,
    ) {
        val toolchainService =
            project.extensions.findByType(JavaToolchainService::class.java)
                ?: throw IllegalStateException("Java Toolchain Service is not available in this project.")

        project.tasks.withType(KotlinCompile::class.java).configureEach { task ->
            val toolchain =
                toolchainService
                    .compilerFor {
                        it.languageVersion.set(JavaLanguageVersion.of(extension.version))
                        it.vendor.set(getVendorSpec(extension.vendor))
                    }.orNull

            if (toolchain != null) {
                println("Setting toolchain for KotlinCompile task: version=${extension.version}, vendor=${extension.vendor}")
                task.kotlinOptions.jvmTarget = extension.version.toString()
                task.compilerOptions.freeCompilerArgs.addAll("-Xjsr305=strict")
            } else {
                throw IllegalStateException(
                    "Failed to resolve the Kotlin toolchain with vendor '${extension.vendor}' and version '${extension.version}'. Ensure the correct JDK is installed.",
                )
            }
        }
    }

    private fun getVendorSpec(vendor: String): JvmVendorSpec =
        when (vendor.lowercase(Locale.getDefault())) {
            "amazon", "amazon corretto" -> JvmVendorSpec.AMAZON
            "adoptium", "temurin", "eclipse", "eclipse temurin" -> JvmVendorSpec.ADOPTIUM
            else -> JvmVendorSpec.matching(vendor)
        }
}
