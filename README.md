# Plexus Language

![Build Status](https://github.com/codehaus-plexus/plexus-languages/workflows/GitHub%20CI/badge.svg)
![GitHub Workflow Status](https://img.shields.io/github/actions/workflow/status/codehaus-plexus/plexus-languages/maven.yml?branch=master)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/codehaus/plexus/plexus-languages/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/codehaus/plexus/plexus-languages/README.md)

Plexus Languages:

* [![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-languages.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.plexus/plexus-languages)

Plexus Java:

* [![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-java.svg?label=Maven%20Central)](https://search.maven.org/artifact/org.codehaus.plexus/plexus-java)

## Module Parsing Implementations

Plexus Java uses a multi-release JAR to provide optimal module-info parsing for different Java versions:

- **Java 8**: ASM-based parser for module-info.class files
- **Java 9-23**: Native `java.lang.module.ModuleDescriptor` API
- **Java 24+**: Java Class File API (JEP 484)

The appropriate implementation is automatically selected based on the runtime JVM version.

