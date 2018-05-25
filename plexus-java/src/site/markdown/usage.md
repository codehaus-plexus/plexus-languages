
## LocationManager.resolvePaths

In order to use this class you must setup a `ResolvePathsRequest`, which requires a list of all the jars and output directories and the main module descriptor.

You start by using `ResolvePathsRequest.ofXXX`, where XXX is either Files, Paths or Strings. This way the Result will contain the same type of objects.

The `ResolvePathRequest` also contains:

  * mainModuleDescriptor: the path or file of the main module descriptor, can either be `mdouel-info.java` or `module-info.class`  
  
  * additionalModules: the modules that will be addedusing `-add-modules`
  
  * jdkHome: in case you need to use a different JDK to extract the name from the modules. Can be interesting if the runtime is still Java 7.   

The `ResolvePathsResult` contains:

 * mainModuleDescriptor: the module descriptor of the passed descriptor file. 
 
 * pathElements: as a map in the same order as provided by the request. Every entry has a matching moduledescriptor when available.
 
 * classpathElements: all the pathElements which should end up on the classpath.
 
 * modulepathElements: all the pathElements which should end up on the modulepath. Per entry you get the source of the modulename which is either the moduledescriptor, the manifestfile of the filename. This information can be used to warn users in case they use automatic modules, which module names are not reliable yet. 
  
 * pathExceptions: pathElements with their exception while trying to resolve it. Only pathElements with an exception are listed.
 
## JavaVersion

This is a String based, lazy-parsing implementation of a Java Version which can be used to compare versions. It's goal is to support to support the following patterns:

 * [Java SE Naming and Versions](http://www.oracle.com/technetwork/java/javase/namechange-140185.html)
 * [JEP 223: New Version-String Scheme](http://openjdk.java.net/jeps/223)
 * [JEP 322: Time-Based Release Versioning](http://openjdk.java.net/jeps/322) 

Additional features:

 * `JavaVersion.JAVA_SPECIFICATION_VERSION` represents `System.getProperty( "java.specification.version" )`
  
 * `JavaVersion.JAVA_VERSION` represents `System.getProperty( "java.version" )`