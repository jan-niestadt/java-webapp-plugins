package org.ivdnt.test;

import java.util.ServiceLoader;

interface ServiceLoaderFactory<T> {
    ServiceLoader<T> serviceLoader(ClassLoader classLoader);
}
