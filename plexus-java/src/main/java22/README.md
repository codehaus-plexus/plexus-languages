# Java 22+ Class File API Implementation

This directory contains an implementation of the module-info parser using the Java Class File API, which was introduced as a preview feature in Java 22 (JEP 457).

## Background

The Class File API provides a native Java API for parsing and generating class files, eliminating the need for external libraries like ASM for this purpose.

### Timeline

- **Java 22** (March 2024): Preview feature (JEP 457) - requires `--enable-preview`
- **Java 23** (September 2024): Second Preview (JEP 466) - requires `--enable-preview`
- **Java 24** (March 2025): Expected to be finalized (JEP 484) - no preview flag needed

## Implementation

This implementation uses:
- `java.lang.classfile.ClassFile` for parsing class files
- `java.lang.classfile.attribute.ModuleAttribute` for accessing module information
- The same `JavaModuleDescriptor` builder pattern as other implementations

## Building

When building with Java 22 or 23, the `--enable-preview` flag is automatically added by the Maven compiler plugin configuration.

When building with Java 24+, the preview flag should not be needed as the API should be finalized.

When building with Java 17 or earlier, this code is not compiled, and the Java 9 implementation (using `java.lang.module.ModuleDescriptor`) is used instead.

## Multi-Release JAR

This implementation is part of a multi-release JAR structure:
- Java 8: Uses ASM-based parser
- Java 9-21: Uses `java.lang.module.ModuleDescriptor`
- Java 22+: Uses Class File API (this implementation)

The appropriate version is automatically selected at runtime based on the JVM version.
