# Plexus Languages

[![Maven Central](https://img.shields.io/maven-central/v/org.codehaus.plexus/plexus-java.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.codehaus.plexus/plexus-java)
[![GitHub CI](https://github.com/codehaus-plexus/plexus-languages/workflows/GitHub%20CI/badge.svg)](https://github.com/codehaus-plexus/plexus-languages/actions)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/codehaus/plexus/plexus-languages/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/codehaus/plexus/plexus-languages/README.md)

Shared language-oriented features for build tools. Currently one module, **`plexus-java`**, which does the
Java Platform Module System work that Maven plugins need:

- reads a module descriptor — from `module-info.java`, `module-info.class`, an `Automatic-Module-Name`
  manifest entry, or a filename — and tells you the module name and where it came from
- splits a set of paths into a **classpath** and a **module path**, which is what
  `maven-compiler-plugin`, `maven-surefire-plugin` and `maven-javadoc-plugin` need to invoke a modular
  build correctly

The entry point is
[`LocationManager`](https://codehaus-plexus.github.io/plexus-languages/plexus-java/locationmanager.html).

## Status

Maintained. Several core Maven plugins depend on it, so public API is kept compatible.

## Using it

```xml
<dependency>
  <groupId>org.codehaus.plexus</groupId>
  <artifactId>plexus-java</artifactId>
</dependency>
```

Check the badge above for the current version. Note the artifact is `plexus-java`; `plexus-languages` is
the aggregator.

## Requirements

Java 8 or later to run. Reading modular jars obviously requires a JDK that has modules.

## Documentation

- [Project site](https://codehaus-plexus.github.io/plexus-languages/) — including [usage](https://codehaus-plexus.github.io/plexus-languages/plexus-java/usage.html) and [LocationManager](https://codehaus-plexus.github.io/plexus-languages/plexus-java/locationmanager.html)
- [Javadoc](https://javadoc.io/doc/org.codehaus.plexus/plexus-java)
- [Release notes](https://github.com/codehaus-plexus/plexus-languages/releases)

## Contributing

See [CONTRIBUTING.md](https://github.com/codehaus-plexus/.github/blob/master/CONTRIBUTING.md). In short:
`mvn verify` builds, and run `mvn spotless:apply` before pushing or CI will fail on formatting.

Please report security vulnerabilities privately — see
[SECURITY.md](https://github.com/codehaus-plexus/.github/blob/master/SECURITY.md), not a public issue.
