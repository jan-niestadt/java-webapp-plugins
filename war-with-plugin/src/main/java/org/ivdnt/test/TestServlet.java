package org.ivdnt.test;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TestServlet extends HttpServlet {

    /** Directory to load plugins from */
    public static final File PLUGIN_DIR = new File("/home/jan/int-projects/studiedag/plugins");

    private PluginManager plugins;

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
        for (StringProcessingPlugin plugin: plugins.list()) {
            output.append("- " + plugin.getName() + " (" + plugin.getDescription() + ")\n");
        }
        return output.toString();
    }

    private String process(String pluginName, String input) {
        Optional<StringProcessingPlugin> plugin = plugins.get(pluginName);
        if (plugin.isPresent()) {
            String output = plugin.get().process(input);
            return "PLUGIN " + pluginName + ": " + input + " -> " + output;
        }
        return "PLUGIN NOT LOADED " + pluginName + ": " + input;
    }

    @Override
    public void init() throws ServletException {
        super.init();
        plugins = new PluginManager(PLUGIN_DIR);
    }


}
