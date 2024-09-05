# Gradle plugin for managing java tool chain across microservices

Follow these steps to integrate and configure the plugin for your project:

## 1. Apply the Plugin

**Apply the plugin in your `build.gradle.kts` file:**

```
plugins {
    id("eu.kakde.gradle.java-toolchain-plugin") version "1.0.0"
}
```

- **And add the extension:**

```
javaToolchainConfig {
    version = 22
    vendor = "adoptium"
}
```

- If **javaToolchainConfig** is not mentioned then automatic java version 21 with amazon will be taken as a default value.

- This value is hard coded in "kotlin/eu/kakde/JavaToolChainPlugin.kt" in class called "JavaToolchainExtension".

## 2. Publish to Local:

You can test the plugin locally by publishing it and then importing it into the corresponding project. Run the following
command to publish the plugin to your local Maven repository:

```bash
./gradlew :plugin:publishToMavenLocal
```

This command will publish the plugin to your local Maven repository, making it available for use in other projects on
your local environment using the same way as in `Apply the Plugin` section.

## 3. Run VerifyJDK task "verifyJdk"

Let's say you have applied this plugin in your project and want to run the task,

```bash
./gradlew :<PROJECT-NAME>:verifyJdk
```
