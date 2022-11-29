package org.ivdnt.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

/**
 * Load plugins from a directory and hot reload them when their JAR changes.
 */
public class PluginManager {

    /** A hot-reloadable plugin. */
    class ReloadablePlugin {

        private StringProcessingPlugin instance;

        private final File jarFile;

        private long fileModifiedDate;

        public ReloadablePlugin(StringProcessingPlugin instance, File jarFile) {
            this.instance = instance;
            this.jarFile = jarFile;
            this.fileModifiedDate = jarFile.lastModified();
        }

        private synchronized void checkReload() {
            if (jarFile.lastModified() > fileModifiedDate) {
                // JAR was modified. Try to reload plugin.
                fileModifiedDate = jarFile.lastModified();
                System.out.println("RELOAD JAR: " + jarFile);

                // NOTE: we recraete loader here because .reload()'ing it is apparently not enough...
                for (StringProcessingPlugin plugin: serviceLoader(jarFile)) {
                    if (plugin.getClass().getName().equals(instance.getClass().getName())) {
                        // This is the updated version of this plugin.
                        instance = plugin;
                        System.out.println("PLUGIN RELOADED: " + plugin.getName());
                    }
                }
            }
        }

        public synchronized StringProcessingPlugin get() {
            checkReload();
            return instance;
        }
    }

    /** Loaded and instantiated plugins. */
    private final Map<String, ReloadablePlugin> plugins;

    /**
     * Return a list of available plugins.
     *
     * @return available plugins
     */
    public List<StringProcessingPlugin> list() {
        return plugins.values().stream()
                .map(ReloadablePlugin::get)
                .collect(Collectors.toList());
    }

    /**
     * Get a specific plugin by name.
     *
     * @param pluginName name of the plugin
     * @return plugin if found
     */
    public Optional<StringProcessingPlugin> get(String pluginName) {
        if (plugins.containsKey(pluginName))
            return Optional.of(plugins.get(pluginName).get());
        return Optional.empty();
    }

    /**
     * Initially Load plugins.
     *
     * @param pluginDir directory to load JARs from.
     */
    public PluginManager(File pluginDir) {
        plugins = new HashMap<>();
        try {
            // TODO: instead of reloading individual plugins, we should rescan the directory for
            //       new or updated JARs, so we can add/remove plugins on the fly as well.'

            File[] files = pluginDir.listFiles(file -> file.getName().toLowerCase(Locale.ROOT).endsWith(".jar"));
            if (files != null) {
                for (File f: files) {
                    loadPluginsFromJar(f);
                }
            }
            System.out.println("PLUGINS LOADED");
        } catch (Exception e) {
            System.out.println("COULD NOT LOAD PLUGINS");
            e.printStackTrace(System.out);
        }
    }

    private void loadPluginsFromJar(File jarFile) {
        try {
            for (StringProcessingPlugin plugin: serviceLoader(jarFile)) {
                plugins.put(plugin.getName(), new ReloadablePlugin(plugin, jarFile));
            }
        } catch (Exception e) {
            System.out.println("ERROR LOADING PLUGIN JAR: " + jarFile);
            e.printStackTrace(System.out);
        }
    }

    private ServiceLoader<StringProcessingPlugin> serviceLoader(File jarFile) {
        try {
            URL url = jarFile.toURI().toURL();
            URLClassLoader child = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());
            return ServiceLoader.load(StringProcessingPlugin.class, child);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
