The plexus-java library is created to have a solution for common activities, so this business logic doesn't have to be maintained at multiple places. The first provided feature was the `LocationManager` to analyze module desciptors and to decide which jars should end up on the modulepath and which on the classpath. The name was based on the [javax.tools.JavaFileManager.Location]. (https://docs.oracle.com/javase/10/docs/api/javax/tools/JavaFileManager.Location.html)

The library requires Java 8 to run, but contains optimized code for Java 9. By requiring Java 8 it was much easier to embed this library in several other projects.

This jar is a multi release jar (aka MRJAR), because it contains 2 implementations for the `BinaryModuleInfoParser`. If the Java runtime is 9 or above, the `java.lang.module.ModuleDescriptor` is used to read the `module-info.class`. If the runtime is Java 8, then ASM is used to read the module descriptor.

When extracting the the automatic module name based the of the file, it is a little bit more complex. The result must be precise, so the only way to solve this is by calling Java 9 code, either from the runtime or by calling Java 9 explicitly when provided via `ResolvePathsRequest.setJdkHome`.

# Request

The `LocationManager.resolvePaths()` only has one argument, `ResolvePathsRequest`. If there is more data required, the request will be extended so the method signature of `resolvePaths` will stay the same.

There are 3 ways to create a `ResolvePathsRequest`:

- `ResolvePathsRequest.ofFiles(Collection<File>)`

- `ResolvePathsRequest.ofPaths(Collection<Path>)`

- `ResolvePathsRequest.ofStrings(Collection<String>)`

As argument you pass all the archives and/or outputDirectories specified to build the project.

Additional methods are:

- `setAdditionalModules`, in case the consumer wants to use `--add-modules`

- `setIncludeAllProviders`, in general would only be used at runtime, not during compile or test. In case `uses` is used, all modules with matching `provides` are added as well.

- `setJdkHome`, should point to Java 9 or above in case the runtime of this library is Java 8

- `setMainModuleDescriptor`, which can either be a `module-info.java` or `module-info.class`

# Phase 1: Collect

If there's a `mainModuleDescriptor`, extract a `JavaModuleDescriptor` of it. This might cause a `IOException` to be thrown.

All pathElements of `ResolvePathsRequest.ofT` are transformed to Path instances. For every element the name will be resolved in the following order:

1. Module descriptor: verify if the jar or the directory contains `module-info.class`. If so, the its descriptor is transformed to a `JavaModuleDescriptor`, where its ModuleNameSource is marked as `ModuleNameSource.MODULEDESCRIPTOR`

2. Manifest: verify if the jar or directory has a `META-INF/MANIFEST.MF` and if it contains the `Automatic-Module-Name` attribute. If so, an automatic `JavaModuleDescriptor` is created, where its ModuleNameSource is marked as `ModuleNameSource.MANIFEST`.

3. Filename: try to extract the module name based on the filename. If the filename could be transformed to a module name (which is not always the case), an automatic `JavaModuleDescriptor` is created, where its ModuleNameSource is marked as `ModuleNameSource.FILENAME`.

When there's an `IOException` with one of the pathElements, the exception is stored in the `ResolvePathsResult.pathExceptions` so the consumer can handle them separately.

The result are a couple of Maps:

* module name to `ModuleNameSource`

* module name to `JavaModuleDescriptor`

# Phase 2: Resolve

If there's a `mainModuleDescriptor`, collect all its direct and indirect requirements.
This contains recursive code and ensures that required modules are only evaluated once.
All these pathElements must be placed on the modulepath, all other pathElements will be marked for the classPath.

# Result

All results will be stored in a `ResolvePathsResult`.

- `getClasspathElements()`, ordered collection of all pathElements that don't belong to the modulepath

- `getMainModuleDescriptor()`, an `JavaModuleDescriptor` instance based on the provided mainModuleDescriptor file

- `getModulepathElements()`, ordered map of the pathElements with their source

- `getPathElements()`, ordered map of the pathElements with their module descriptor

- `getPathExceptions()`, map of pathElements containing only the elements that faced an exception.

