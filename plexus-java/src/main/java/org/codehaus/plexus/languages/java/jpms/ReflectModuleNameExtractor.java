package org.codehaus.plexus.languages.java.jpms;

import java.io.File;
import java.io.IOException;

/**
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public class ReflectModuleNameExtractor implements ModuleNameExtractor
{

    @Override
    public String extract( File modulePath )
        throws IOException
    {
        return MainClassModuleNameExtractor.getModuleName( modulePath.toPath() );
    }

}
