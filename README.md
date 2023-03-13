# java-webapp-plugins

Demo of a Java web application with hot reloadable plugins.

May be useful for BlackLab at some point.

See https://www.mulesoft.com/tcat/tomcat-classpath

(Tomcat has different ClassLoaders; use shared class loader (of create new URLClassLoader...?) and use that with ServiceLoader.load() in PluginManager?)
