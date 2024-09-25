package eu.kakde

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import java.io.File
import java.io.FileInputStream

abstract class VerifyJdkForCompiledClasses : DefaultTask() {
    // List of allowed Java major versions and vendors
    // Java 17 -> 61, Java 21 -> 65
    private val allowedVersions = listOf(61, 65)
    private val allowedVendors = listOf("amazon corretto", "adoptium", "temurin", "eclipse", "eclipse temurin")

    init {
        group = CUSTOM_GROUP
        description = "Verifies the Java toolchain configuration and compiled class files."
    }

    @TaskAction
    fun verifyJDK() {
        // Step 1: Verify the Java Toolchain
        val actualToolchain = getActualToolchainMetadata()
        displayToolchainInfo(actualToolchain)

        // Step 2: Print class directories for verification
        val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
        val classDirs = sourceSets.flatMap { it.output.classesDirs.files }
        println("Class directories being checked: ")
        classDirs.forEach { println(it.absolutePath) }

        // Step 3: Verify the compiled class files
        verifyCompiledClasses(actualToolchain, classDirs)
    }

    private fun getActualToolchainMetadata(): ToolchainMetadata {
        val toolchainService =
            project.extensions.findByType(JavaToolchainService::class.java)
                ?: throw IllegalStateException("Java Toolchain Service is not available in this project.")

        val javaLauncher =
            toolchainService
                .launcherFor {
                    it.languageVersion.set(JavaLanguageVersion.of(getConfiguredJavaVersion()))
                    it.vendor.set(JvmVendorSpec.matching(getConfiguredJavaVendor()))
                }.get()

        return ToolchainMetadata(
            vendor = javaLauncher.metadata.vendor.toString(),
            version = javaLauncher.metadata.languageVersion.asInt(),
        )
    }

    private fun getConfiguredJavaVersion(): Int = project.extensions.getByType(JavaToolchainExtension::class.java).version

    private fun getConfiguredJavaVendor(): String = project.extensions.getByType(JavaToolchainExtension::class.java).vendor

    private fun displayToolchainInfo(toolchain: ToolchainMetadata) {
        println("Vendor used for compiling the code: ${toolchain.vendor}")
        println("Java version used for compiling the code: ${toolchain.version}")
    }

    private fun verifyCompiledClasses(
        actualToolchain: ToolchainMetadata,
        classDirs: Collection<File>,
    ) {
        // Recursively find all .class files in the provided directories
        val classFiles = classDirs.flatMap { findAllClassFiles(it) }

        var totalFilesChecked = 0
        val invalidClassFiles = mutableListOf<String>()

        classFiles.forEach { classFile ->
            totalFilesChecked++
            val classFileVersion = getClassFileVersion(classFile)

            // Check if the class file uses allowed Java versions and vendors
            if (!isVersionAllowed(classFileVersion) || !isVendorAllowed(actualToolchain.vendor)) {
                invalidClassFiles.add(classFile.name)
            }
        }

        // Print a summary
        printSummary(totalFilesChecked, invalidClassFiles)
    }

    private fun isVersionAllowed(version: Int): Boolean = allowedVersions.contains(version)

    private fun isVendorAllowed(vendor: String): Boolean = allowedVendors.any { vendor.contains(it, ignoreCase = true) }

    private fun getClassFileVersion(classFile: File): Int {
        FileInputStream(classFile).use { input ->
            val bytes = ByteArray(8)
            input.read(bytes)
            return ((bytes[6].toInt() and 0xFF) shl 8) + (bytes[7].toInt() and 0xFF)
        }
    }

    private fun findAllClassFiles(directory: File): List<File> {
        // Recursively find all class files in subdirectories
        return if (directory.exists()) {
            directory
                .walkTopDown()
                .filter { it.extension == "class" }
                .toList()
        } else {
            emptyList()
        }
    }

    private fun printSummary(
        totalFilesChecked: Int,
        invalidClassFiles: List<String>,
    ) {
        println("Total class files checked: $totalFilesChecked")

        if (invalidClassFiles.isEmpty()) {
            println("All class files are compiled with the correct Java version and vendor.")
        } else {
            println("Allowed Java Versions: ${allowedVersions.joinToString(", ")}")
            println("Allowed Vendors: ${allowedVendors.joinToString(", ")}")
            println("Total Class files compiled with incorrect Java Version/Vendor: ${invalidClassFiles.size}")
            // Optionally limit the output to a maximum number of invalid files
            invalidClassFiles.take(10).forEach { println("- $it") }
            if (invalidClassFiles.size > 10) {
                println("...and ${invalidClassFiles.size - 10} more.")
            }
        }
    }

    private data class ToolchainMetadata(
        val vendor: String,
        val version: Int,
    )
}
