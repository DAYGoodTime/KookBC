/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.plugin;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import snw.jkook.Core;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginClassLoader;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.launch.AccessClassLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

// The Plugin ClassLoader.
// Call close method on unused instances to ensure the instance will be fully destroyed.
public class SimplePluginClassLoader extends PluginClassLoader {
    public static final Collection<SimplePluginClassLoader> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<String, Class<?>> cache = new ConcurrentHashMap<>();
    private final KBCClient client;
    @Nullable
    private final AccessClassLoader parentClassLoader;

    public SimplePluginClassLoader(KBCClient client, @Nullable AccessClassLoader parent) {
        super(new URL[0], parent != null ? null : SimplePluginManager.class.getClassLoader());
        this.client = client;
        this.parentClassLoader = parent;
        INSTANCES.add(this);
    }


    @Override
    protected <T extends Plugin> T construct(final Class<T> cls, final PluginDescription description) throws Exception {
        T plugin = cls.getDeclaredConstructor().newInstance();
        Method initMethod = cls.getMethod(
                "init",
                File.class, File.class, PluginDescription.class, File.class, Logger.class, Core.class
        );
        File pluginFile;
        final URL location = cls.getProtectionDomain().getCodeSource().getLocation();
        if (location.getFile().endsWith(".class")) {
            if (!location.getFile().contains("!/")) {
                throw new IllegalArgumentException("Cannot obtain the source jar of the main class, location: " + location + ", maybe it is a single class file?");
            }
            String url = location.toString();
            url = url.substring(0, url.indexOf("!/")).replace("jar:file:/", "");
            try {
                pluginFile = new File(url);
            } catch (Exception e) {
                throw new IllegalArgumentException("url: " + url, e);
            }
        } else {
            pluginFile = new File(location.toURI());
        }
        File dataFolder = new File(client.getPluginsFolder(), description.getName());
        initMethod.invoke(plugin,
                new File(dataFolder, "config.yml"),
                dataFolder,
                description,
                pluginFile,
                new PrefixLogger(description.getName(), LoggerFactory.getLogger(cls)),
                client.getCore()
        );
        return plugin;
    }

    @Override
    protected Class<? extends Plugin> lookForMainClass(String mainClassName, File file) throws Exception {
        if (this.findLoadedClass(mainClassName) != null) {
            throw new IllegalArgumentException("The main class defined in plugin.yml has already been defined in the VM.");
        } else {
            if (parentClassLoader == null) {
                super.addURL(file.toURI().toURL());
            } else {
                parentClassLoader.addURL(file.toURI().toURL());
            }
            Class<?> loadClass = this.loadClass(mainClassName, true);
            Class<? extends Plugin> main = loadClass.asSubclass(Plugin.class);
            if (main.getDeclaredConstructors().length != 1) {
                throw new IllegalStateException("Unexpected constructor count, expected 1, got " + main.getDeclaredConstructors().length);
            } else {
                return main;
            }
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (parentClassLoader == null) {
            return super.loadClass(name, resolve);
        }
        return parentClassLoader.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass0(name, false);
    }

    public final Class<?> findClass0(String name, boolean dontCallOther) throws ClassNotFoundException {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        try {
            Class<?> result = parentClassLoader == null ? super.findClass(name) : parentClassLoader.findClass(name);
            if (result != null) {
                cache.put(name, result);
                return result;
            }
        } catch (ClassNotFoundException ignored) {
        }

        // Try to load class from other known instances if needed
        if (!dontCallOther) {
            Class<?> result = loadFromOther(name);
            if (result != null) {
                cache.put(name, result);
                return result;
            }
        }
        throw new ClassNotFoundException(name);
    }

    protected Class<?> loadFromOther(String name) throws ClassNotFoundException {
        for (SimplePluginClassLoader classLoader : INSTANCES) {
            if (classLoader == null) {
                // Suggested by ChatGPT:
                // The keys in a WeakHashMap are held through weak references,
                // which may be garbage collected when no strong references to them exist.
                // If null checks are not performed while traversing the key set,
                // it may lead to encountering null keys that have already been garbage collected,
                // resulting in a NullPointerException.
                // Therefore, when traversing the key set of a WeakHashMap,
                // it is necessary to perform a null check first and process only non-null keys.
                continue;
            }
            if (classLoader == this) {
                continue;
            }
            try {
                return classLoader.findClass0(name, true); // use true to prevent stack over flow
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public void close() throws IOException {
        INSTANCES.remove(this);
        super.close();
    }
}
