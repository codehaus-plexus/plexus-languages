package org.codehaus.plexus.languages.java.jpms;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Extract information from the module-info file
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public interface ModuleInfoParser
{
    
    
    /**
     * Extracts the name from the module-info file
     * 
     * @param modulePath
     * @return
     * @throws IOException
     */
    JavaModuleDescriptor getModuleDescriptor( Path modulePath )
        throws IOException;
}