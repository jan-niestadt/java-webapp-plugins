package org.ivdnt.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jdk.tools.jlink.resources.plugins;

public class TestServlet extends HttpServlet {

    /** Directory to load plugins from */
    public static final File PLUGIN_DIR = new File("/home/jan/int-projects/studiedag/plugins");

    /** Package in which plugin classes are found */
    public static final String PLUGIN_PACKAGE = "org.ivdnt.test.";

    /** Loaded and instantiated plugins. */
    private Map<String, StringProcessingPlugin> plugins;

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
        for (Map.Entry<String, StringProcessingPlugin> e: plugins.entrySet()) {
            output.append("- " + e.getKey() + " (" + e.getValue().getDescription() + ")\n");
        }
        return output.toString();
    }

    private String process(String pluginName, String input) {
        ensurePluginsLoaded();

        StringProcessingPlugin plugin = plugins.get(pluginName);
        if (plugin != null) {
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
                loadPlugin(f);
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

        private String fqClassName;

        public PluginEntry(File f) {

        }
    }

    private void loadPlugin(File jarFile) {
        String name = jarFile.getName().replaceAll("\\.[jJ][aA][rR]$", "");
        try {
            // https://stackoverflow.com/questions/60764/how-to-load-jar-files-dynamically-at-runtime
            URLClassLoader child = new URLClassLoader(
                    new URL[] { jarFile.toURI().toURL() },
                    this.getClass().getClassLoader()
            );
            String fqClassName = PLUGIN_PACKAGE + name;
            Class<? extends StringProcessingPlugin> clazz = (Class<? extends StringProcessingPlugin>)Class.forName(
                    fqClassName, true, child);
            plugins.put(name, clazz.getConstructor().newInstance());
        } catch (Exception e) {
            System.out.println("ERROR INITIALIZING PLUGIN " + name);
            e.printStackTrace(System.out);
        }
    }

}
