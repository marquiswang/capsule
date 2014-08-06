/*
 * Capsule
 * Copyright (c) 2014, Parallel Universe Software Co. All rights reserved.
 * 
 * This program and the accompanying materials are licensed under the terms 
 * of the Eclipse Public License v1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package co.paralleluniverse.capsule;

import co.paralleluniverse.common.JarClassLoader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 *
 * @author pron
 */
public final class CapsuleLauncher {
    private static final String CAPSULE_CLASS_NAME = "Capsule";
    private static final String CUSTOM_CAPSULE_CLASS_NAME = "CustomCapsule";
    private static final String OPT_JMX_REMOTE = "com.sun.management.jmxremote";
    private static final String ATTR_MAIN_CLASS = "Main-Class";

    public static Object getCapsule(Path path) {
        try {
            // check manifest
            final Manifest mf;
            try (JarFile jar = new JarFile(path.toFile())) {
                mf = jar.getManifest();
            }
            final String mainClass = mf.getMainAttributes() != null ? mf.getMainAttributes().getValue(ATTR_MAIN_CLASS) : null;
            if (mainClass == null || (!CAPSULE_CLASS_NAME.equals(mainClass) && !CUSTOM_CAPSULE_CLASS_NAME.equals(mainClass)))
                throw new RuntimeException(path + " does not appear to be a valid capsule.");

            // load class
            final ClassLoader cl = new URLClassLoader(new URL[]{path.toUri().toURL()});
            final Class clazz = cl.loadClass(mainClass);

            // create capsule instance
            final Object capsule;
            try {
                final Constructor<?> ctor = clazz.getConstructor(Path.class);
                ctor.setAccessible(true);
                capsule = ctor.newInstance(path);
            } catch (Exception e) {
                throw new RuntimeException("Could not create capsule instance.", e);
            }
            return capsule;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(path + " does not appear to be a valid capsule.", e);
        }
    }

    public static Object getCapsule(byte[] buf) {
        try {
            // check manifest
            final JarClassLoader cl = new JarClassLoader(buf);
            final Manifest mf = cl.getManifest();
            final String mainClass = mf.getMainAttributes() != null ? mf.getMainAttributes().getValue(ATTR_MAIN_CLASS) : null;
            if (mainClass == null || (!CAPSULE_CLASS_NAME.equals(mainClass) && !CUSTOM_CAPSULE_CLASS_NAME.equals(mainClass)))
                throw new RuntimeException("The given buffer does not appear to be a valid capsule.");

            // load class
            final Class clazz = cl.loadClass(mainClass);

            // create capsule instance
            final Object capsule;
            try {
                final Constructor<?> ctor = clazz.getDeclaredConstructor(Path.class, Path.class, byte[].class, Object.class);
                ctor.setAccessible(true);
                capsule = ctor.newInstance(null, null, buf, null);
            } catch (Exception e) {
                throw new RuntimeException("Could not create capsule instance.", e);
            }
            return capsule;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The given buffer does not appear to be a valid capsule.", e);
        }
    }

    public static ProcessBuilder prepareForLaunch(Object capsule, List<String> cmdLine, String[] args) {
        try {
            final Method launch = capsule.getClass().getMethod("prepareForLaunch", List.class, String[].class);
            launch.setAccessible(true);
            return (ProcessBuilder) launch.invoke(capsule, cmdLine, args);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not call prepareForLaunch on " + capsule + ". It does not appear to be a valid capsule.", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InvocationTargetException e) {
            final Throwable t = e.getTargetException();
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof Error)
                throw (Error) t;
            throw new RuntimeException(t);
        }
    }

    public static String getAppId(Object capsule) {
        try {
            final Method appId = capsule.getClass().getMethod("appId", String[].class);
            appId.setAccessible(true);
            return (String) appId.invoke(capsule, (Object) null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Could not call appId on " + capsule + ". It does not appear to be a valid capsule.", e);
        } catch (IllegalAccessException e) {
            throw new AssertionError();
        } catch (InvocationTargetException e) {
            final Throwable t = e.getTargetException();
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof Error)
                throw (Error) t;
            throw new RuntimeException(t);
        }
    }

    public static List<String> enableJMX(List<String> cmdLine) {
        final String arg = "-D" + OPT_JMX_REMOTE;
        if (cmdLine.contains(arg))
            return cmdLine;
        final List<String> cmdLine2 = new ArrayList<>(cmdLine);
        cmdLine2.add(arg);
        return cmdLine2;
    }

    private CapsuleLauncher() {
    }
}