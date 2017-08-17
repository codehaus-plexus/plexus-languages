package org.codehaus.plexus.languages.java.jpms;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

public interface ModuleNamesExtractor extends ModuleNameExtractor
{
    Map<Path, String> extract( Collection<Path> files ) throws IOException;
}
