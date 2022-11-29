package org.ivdnt.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Load plugins and hot reload them if their JAR changes.
 */
public class PluginManager<T extends StringProcessor> implements Iterable<T> {

    /**
     * A JAR file we're monitoring, so we can hot reload.
     */
    class PluginJar {

        /** The JAR file */
        private final File jarFile;

        /** Mtime from last load. */
        private long fileModifiedDate;

        public PluginJar(File jarFile) {
            this.jarFile = jarFile;
            this.fileModifiedDate = -1;
            checkReload();
        }

        /**
         * Check if the file changed, and reload plugins if it did.
         */
        public synchronized void checkReload() {
            if (jarFile.lastModified() > fileModifiedDate) {
                // JAR was modified. Try to reload plugin.
                fileModifiedDate = jarFile.lastModified();
                System.out.println("LOAD JAR: " + jarFile);

                try {
                    // NOTE: we recreate loader here because .reload()'ing it is apparently not enough...
                    URL url = jarFile.toURI().toURL();
                    URLClassLoader child = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());
                    ServiceLoader<T> serviceLoader = ServiceLoader.load(clazz, child);
                    for (T plugin: serviceLoader) {
                        registerPlugin(plugin);
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    /** Base class or interface for our plugins. */
    private final Class<T> clazz;

    /** Default package to optionally use when searching by class name. */
    private String defaultPackage = "";

    /** Our plugin JAR files to monitor for changes. */
    private final List<PluginJar> jarFiles = new ArrayList<>();

    /** Loaded and instantiated plugins. */
    private final Map<String, T> plugins = new HashMap<>();

    /** Loaded and instantiated plugins. */
    private final Map<String, T> pluginsByClass = new HashMap<>();

    /**
     * Initially Load plugins.
     *
     * @param clazz base class or interface for our plugins
     */
    public PluginManager(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void setDefaultPackage(String defaultPackage) {
        this.defaultPackage = defaultPackage;
    }

    /**
     * Load and monitor plugin JARs from specified dir.
     * @param pluginDir directory to load plugins from
     */
    public void registerDirectory(File pluginDir) {
        try {
            File[] files = pluginDir.listFiles(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (files != null) {
                for (File f: files) {
                    // PluginJar constructor will load the JAR file and register the plugins.
                    registerJarFile(f);
                }
            }
            System.out.println("PLUGINS LOADED");
        } catch (Exception e) {
            System.out.println("COULD NOT LOAD PLUGINS");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Load and start monitoring a JAR containing plugins.
     *
     * @param f JAR file to load and monitor
     */
    public void registerJarFile(File f) {
        jarFiles.add(new PluginJar(f));
    }

    /***
     * Register a plugin.
     *
     * Note that hot reloading won't work if you call this method directly.
     *
     * @param plugin plugin to register
     */
    public void registerPlugin(T plugin) {
        plugins.put(plugin.getName(), plugin);
        pluginsByClass.put(plugin.getClass().getName(), plugin);
    }

    /**
     * Return a list of available plugins.
     *
     * @return available plugins
     */
    public Iterator<T> iterator() {
        return plugins.values().iterator();
    }

    /**
     * Get a specific plugin by name.
     *
     * @param pluginName name of the plugin
     * @return plugin if found
     */
    public Optional<T> byName(String pluginName) {
        reloadChangedPlugins();
        if (plugins.containsKey(pluginName))
            return Optional.of(plugins.get(pluginName));
        return Optional.empty();
    }

    /**
     * Get a specific plugin by fully qualified class name.
     *
     * @param className classname, either fully qualified or in the default package
     * @return plugin if found
     */
    public Optional<T> byClass(String className) {
        reloadChangedPlugins();
        if (pluginsByClass.containsKey(className))
            return Optional.of(pluginsByClass.get(className));
        if (pluginsByClass.containsKey(defaultPackage + "." + className))
            return Optional.of(pluginsByClass.get(defaultPackage + "." + className));
        return Optional.empty();
    }

    /**
     * Get plugin by either name or fully qualified classname.
     *
     * @param id name or class name
     * @return plugin if found
     */
    public Optional<T> get(String id) {
        Optional<T> result = byName(id);
        if (result.isEmpty())
            result = byClass(id);
        return result;
    }

    /**
     * Check if any of the JAR files changed and if so, reload plugins.
     */
    private synchronized void reloadChangedPlugins() {
        for (PluginJar jar: jarFiles) {
            jar.checkReload();
        }
    }

}
