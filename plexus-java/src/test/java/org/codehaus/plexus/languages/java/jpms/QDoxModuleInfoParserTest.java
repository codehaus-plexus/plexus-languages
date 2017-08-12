package org.codehaus.plexus.languages.java.jpms;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.codehaus.plexus.languages.java.jpms.JavaModuleDescriptor.JavaExports;
import org.junit.Test;

public class QDoxModuleInfoParserTest
{
    private QDoxModuleInfoParser parser = new QDoxModuleInfoParser();

    @Test
    public void test() throws Exception
    {
        JavaModuleDescriptor moduleDescriptor = parser.fromSourcePath( new File( "src/test/resources/src.dir" ) );
        assertEquals( "a.b.c", moduleDescriptor.name() ); 
        assertEquals( "d.e", moduleDescriptor.requires().iterator().next().name() );
        
        Iterator<JavaExports> exportsIter = moduleDescriptor.exports().iterator();
        
        JavaExports exports = exportsIter.next(); 
        assertEquals( "f.g", exports.source() );
        
        exports = exportsIter.next(); 
        assertEquals( "f.g.h", exports.source() );
        assertEquals( new HashSet<>( Arrays.asList( "i.j", "k.l.m" ) ), exports.targets() );
        
    }

}
