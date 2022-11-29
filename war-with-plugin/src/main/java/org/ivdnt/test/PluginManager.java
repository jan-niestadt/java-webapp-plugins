package org.ivdnt.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Load plugins from a directory and hot reload them when their JAR changes.
 */
public class PluginManager<T extends StringProcessingPlugin> implements Iterable<T> {

    interface ServiceLoaderFactory<T> {
        ServiceLoader<T> serviceLoader(File jarFile);
    }

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

                // NOTE: we recraete loader here because .reload()'ing it is apparently not enough...
                for (Object plugin: serviceLoaderFactory.serviceLoader(jarFile)) {
                    registerPlugin((T)plugin);
                }
            }
        }

    }

    /** How to create ServiceLoader for our service type */
    private final ServiceLoaderFactory serviceLoaderFactory;

    /** Our plugin JAR files to monitor for changes. */
    private final List<PluginJar> jarFiles = new ArrayList<>();

    /** Loaded and instantiated plugins. */
    private final Map<String, T> plugins;

    /**
     * Initially Load plugins.
     *
     * @param pluginDir directory to load JARs from.
     */
    public PluginManager(File pluginDir, ServiceLoaderFactory serviceLoaderFactory) {
        this.serviceLoaderFactory = serviceLoaderFactory;
        plugins = new HashMap<>();
        try {
            File[] files = pluginDir.listFiles(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (files != null) {
                for (File f: files) {
                    // PluginJar constructor will load the JAR file and register the plugins.
                    registerPluginJar(f);
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
    public void registerPluginJar(File f) {
        jarFiles.add(new PluginJar(f));
    }

    /***
     * Register a plugin.
     *
     * @param plugin plugin to register
     */
    public void registerPlugin(T plugin) {
        plugins.put(plugin.getName(), plugin);
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
    public Optional<T> get(String pluginName) {
        reloadChangedPlugins();
        if (plugins.containsKey(pluginName))
            return Optional.of(plugins.get(pluginName));
        return Optional.empty();
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
