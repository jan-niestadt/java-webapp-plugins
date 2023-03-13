# java-webapp-plugins

Demo of a Java web application with hot reloadable plugins.

May be useful for BlackLab at some point.

See https://webcache.googleusercontent.com/search?q=cache:kRDXk4QofpoJ:https://www.mulesoft.com/tomcat-classpath&cd=1&hl=nl&ct=clnk&gl=nl

(Tomcat has different ClassLoaders; use shared class loader (of create new URLClassLoader...?) and use that with ServiceLoader.load() in PluginManager?)
