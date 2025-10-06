# Java 24+ Class File API Implementation

This directory contains an implementation of the module-info parser using the Java Class File API, which was finalized in Java 24 (JEP 484).

## Background

The Class File API provides a native Java API for parsing and generating class files, eliminating the need for external libraries like ASM for this purpose.

### Timeline

- **Java 22** (March 2024): Preview feature (JEP 457)
- **Java 23** (September 2024): Second Preview (JEP 466)
- **Java 24** (March 2025): Finalized (JEP 484)

## Implementation

This implementation uses:
- `java.lang.classfile.ClassFile` for parsing class files
- `java.lang.classfile.attribute.ModuleAttribute` for accessing module information
- The same `JavaModuleDescriptor` builder pattern as other implementations

## Building

When building with Java 24+, this code is automatically compiled and included in the multi-release JAR.

When building with Java 23 or earlier, this code is not compiled, and the Java 9 implementation (using `java.lang.module.ModuleDescriptor`) is used instead.

## Multi-Release JAR

This implementation is part of a multi-release JAR structure:
- Java 8: Uses ASM-based parser
- Java 9-23: Uses `java.lang.module.ModuleDescriptor`
- Java 24+: Uses Class File API (this implementation)

The appropriate version is automatically selected at runtime based on the JVM version.
