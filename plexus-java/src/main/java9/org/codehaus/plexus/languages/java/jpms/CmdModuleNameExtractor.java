package org.codehaus.plexus.languages.java.jpms;

import java.io.IOException;
import java.lang.module.FindException;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * 
 * @author Robert Scholte
 * @since 1.0.0
 */
public class CmdModuleNameExtractor
{
    public static void main( String[] args )
    {
        Properties properties = new Properties();

        for ( String path : args )
        {
            try
            {
                String moduleName = getModuleName( Paths.get( path ) );
                if ( moduleName != null )
                {
                    properties.setProperty( path, moduleName );
                }
            }
            catch ( Exception e )
            {
                System.err.append( e.getMessage() );
            }
        }

        try
        {
            properties.store( System.out, "" );
        }
        catch ( IOException e )
        {
            System.exit( 1 );
        }
    }

    public static String getModuleName( Path modulePath ) throws FindException
    {
        Set<ModuleReference> moduleReferences = ModuleFinder.of( modulePath ).findAll();
        
        Optional<ModuleReference> modRef = moduleReferences.stream().findFirst();

        return modRef.isPresent() ? modRef.get().descriptor().name() : null;
    }
}
