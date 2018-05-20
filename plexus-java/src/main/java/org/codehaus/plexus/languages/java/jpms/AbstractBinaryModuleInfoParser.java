package org.codehaus.plexus.languages.java.jpms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

public abstract class AbstractBinaryModuleInfoParser implements ModuleInfoParser
{
    private static final Pattern MRJAR_DESCRIPTOR = Pattern.compile( "META-INF/versions/[^/]+/module-info.class" );

    @Override
    public JavaModuleDescriptor getModuleDescriptor( Path modulePath )
        throws IOException
    {
        JavaModuleDescriptor descriptor;
        if ( Files.isDirectory( modulePath ) )
        {
            try ( InputStream in = Files.newInputStream( modulePath.resolve( "module-info.class" ) ) )
            {
                descriptor = parse( in );
            }
        }
        else
        {
            try ( JarFile jarFile = new JarFile( modulePath.toFile() ) )
            {
                JarEntry moduleInfo;
                if ( modulePath.toString().toLowerCase().endsWith( ".jmod" ) )
                {
                    moduleInfo = jarFile.getJarEntry( "classes/module-info.class" );
                }
                else
                {
                    moduleInfo = jarFile.getJarEntry( "module-info.class" );

                    if ( moduleInfo == null )
                    {
                        Manifest manifest =  jarFile.getManifest();

                        if ( manifest != null && "true".equalsIgnoreCase( manifest.getMainAttributes().getValue( "Multi-Release" ) ) ) 
                        {
                            // look for multirelease descriptor
                            Enumeration<JarEntry> entryIter = jarFile.entries();
                            while ( entryIter.hasMoreElements() )
                            {
                                JarEntry entry = entryIter.nextElement();
                                if ( MRJAR_DESCRIPTOR.matcher( entry.getName() ).matches() )
                                {
                                    moduleInfo = entry;
                                    break;
                                }
                            }
                        }
                    }
                }

                if ( moduleInfo != null )
                {
                    descriptor = parse( jarFile.getInputStream( moduleInfo ) );
                }
                else
                {
                    descriptor = null;
                }
            }
        }
        return descriptor;
    }

    abstract JavaModuleDescriptor parse( InputStream in ) throws IOException;
}
