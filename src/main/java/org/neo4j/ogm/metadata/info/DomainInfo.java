package org.neo4j.ogm.metadata.info;

import org.neo4j.ogm.metadata.MappingException;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A Type Hierarchy (including Interfaces) is actually a DAG. Maybe we should be using Neo? !!
 *
 * This class needs a lot of tidying up
 */
public class DomainInfo {

    private List<String> classPaths = new ArrayList<>();

    private final HashMap<String, ClassInfo> classNameToClassInfo = new HashMap<>();
    private final HashMap<String, InterfaceInfo> interfaceNameToInterfaceInfo = new HashMap<>();
    private final HashMap<String, ArrayList<ClassInfo>> annotationNameToClassInfo = new HashMap<>();
    private final HashMap<String, ArrayList<ClassInfo>> interfaceNameToClassInfo = new HashMap<>();

    private ConstantPool constantPool;


    private static void buildTree(ClassInfo classInfo) {
        for (ClassInfo subclass : classInfo.directSubclasses()) {
            buildTree(subclass);
        }
    }

    // todo - should be part of interface info functionality
    private void constructInterfaceHierarcy(InterfaceInfo interfaceInfo) {
        if (interfaceInfo.allSuperInterfaces().isEmpty() && !interfaceInfo.superInterfaces().isEmpty()) {
            interfaceInfo.allSuperInterfaces().addAll(interfaceInfo.superInterfaces());
            for (InterfaceInfo superinterfaceInfo : interfaceInfo.superInterfaces()) {
                if (superinterfaceInfo != null) {
                    constructInterfaceHierarcy(superinterfaceInfo);
                    interfaceInfo.allSuperInterfaces().addAll(superinterfaceInfo.allSuperInterfaces());
                }
            }
        }
    }

    private void constructClassHierarchy() {

        if (classNameToClassInfo.isEmpty() && interfaceNameToInterfaceInfo.isEmpty()) {
            return;
        }

        /*
         * get the root classes in the type hierarchy.
         */
        ArrayList<ClassInfo> roots = new ArrayList<>();
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            if (classInfo.directSuperclass() == null) {
                roots.add(classInfo);
            }
        }

        LinkedList<ClassInfo> nodes = new LinkedList<>();
        nodes.addAll(roots);
        while (!nodes.isEmpty()) {
            ClassInfo head = nodes.removeFirst();
            for (ClassInfo subclass : head.directSubclasses()) {
                nodes.add(subclass);
            }
        }

        for (ClassInfo root : roots) {
            buildTree(root);
        }

