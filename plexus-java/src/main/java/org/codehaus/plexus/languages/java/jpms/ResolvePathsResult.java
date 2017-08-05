package org.codehaus.plexus.languages.java.jpms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the results of the project analyzer
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class ResolvePathsResult<T>
{
    /**
     * Source of the modulename 
     */
    public enum ModuleNameSource
    {
        FILENAME, MANIFEST, MODULEDESCRIPTOR
    }
    
    private JavaModuleDescriptor mainModuleDescriptor;
    
    /**
     * Ordered map, respects the classpath order
     */
    private Map<T, JavaModuleDescriptor> pathElements;
    
    private Map<T, ModuleNameSource> modulepathElements = new HashMap<>();
    
    private Collection<T> classpathElements = new ArrayList<>();
    
    void setMainModuleDescriptor( JavaModuleDescriptor mainModuleDescriptor )
    {
        this.mainModuleDescriptor = mainModuleDescriptor;
    }

    public JavaModuleDescriptor getMainModuleDescriptor()
    {
        return mainModuleDescriptor;
    }

    void setPathElements( Map<T, JavaModuleDescriptor> pathElements )
    {
        this.pathElements = pathElements;
    }
    
    /**
     * Ordered map, respects the classpath order
     */
    public Map<T, JavaModuleDescriptor> getPathElements()
    {
        return pathElements;
    }
    
    void setClasspathElements( Collection<T> classpathElements )
    {
        this.classpathElements = classpathElements;
    }
    
    public Collection<T> getClasspathElements()
    {
        return classpathElements;
    }
    
    void setModulepathElements( Map<T, ModuleNameSource> modulepathElements )
    {
        this.modulepathElements = modulepathElements;
    }
    
    public Map<T, ModuleNameSource> getModulepathElements()
    {
        return modulepathElements;
    }
}
