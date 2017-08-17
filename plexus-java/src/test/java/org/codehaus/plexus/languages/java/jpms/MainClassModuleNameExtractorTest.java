package org.codehaus.plexus.languages.java.jpms;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class MainClassModuleNameExtractorTest extends AbstractFilenameModuleNameExtractorTest
{
    @Override
    protected ModuleNameExtractor getExtractor()
    {
        return new ModuleNameExtractor()
        {
            MainClassModuleNameExtractor extractor = new MainClassModuleNameExtractor( new File( System.getProperty( "java.home" ) ) );
            
            @Override
            public String extract( File file )
                throws IOException
            {
                return extractor.extract( Collections.singletonMap( file, file.toPath() ) ).get( file );
            }
        };
    }
}
