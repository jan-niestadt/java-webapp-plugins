package org.ivdnt.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {

    /** Directory to load plugins from */
    public static final File PLUGIN_DIR = new File("/home/jan/int-projects/studiedag/plugins");

    /** Loaded and instantiated plugins. */
    private Map<String, PluginEntry> plugins;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException {
        try {
            resp.addHeader("Content-Type", "text/plain");

            String plugin = req.getParameter("plugin");
            if (plugin == null || plugin.isEmpty()) {
                // No plugin specified. Show list of plugins.
                String output = getListOfPlugins();
                resp.getOutputStream().println(output);
            } else {
                // Process input string using specified plugin.
                String input = req.getParameter("input");
                String output = process(plugin, input);
                resp.getOutputStream().println(output);
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private String getListOfPlugins() {
        StringBuilder output = new StringBuilder();
        output.append("Available plugins:\n\n");
        for (PluginEntry e: plugins.values()) {
            StringProcessingPlugin plugin = e.get();
            output.append("- " + plugin.getName() + " (" + plugin.getDescription() + ")\n");
        }
        return output.toString();
    }

    private String process(String pluginName, String input) {
        ensurePluginsLoaded();

        PluginEntry pluginEntry = plugins.get(pluginName);
        if (pluginEntry != null) {
            StringProcessingPlugin plugin = pluginEntry.get();
            String output = plugin.process(input);
            return "PLUGIN " + pluginName + ": " + input + " -> " + output;
        }
        return "PLUGIN NOT LOADED " + pluginName + ": " + input;
    }

    @Override
    public void init() throws ServletException {
        super.init();

        ensurePluginsLoaded();

        System.out.println("init");
    }

    private synchronized void ensurePluginsLoaded() {
        if (plugins != null)
            return;
        plugins = new HashMap<>();
        try {
            for (File f: PLUGIN_DIR.listFiles()) {
                loadPluginsFromJar(f);
            }
            System.out.println("PLUGINS LOADED");
        } catch (Exception e) {
            System.out.println("COULD NOT LOAD PLUGINS");
            e.printStackTrace(System.out);
        }
    }

    /** A hot-reloadable plugin. */
    static class PluginEntry {

        private StringProcessingPlugin instance;

        private File jarFile;

        private long fileModifiedDate;

        public PluginEntry(StringProcessingPlugin instance, File jarFile) {
            this.instance = instance;
            this.jarFile = jarFile;
            this.fileModifiedDate = jarFile.lastModified();
        }

        private synchronized void checkReload() {
            if (jarFile.lastModified() > fileModifiedDate) {
                // JAR was modified. Try to reload plugin.
                fileModifiedDate = jarFile.lastModified();
                System.out.println("RELOAD JAR: " + jarFile);

                try {
                    URL url = jarFile.toURI().toURL();
                    URLClassLoader child = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());
                    String fqcn = instance.getClass().getName();
                    ServiceLoader<StringProcessingPlugin> loader = ServiceLoader.load(StringProcessingPlugin.class, child);
                    for (StringProcessingPlugin plugin: loader) {
                        if (plugin.getClass().getName().equals(instance.getClass().getName())) {
                            // This is the updated version of this plugin.
                            instance = plugin;
                            System.out.println("PLUGIN RELOADED: " + plugin.getName());
                        }
                    }
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public synchronized StringProcessingPlugin get() {
            checkReload();
            return instance;
        }
    }

    private void loadPluginsFromJar(File jarFile) {
        try {
            // https://stackoverflow.com/questions/60764/how-to-load-jar-files-dynamically-at-runtime
            URL url = jarFile.toURI().toURL();
            URLClassLoader child = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());
            ServiceLoader<StringProcessingPlugin> loader = ServiceLoader.load(StringProcessingPlugin.class, child);
            for (StringProcessingPlugin plugin: loader) {
                plugins.put(plugin.getName(), new PluginEntry(plugin, jarFile));
            }
        } catch (Exception e) {
            System.out.println("ERROR LOADING PLUGIN JAR: " + jarFile);
            e.printStackTrace(System.out);
        }
    }

}
