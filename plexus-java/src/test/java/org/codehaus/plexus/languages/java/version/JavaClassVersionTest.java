package org.codehaus.plexus.languages.java.version;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaClassVersionTest {

    @ParameterizedTest
    @MethodSource("provideClassFiles")
    void filesClassVersions(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int javaVersion = Integer.parseInt(fileName.substring(fileName.indexOf("-") + 1, fileName.length() - 6));
        JavaClassfileVersion classVersion = JavaClassfileVersion.of(filePath);
        assertEquals(javaVersion + 44, classVersion.majorVersion());
        assertEquals(0, classVersion.minorVersion());
        assertEquals(JavaVersion.parse("" + javaVersion), classVersion.javaVersion());
    }

    static Stream<Path> provideClassFiles() {
        List<Path> paths;
        try (DirectoryStream<Path> directoryStream =
                Files.newDirectoryStream(Paths.get("src/test/test-data/classfile.version/"), "*-[0-9]?.class")) {
            paths = StreamSupport.stream(directoryStream.spliterator(), false)
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return paths.stream();
    }

    @Test
    void javaClassPreview() {
        Path previewFile = Paths.get("src/test/test-data/classfile.version/helloworld-preview.class");
        JavaClassfileVersion previewClass = JavaClassfileVersion.of(previewFile);
        assertTrue(previewClass.isPreview());
        assertEquals(20 + 44, previewClass.majorVersion());
        assertEquals(JavaVersion.parse("20"), previewClass.javaVersion());
    }

    @Test
    void javaClassVersionMajor45orAbove() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JavaClassfileVersion(44, 0),
                "Java class major version must be 45 or above.");
    }

    @Test
    void equalsContract() {
        JavaClassfileVersion javaClassVersion = new JavaClassfileVersion(65, 0);
        JavaClassfileVersion previewFeature = new JavaClassfileVersion(65, 65535);
        assertNotEquals(javaClassVersion, previewFeature);
        assertNotEquals(javaClassVersion.hashCode(), previewFeature.hashCode());

        JavaClassfileVersion javaClassVersionOther = new JavaClassfileVersion(65, 0);
        assertEquals(javaClassVersion, javaClassVersionOther);
        assertEquals(javaClassVersion.hashCode(), javaClassVersionOther.hashCode());
        assertEquals(javaClassVersion.javaVersion(), javaClassVersionOther.javaVersion());
        assertEquals(javaClassVersion.javaVersion(), previewFeature.javaVersion());
    }
}
