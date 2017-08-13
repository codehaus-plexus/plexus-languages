package org.codehaus.plexus.languages.java.jpms;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

public class ReflectModuleNameExtractor implements ModuleNameExtractor
{

    @Override
    public String extract( File modulePath )
        throws IOException
    {
        String name = null;
        try
        {
            // Use Java9 code to get moduleName, don't try to do it better with own implementation
            Class moduleFinderClass = Class.forName( "java.lang.module.ModuleFinder" );

            Method ofMethod = moduleFinderClass.getMethod( "of", java.nio.file.Path[].class );
            Object moduleFinderInstance = ofMethod.invoke( null, new Object[] { new java.nio.file.Path[] { modulePath.toPath() } } );

            Method findAllMethod = moduleFinderClass.getMethod( "findAll" );
            Set<Object> moduleReferences = (Set<Object>) findAllMethod.invoke( moduleFinderInstance );

            if ( moduleReferences.isEmpty() )
            {
                return null;
            }
            Object moduleReference = moduleReferences.iterator().next();
            Method descriptorMethod = moduleReference.getClass().getMethod( "descriptor" );
            Object moduleDescriptorInstance = descriptorMethod.invoke( moduleReference );
            
            Method nameMethod = moduleDescriptorInstance.getClass().getMethod( "name" );
            name = (String) nameMethod.invoke( moduleDescriptorInstance );
        }
        catch ( ReflectiveOperationException e )
        {
            // noop
        }
        catch ( SecurityException e )
        {
            // noop
        }
        catch ( IllegalArgumentException e )
        {
            // noop
        }
        return name;        
    }

}
