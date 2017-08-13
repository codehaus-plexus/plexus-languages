
## LocationManager.resolvePaths

In order to use this class you must setup a `ResolvePathsRequest`, which requires a list of all the jars and output directories and the main module descriptor.

You start by using `ResolvePathsRequest.withXXX`, where XXX is either Files, Paths or Strings. This way the Result will contain the same type of objects.

For the module descriptor you will either use the QDoxModuleInfoParser (in case of `module-info.java`) or AsmModuleInfoParser(in case of `module-info.class`).

The `ResolvePathsResult` contains:

 * pathElements: as a map in the same order as provided by the request. Every entry has a matching moduledescriptor when available.
 
 * classpathElements: all the pathElements which should end up on the classpath
 
 * modulepathElements: all the pathElements which should end up on the modulepath. Per entry you get the source of the modulename which is either the moduledescriptor, the manifestfile of the filename. This information can be used to warn users in case they use automatic modules, which module names are not reliable yet. 
  

