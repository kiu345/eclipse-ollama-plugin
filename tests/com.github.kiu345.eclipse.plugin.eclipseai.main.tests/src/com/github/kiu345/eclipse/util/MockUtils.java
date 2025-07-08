package com.github.kiu345.eclipse.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;

public class MockUtils {
    public static ILog createLogMock() {
        return new ILog() {
            @Override
            public void removeLogListener(ILogListener listener) {
                // FAKE NOOP
            }

            @Override
            public void log(IStatus status) {
                // FAKE NOOP
            }

            @Override
            public Bundle getBundle() {
                // FAKE NOOP
                return new Bundle() {

                    @Override
                    public int compareTo(Bundle o) {
                        return 0;
                    }

                    @Override
                    public void update(InputStream input) throws BundleException {
                        // FAKE NOOP
                    }

                    @Override
                    public void update() throws BundleException {
                        // FAKE NOOP

                    }

                    @Override
                    public void uninstall() throws BundleException {
                        // FAKE NOOP

                    }

                    @Override
                    public void stop(int options) throws BundleException {
                        // FAKE NOOP
                    }

                    @Override
                    public void stop() throws BundleException {
                        // FAKE NOOP

                    }

                    @Override
                    public void start(int options) throws BundleException {
                        // FAKE NOOP

                    }

                    @Override
                    public void start() throws BundleException {
                        // FAKE NOOP
                    }

                    @Override
                    public Class<?> loadClass(String name) throws ClassNotFoundException {
                        return null;
                    }

                    @Override
                    public boolean hasPermission(Object permission) {
                        return false;
                    }

                    @Override
                    public Version getVersion() {
                        return null;
                    }

                    @Override
                    public String getSymbolicName() {
                        return "test";
                    }

                    @Override
                    public int getState() {
                        // FAKE NOOP
                        return 0;
                    }

                    @Override
                    public Map<X509Certificate, List<X509Certificate>> getSignerCertificates(int signersType) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public ServiceReference<?>[] getServicesInUse() {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public Enumeration<URL> getResources(String name) throws IOException {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public URL getResource(String name) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public ServiceReference<?>[] getRegisteredServices() {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public String getLocation() {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public long getLastModified() {
                        // FAKE NOOP
                        return 0;
                    }

                    @Override
                    public Dictionary<String, String> getHeaders(String locale) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public Dictionary<String, String> getHeaders() {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public Enumeration<String> getEntryPaths(String path) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public URL getEntry(String path) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public File getDataFile(String filename) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public long getBundleId() {
                        // FAKE NOOP
                        return 0;
                    }

                    @Override
                    public BundleContext getBundleContext() {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public Enumeration<URL> findEntries(String path, String filePattern, boolean recurse) {
                        // FAKE NOOP
                        return null;
                    }

                    @Override
                    public <A> A adapt(Class<A> type) {
                        // FAKE NOOP
                        return null;
                    }
                };
            }

            @Override
            public void addLogListener(ILogListener listener) {
                // FAKE NOOP
            }
        };
    }
}