        // A <-[:has_annotation]- T
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            for (AnnotationInfo annotation : classInfo.annotations()) {
                ArrayList<ClassInfo> classInfoList = annotationNameToClassInfo.get(annotation.getName());
                if (classInfoList == null) {
                    annotationNameToClassInfo.put(annotation.getName(), classInfoList = new ArrayList<>());
                }
                classInfoList.add(classInfo);
            }
        }

        // I - [:extends] -> J
        for (InterfaceInfo interfaceInfo : interfaceNameToInterfaceInfo.values()) {
            constructInterfaceHierarcy(interfaceInfo);
        }

        // T -[:implements]-> I
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            HashSet<InterfaceInfo> interfaceAndSuperinterfaces = new HashSet<>();
            for (InterfaceInfo interfaceInfo : classInfo.interfaces()) {
                interfaceAndSuperinterfaces.add(interfaceInfo);
                if (interfaceInfo != null) {
                    interfaceAndSuperinterfaces.addAll(interfaceInfo.allSuperInterfaces());
                }
            }
            for (InterfaceInfo interfaceInfo : interfaceAndSuperinterfaces) {
                ArrayList<ClassInfo> classInfoList = interfaceNameToClassInfo.get(interfaceInfo.name());
                if (classInfoList == null) {
                    interfaceNameToClassInfo.put(interfaceInfo.name(), classInfoList = new ArrayList<>());
                }
                classInfoList.add(classInfo);
            }
        }

        // transitive interface implementations: S-[:extends]->T-[:implements]->I  => S-[:implements]->I
        for (String interfaceName : interfaceNameToClassInfo.keySet()) {
            ArrayList<ClassInfo> classes = interfaceNameToClassInfo.get(interfaceName);
            HashSet<ClassInfo> subClasses = new HashSet<>(classes);
            for (ClassInfo classInfo : classes) {
                if (classInfo != null) {
                    for (ClassInfo subClassInfo : classInfo.directSubclasses()) {
                        subClasses.add(subClassInfo);
                    }
                }
            }
            interfaceNameToClassInfo.put(interfaceName, new ArrayList<>(subClasses));
        }

        // TODO: transitive annotations
        // if a superclass type, method or field is annotated, inject the annotation to subclasses
        // explicitly. Saves having to walk through type hierarchies to find an annotation.
        // must also include annotated interfaces.  WHICH WE DONT DO YET.

    }

    /**
     * parses class file binary header.
     */
    private void readClassInfo(final InputStream inputStream) throws IOException {

        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream, 1024));

        // Magic
        if (dataInputStream.readInt() != 0xCAFEBABE) {
            return;
        }

        dataInputStream.readUnsignedShort();    //minor version
        dataInputStream.readUnsignedShort();    // major version

        // Constant pool count (1-indexed, zeroth entry not used)
        int cpCount = dataInputStream.readUnsignedShort();

        // Constant pool
        Object[] constantPoolBackingArray = new Object[cpCount];
        //constantPool = new ConstantPool(new Object[cpCount]);

        for (int i = 1; i < cpCount; ++i) {
            final int tag = dataInputStream.readUnsignedByte();
            switch (tag) {
                case 1: // Modified UTF8
                    constantPoolBackingArray[i] = dataInputStream.readUTF();
                    break;
                case 3: // int
                case 4: // float
                    dataInputStream.skipBytes(4);
                    break;
                case 5: // long
                case 6: // double
                    dataInputStream.skipBytes(8);
                    i++; // double slot
                    break;
                case 7: // Class
                case 8: // String
                    // Forward or backward reference a Modified UTF8 entry
                    constantPoolBackingArray[i] = dataInputStream.readUnsignedShort();
                    break;
                case 9: // field ref
                case 10: // method ref
                case 11: // interface ref
                case 12: // name and type
                    dataInputStream.skipBytes(2); // reference to owning class
                    constantPoolBackingArray[i]=dataInputStream.readUnsignedShort();
                    break;
                case 15: // method handle
                    dataInputStream.skipBytes(3);
                    break;
                case 16: // method type
                    dataInputStream.skipBytes(2);
                    break;
                case 18: // invoke dynamic
                    dataInputStream.skipBytes(4);
                    break;
                default:
                    throw new ClassFormatError("Unknown tag value for constant pool entry: " + tag);
            }
        }

        constantPool = new ConstantPool(constantPoolBackingArray);

        // Access flags
        int flags = dataInputStream.readUnsignedShort();
        boolean isInterface = (flags & 0x0200) != 0;

        String className = constantPool.lookup(dataInputStream.readUnsignedShort()).replace('/', '.');
        String superclassName = constantPool.lookup(dataInputStream.readUnsignedShort()).replace('/', '.');

        // TODO : should be a field info?
        Map<String, ObjectAnnotations> fieldInfoMap = new HashMap<>();

        // TODO : should be a method info?
        Map<String, ObjectAnnotations> methodInfoMap = new HashMap<>();

        // get the interface names implemented by this class
        Set<InterfaceInfo> interfaces = new HashSet<>();

        int interfaceCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < interfaceCount; i++) {
            String interfaceName = constantPool.lookup(dataInputStream.readUnsignedShort()).replace('/', '.');
            interfaces.add(new InterfaceInfo(interfaceName));
        }

        // get the field information for this class
        int fieldCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < fieldCount; i++) {
            dataInputStream.skipBytes(2); // access_flags
            String fieldName = constantPool.lookup(dataInputStream.readUnsignedShort()); // name_index
            dataInputStream.skipBytes(2); // descriptor_index
            int attributesCount = dataInputStream.readUnsignedShort();

            for (int j = 0; j < attributesCount; j++) {
                ObjectAnnotations fieldAnnotations = new ObjectAnnotations();
                String attributeName = constantPool.lookup(dataInputStream.readUnsignedShort());
                int attributeLength = dataInputStream.readInt();
                if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                    int annotationCount = dataInputStream.readUnsignedShort();
                    for (int m = 0; m < annotationCount; m++) {
                        AnnotationInfo info = AnnotationInfo.readAnnotation(dataInputStream, constantPool);
                        // todo: maybe register just the annotations we're interested in.
                        fieldAnnotations.put(info.getName(), info);
                    }
                }
                else {
                    dataInputStream.skipBytes(attributeLength);
                }
                fieldInfoMap.put(fieldName, fieldAnnotations);
            }
        }

        // get method information for this class
        int methodCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < methodCount; i++) {
            dataInputStream.skipBytes(2); // access_flags
            String methodName = constantPool.lookup(dataInputStream.readUnsignedShort()); // name_index
            dataInputStream.skipBytes(2); // descriptor_index
            int attributesCount = dataInputStream.readUnsignedShort();

            for (int j = 0; j < attributesCount; j++) {
                ObjectAnnotations methodAnnotations = new ObjectAnnotations();

                String attributeName = constantPool.lookup(dataInputStream.readUnsignedShort());
                int attributeLength = dataInputStream.readInt();
                if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                    int annotationCount = dataInputStream.readUnsignedShort();
                    for (int m = 0; m < annotationCount; m++) {
                        AnnotationInfo info = AnnotationInfo.readAnnotation(dataInputStream, constantPool);
                        // todo: maybe register just the annotations we're interested in.
                        methodAnnotations.put(info.getName(), info);
                    }
                }
                else {
                    dataInputStream.skipBytes(attributeLength);
                }
                methodInfoMap.put(methodName, methodAnnotations);
            }
        }

        // class annotations
        Set<AnnotationInfo> classAnnotations = new HashSet<>();

        int attributesCount = dataInputStream.readUnsignedShort();
        for (int i = 0; i < attributesCount; i++) {
            String attributeName = constantPool.lookup(dataInputStream.readUnsignedShort());
            int attributeLength = dataInputStream.readInt();
            if ("RuntimeVisibleAnnotations".equals(attributeName)) {
                int annotationCount = dataInputStream.readUnsignedShort();
                for (int m = 0; m < annotationCount; m++) {
                    AnnotationInfo info = AnnotationInfo.readAnnotation(dataInputStream, constantPool);
                    // todo: maybe register just the annotations we're interested in.
                    classAnnotations.add(info);
                }
            }
            else {
                dataInputStream.skipBytes(attributeLength);
            }
        }

        // split reader here, and return the interfaces and annotations ?
        // this class IS AN INTERFACE
        if (isInterface) {
            // its an interface ref
            InterfaceInfo thisInterfaceInfo = interfaceNameToInterfaceInfo.get(className);
            if (thisInterfaceInfo == null) {
                interfaceNameToInterfaceInfo.put(className, new InterfaceInfo(className));
            } else {
                return;
            }

        } else {
            // its a class ref
            ClassInfo thisClassInfo = classNameToClassInfo.get(className);
            if (thisClassInfo == null) {
                thisClassInfo = new ClassInfo(className, interfaces, classAnnotations, fieldInfoMap, methodInfoMap);
                classNameToClassInfo.put(className, thisClassInfo);
            } else if (thisClassInfo.visited()) {
                return;
            } else {
                thisClassInfo.visit(interfaces, classAnnotations);
            }

            ClassInfo superclassInfo = classNameToClassInfo.get(superclassName);
            if (superclassInfo == null) {
                classNameToClassInfo.put(superclassName, new ClassInfo(superclassName, thisClassInfo));
            } else {
                superclassInfo.addSubclass(thisClassInfo);
            }
        }

    }

    private void scanFile(File file, String relativePath) throws IOException {
        if (relativePath.endsWith(".class")) {
            try (InputStream inputStream = new FileInputStream(file)) {
                readClassInfo(inputStream);
            }
        }
    }

    private void scanFolder(File folder, int prefixSize) throws IOException {

        String absolutePath = folder.getPath();
        String relativePath = prefixSize > absolutePath.length() ? "" : absolutePath.substring(prefixSize);

        boolean scanFolders = false, scanFiles = false;

        // TODO: use filter pattern
        for (String pathToScan : classPaths) {
            if (relativePath.startsWith(pathToScan) || (relativePath.length() == pathToScan.length() - 1 && pathToScan.startsWith(relativePath))) {
                scanFolders = scanFiles = true;
                break;
            }
            if (pathToScan.startsWith(relativePath)) {
                scanFolders = true;
            }
        }

        if (scanFolders || scanFiles) {
            File[] subFiles = folder.listFiles();
            for (final File subFile : subFiles) {
                if (subFile.isDirectory()) {
                    scanFolder(subFile, prefixSize);
                } else if (scanFiles && subFile.isFile()) {
                    String leafSuffix = "/" + subFile.getName();
                    scanFile(subFile, relativePath + leafSuffix);
                }
            }
        }
    }

    /**
     * Scan a zipfile for matching file path patterns. (Does not recurse into zipfiles within zipfiles.)
     */
    private void scanZipFile(final ZipFile zipFile) throws IOException {

        for (Enumeration<? extends ZipEntry> entries = zipFile.entries(); entries.hasMoreElements();) {
            final ZipEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String path = entry.getName();
                boolean scanFile = false;
                for (String pathToScan : classPaths) {
                    if (path.startsWith(pathToScan)) {
                        scanFile = true;
                        break;
                    }
                }
                if (scanFile && path.endsWith(".class")) {
                    try (InputStream inputStream = zipFile.getInputStream(entry)) {
                        readClassInfo(inputStream);
                    }
                }
            }
        }
    }

    /**
     * Get a list of unique elements on the classpath as File objects, preserving order.
     * Classpath elements that do not exist are not returned.
     */
    public static ArrayList<File> getUniqueClasspathElements() {
        String[] pathElements = System.getProperty("java.class.path").split(File.pathSeparator);
        HashSet<String> pathElementsSet = new HashSet<>();
        ArrayList<File> pathFiles = new ArrayList<>();
        for (String pathElement : pathElements) {
            if (pathElementsSet.add(pathElement)) {
                File file = new File(pathElement);
                if (file.exists()) {
                    pathFiles.add(file);
                }
            }
        }
        return pathFiles;
    }

    public void scan(String... packages) {

        classPaths.clear();
        classNameToClassInfo.clear();
        interfaceNameToInterfaceInfo.clear();
        annotationNameToClassInfo.clear();
        interfaceNameToClassInfo.clear();

        for (String packageName : packages) {
            String path = packageName.replaceAll("\\.", File.separator);
            classPaths.add(path);
        }

        try {
            for (File pathElt : getUniqueClasspathElements()) {
                String path = pathElt.getPath();
                if (pathElt.isDirectory()) {
                    scanFolder(pathElt, path.length() + 1);
                } else if (pathElt.isFile()) {
                    String pathLower = path.toLowerCase();
                    if (pathLower.endsWith(".jar") || pathLower.endsWith(".zip")) {
                        scanZipFile(new ZipFile(pathElt));
                    } else {
                        scanFile(pathElt, pathElt.getName());
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        constructClassHierarchy();

    }

    public ClassInfo getClass(String fqn) {
        return classNameToClassInfo.get(fqn);
    }

    public ClassInfo getClassSimpleName(String simpleClassName) {

        ClassInfo match = null;
        for (String fqn : classNameToClassInfo.keySet()) {
            if (fqn.endsWith("." + simpleClassName)) {
                if (match == null) {
                    match = classNameToClassInfo.get(fqn);
                } else {
                    throw new MappingException("More than one class has simple name: " + simpleClassName);
                }
            }
        }
        return match;
    }

    public ClassInfo getNamedClassWithAnnotation(String annotation, String className) {
        for (ClassInfo classInfo : annotationNameToClassInfo.get(annotation)) {
            if (classInfo.name().equals(className)) {
                return classInfo;
            }
        }
        return null;
    }

    public List<ClassInfo> getClassInfosWithAnnotation(String annotation) {
        return annotationNameToClassInfo.get(annotation);
    }
}