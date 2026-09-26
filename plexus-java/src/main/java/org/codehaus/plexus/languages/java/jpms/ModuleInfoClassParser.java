package org.codehaus.plexus.languages.java.jpms;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads the {@code Module} attribute (JVMS 4.7.25) directly out of a {@code module-info.class} file,
 * without depending on a bytecode library or on the class-file support of the running JDK.
 *
 * <p>Only the class file's overall shape (constant pool, then fields, then methods, then attributes)
 * needs to be understood to reach the {@code Module} attribute; every other section is skipped using
 * its own declared length rather than parsed. The {@code Module} attribute format has been stable
 * since it was introduced in Java 9, and this parser never looks at the class file's major/minor
 * version, so it reads a {@code module-info.class} produced by any future JDK just as well as one
 * from Java 9.
 *
 * @author Robert Scholte
 * @since 1.7.0
 */
class ModuleInfoClassParser extends AbstractBinaryModuleInfoParser {

    private static final int MAGIC = 0xCAFEBABE;

    // Constant pool tags, JVMS 4.4, Table 4.4-A.
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private static final String MODULE_ATTRIBUTE_NAME = "Module";

    // Module_attribute requires_flags, JVMS 4.7.25.
    private static final int ACC_TRANSITIVE = 0x0020;
    private static final int ACC_STATIC_PHASE = 0x0040;

    @Override
    JavaModuleDescriptor parse(InputStream in) throws IOException {
        DataInputStream dis = (in instanceof DataInputStream) ? (DataInputStream) in : new DataInputStream(in);

        int magic = dis.readInt();
        if (magic != MAGIC) {
            throw new IOException("Not a class file, bad magic number: 0x" + Integer.toHexString(magic));
        }

        dis.readUnsignedShort(); // minor_version, unused
        dis.readUnsignedShort(); // major_version, deliberately unchecked: the Module attribute below
        // has the same layout no matter which major version produced the file.

        ConstantPool pool = ConstantPool.read(dis);

        dis.readUnsignedShort(); // access_flags
        dis.readUnsignedShort(); // this_class
        dis.readUnsignedShort(); // super_class

        int interfacesCount = dis.readUnsignedShort();
        skipFully(dis, 2L * interfacesCount);

        skipMembers(dis); // fields
        skipMembers(dis); // methods

        int attributesCount = dis.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            int attributeNameIndex = dis.readUnsignedShort();
            long attributeLength = readU4(dis);
            if (MODULE_ATTRIBUTE_NAME.equals(pool.utf8(attributeNameIndex))) {
                return readModuleAttribute(dis, pool);
            }
            skipFully(dis, attributeLength);
        }

