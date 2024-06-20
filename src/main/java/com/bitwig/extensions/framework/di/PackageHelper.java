package com.bitwig.extensions.framework.di;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class to get classes from packages.
 */
public class PackageHelper {
    
    private PackageHelper() {
        // just a  utility class
    }
    
    public static List<Class<?>> getClasses(final Class<?> baseClass, final Package... packages) throws
        IOException,
        ClassNotFoundException {
        final ClassLoader classLoader = baseClass.getClassLoader();
        final List<Class<?>> classes = new ArrayList<>();
        final List<Class<?>> baseList = getClasses(baseClass.getPackageName(), classLoader);
        for (final Package pack : packages) {
            classes.addAll(getClasses(pack.getName(), classLoader));
        }
        classes.addAll(baseList);
        return classes;
    }
    
    private static List<Class<?>> getClasses(final String packageName, final ClassLoader classLoader) throws
        IOException,
        ClassNotFoundException {
        assert classLoader != null;
        final String path = packageName.replace('.', '/');
        final Enumeration<URL> resources = classLoader.getResources(path);
        final List<String> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            final URL resource = resources.nextElement();
            dirs.add(resource.getFile());
        }
        
        final TreeSet<String> classes = new TreeSet<>();
        for (final String directory : dirs) {
            classes.addAll(findClasses(directory, packageName));
        }
        final ArrayList<Class<?>> classList = new ArrayList<>();
        for (final String clazz : classes) {
            classList.add(Class.forName(clazz));
        }
        
        return classList;
    }
    
    
    private static Optional<URL> toDirectoryFilePath(final String directory) throws MalformedURLException {
        if (directory.startsWith("file:") && directory.contains("!")) {
            final String[] split = directory.split("!");
            return Optional.of(new URL(split[0]));
        }
        return Optional.empty();
    }
    
    private static String toSystemPath(final String directory) {
        if (File.separatorChar == '\\') {
            return directory.replace("%20", " ");
        }
        return directory;
    }
    
    private static Optional<String> classNameFromZipEntry(final ZipEntry entry, final String packageName) {
        if (entry.getName().endsWith(".class")) {
            final String className =
                entry.getName().replaceAll("[$].*", "").replaceAll("[.]class", "").replace('/', '.');
            if (className.startsWith(packageName)) {
                return Optional.of(className);
            }
        }
        return Optional.empty();
    }
    
    private static TreeSet<String> findClasses(final String directory, final String packageName) throws IOException {
        final TreeSet<String> classes = new TreeSet<>();
        
        final Optional<URL> dirUrl = toDirectoryFilePath(directory);
        if (dirUrl.isPresent()) {
            try (final ZipInputStream zip = new ZipInputStream(dirUrl.get().openStream())) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    classNameFromZipEntry(entry, packageName) //
                        .ifPresent(classes::add);
                }
            }
        }
        
        final File dir = new File(toSystemPath(directory));
        if (!dir.exists()) {
            return classes;
        }
        final File[] files = dir.listFiles();
        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    assert !file.getName().contains(".");
                    classes.addAll(findClasses(file.getAbsolutePath(), packageName + "." + file.getName()));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(packageName + '.' + file.getName().substring(0, file.getName().length() - 6));
                }
            }
        }
        return classes;
    }
}
