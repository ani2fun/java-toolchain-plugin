package eu.kakde

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.util.Locale

val CUSTOM_GROUP = "Toolchain Verification Task"

open class JavaToolchainExtension {
    // Default values for the JDK version and vendor
    var version: Int = 21

    // "amazon" | "amazon corretto" | "adoptium" | "temurin" | "eclipse" | "eclipse temurin"
    var vendor: String = "Amazon Corretto"
}

class JavaToolChainPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Apply the Java plugin, which is necessary for using Java toolchain API
        project.pluginManager.apply(JavaLibraryPlugin::class.java)

        // Create the extension for configuring the JDK version and vendor
        val extension = project.extensions.create("javaToolchainConfig", JavaToolchainExtension::class.java)

        // Apply the toolchain configuration automatically across the entire project
        project.afterEvaluate {
            configureJavaToolchain(project, extension)
            project.tasks.register("verifyJdk", VerifyJDKTask::class.java)
        }
    }

    private fun configureJavaToolchain(
        project: Project,
        extension: JavaToolchainExtension,
    ) {
        // Get Java Toolchain Service
        val toolchainService =
            project.extensions.findByType(JavaToolchainService::class.java)
                ?: throw IllegalStateException("Java Toolchain Service is not available in this project.")

        // Automatically apply the toolchain configuration to all JavaCompile tasks and globally for the project
        project.tasks.withType(JavaCompile::class.java).configureEach { task ->
            task.javaCompiler.set(
                toolchainService.compilerFor {
                    it.languageVersion.set(JavaLanguageVersion.of(extension.version))
                    it.vendor.set(getVendorSpec(extension.vendor))
                },
            )
        }

        // Additionally, apply the toolchain configuration globally for the entire project
        project.extensions.configure(JavaPluginExtension::class.java) { javaPluginExtension ->
            javaPluginExtension.toolchain.languageVersion.set(JavaLanguageVersion.of(extension.version))
            javaPluginExtension.toolchain.vendor.set(getVendorSpec(extension.vendor))
        }
    }

    private fun getVendorSpec(vendor: String): JvmVendorSpec =
        when (vendor.lowercase(Locale.getDefault())) {
            "amazon", "amazon corretto" -> JvmVendorSpec.AMAZON
            "adoptium", "temurin", "eclipse", "eclipse temurin" -> JvmVendorSpec.ADOPTIUM
            else -> throw IllegalArgumentException("Unsupported vendor: $vendor")
        }
}