        throw new IOException("Not a module: no Module attribute found");
    }

    /**
     * Skips over a {@code fields} or {@code methods} array: same {@code field_info}/{@code method_info}
     * shape, each with its own list of attributes that are skipped by their declared length.
     */
    private static void skipMembers(DataInputStream dis) throws IOException {
        int count = dis.readUnsignedShort();
        for (int i = 0; i < count; i++) {
            skipFully(dis, 6); // access_flags, name_index, descriptor_index
            int attributesCount = dis.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++) {
                skipFully(dis, 2); // attribute_name_index
                long attributeLength = readU4(dis);
                skipFully(dis, attributeLength);
            }
        }
    }

    /**
     * Reads the body of the {@code Module} attribute (JVMS 4.7.25), mapping it onto a
     * {@link JavaModuleDescriptor} the same way {@code java.lang.module.ModuleDescriptor} does.
     */
    private static JavaModuleDescriptor readModuleAttribute(DataInputStream dis, ConstantPool pool) throws IOException {
        String moduleName = pool.moduleName(dis.readUnsignedShort());
        JavaModuleDescriptor.Builder builder = JavaModuleDescriptor.newModule(moduleName);

        dis.readUnsignedShort(); // module_flags
        dis.readUnsignedShort(); // module_version_index

        int requiresCount = dis.readUnsignedShort();
        for (int i = 0; i < requiresCount; i++) {
            int requiresIndex = dis.readUnsignedShort();
            int requiresFlags = dis.readUnsignedShort();
            dis.readUnsignedShort(); // requires_version_index

            String requiredModule = pool.moduleName(requiresIndex);
            if ((requiresFlags & (ACC_STATIC_PHASE | ACC_TRANSITIVE)) != 0) {
                Set<JavaModuleDescriptor.JavaRequires.JavaModifier> modifiers = new LinkedHashSet<>();
                if ((requiresFlags & ACC_STATIC_PHASE) != 0) {
                    modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.STATIC);
                }
                if ((requiresFlags & ACC_TRANSITIVE) != 0) {
                    modifiers.add(JavaModuleDescriptor.JavaRequires.JavaModifier.TRANSITIVE);
                }
                builder.requires(modifiers, requiredModule);
            } else {
                builder.requires(requiredModule);
            }
        }

        int exportsCount = dis.readUnsignedShort();
        for (int i = 0; i < exportsCount; i++) {
            int exportsIndex = dis.readUnsignedShort();
            dis.readUnsignedShort(); // exports_flags
            int exportsToCount = dis.readUnsignedShort();
            String pkg = pool.packageName(exportsIndex);
            if (exportsToCount == 0) {
                builder.exports(pkg);
            } else {
                Set<String> targets = new HashSet<>();
                for (int j = 0; j < exportsToCount; j++) {
                    targets.add(pool.moduleName(dis.readUnsignedShort()));
                }
                builder.exports(pkg, targets);
            }
        }

        int opensCount = dis.readUnsignedShort();
        for (int i = 0; i < opensCount; i++) {
            dis.readUnsignedShort(); // opens_index
            dis.readUnsignedShort(); // opens_flags
            int opensToCount = dis.readUnsignedShort();
            skipFully(dis, 2L * opensToCount);
        }

        int usesCount = dis.readUnsignedShort();
        for (int i = 0; i < usesCount; i++) {
            builder.uses(pool.className(dis.readUnsignedShort()));
        }

        int providesCount = dis.readUnsignedShort();
        for (int i = 0; i < providesCount; i++) {
            String service = pool.className(dis.readUnsignedShort());
            int withCount = dis.readUnsignedShort();
            List<String> providers = new ArrayList<>(withCount);
            for (int j = 0; j < withCount; j++) {
                providers.add(pool.className(dis.readUnsignedShort()));
            }
            builder.provides(service, providers);
        }

        return builder.build();
    }

    private static long readU4(DataInputStream dis) throws IOException {
        return dis.readInt() & 0xFFFFFFFFL;
    }

    private static void skipFully(DataInputStream dis, long length) throws IOException {
        long remaining = length;
        while (remaining > 0) {
            long skipped = dis.skip(remaining);
            if (skipped <= 0) {
                // skip() may legitimately return 0 near the end of the stream; fall back to read()
                if (dis.read() < 0) {
                    throw new IOException("Unexpected end of class file");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    /**
     * The subset of the constant pool this parser needs: the raw UTF-8 entries, and, for the
     * Class/Module/Package entries, which UTF-8 entry holds their name. All three of those tags
     * have the same one-field shape (JVMS 4.4.1, 4.4.8, 4.4.9): {@code u1 tag; u2 name_index;}.
     */
    private static final class ConstantPool {
        private final String[] utf8;
        private final int[] classLikeNameIndex;

        private ConstantPool(String[] utf8, int[] classLikeNameIndex) {
            this.utf8 = utf8;
            this.classLikeNameIndex = classLikeNameIndex;
        }

        static ConstantPool read(DataInputStream dis) throws IOException {
            int constantPoolCount = dis.readUnsignedShort();
            String[] utf8 = new String[constantPoolCount];
            int[] classLikeNameIndex = new int[constantPoolCount];

            for (int i = 1; i < constantPoolCount; i++) {
                int tag = dis.readUnsignedByte();
                switch (tag) {
                    case CONSTANT_UTF8:
                        utf8[i] = dis.readUTF();
                        break;
                    case CONSTANT_CLASS:
                    case CONSTANT_MODULE:
                    case CONSTANT_PACKAGE:
                        classLikeNameIndex[i] = dis.readUnsignedShort();
                        break;
                    case CONSTANT_STRING:
                        skipFully(dis, 2);
                        break;
                    case CONSTANT_INTEGER:
                    case CONSTANT_FLOAT:
                    case CONSTANT_FIELDREF:
                    case CONSTANT_METHODREF:
                    case CONSTANT_INTERFACE_METHODREF:
                    case CONSTANT_NAME_AND_TYPE:
                    case CONSTANT_DYNAMIC:
                    case CONSTANT_INVOKE_DYNAMIC:
                        skipFully(dis, 4);
                        break;
                    case CONSTANT_LONG:
                    case CONSTANT_DOUBLE:
                        skipFully(dis, 8);
                        // long/double take up two consecutive constant pool entries; the second is unused
                        i++;
                        break;
                    case CONSTANT_METHOD_HANDLE:
                        skipFully(dis, 3);
                        break;
                    case CONSTANT_METHOD_TYPE:
                        skipFully(dis, 2);
                        break;
                    default:
                        throw new IOException("Unsupported constant pool tag " + tag + " at index " + i);
                }
            }
            return new ConstantPool(utf8, classLikeNameIndex);
        }

        String utf8(int index) {
            return utf8[index];
        }

        String moduleName(int index) {
            return utf8[classLikeNameIndex[index]];
        }

        String packageName(int index) {
            return utf8[classLikeNameIndex[index]].replace('/', '.');
        }

        String className(int index) {
            return utf8[classLikeNameIndex[index]].replace('/', '.');
        }
    }
}
