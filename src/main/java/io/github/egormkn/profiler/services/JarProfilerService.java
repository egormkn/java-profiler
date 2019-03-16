package io.github.egormkn.profiler.services;

import javafx.beans.property.*;
import javafx.collections.ObservableSet;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.aspectj.weaver.loadtime.WeavingURLClassLoader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Permission;
import java.util.Set;

public class JarProfilerService extends Service<Void> {

    private ObjectProperty<File> file = new SimpleObjectProperty<>();

    private StringProperty args = new SimpleStringProperty();

    private SetProperty<String> packages = new SimpleSetProperty<>();

    private StringProperty mainClass = new SimpleStringProperty();

    @Override
    protected Task<Void> createTask() {
        return new ProfilerTask(getFile(), getArgs(), getPackages(), getMainClass());
    }

    public ObservableSet<String> getPackages() {
        return packages.get();
    }

    public void setPackages(ObservableSet<String> packages) {
        this.packages.set(packages);
    }

    public SetProperty<String> packagesProperty() {
        return packages;
    }

    public String getMainClass() {
        return mainClass.get();
    }

    public void setMainClass(String mainClass) {
        this.mainClass.set(mainClass);
    }

    public StringProperty mainClassProperty() {
        return mainClass;
    }

    public File getFile() {
        return file.get();
    }

    public void setFile(File file) {
        this.file.set(file);
    }

    public ObjectProperty<File> fileProperty() {
        return file;
    }

    public String getArgs() {
        return args.get();
    }

    public void setArgs(String args) {
        this.args.set(args);
    }

    public StringProperty argsProperty() {
        return args;
    }

    private static class ProfilerTask extends Task<Void> {

        private final File file;

        private final String[] args;

        private final Set<String> packages;

        private final String mainClass;

        private ProfilerTask(File file, String args, Set<String> packages, String mainClass) {
            this.file = file;
            this.args = args.split("\\s+");
            this.packages = packages;
            this.mainClass = mainClass;
        }

        private static void forbidSystemExitCall() {
            SecurityManager securityManager = new SecurityManager() {

                @Override
                public void checkPermission(Permission perm) {
                    // Do anything
                }

                @Override
                public void checkExit(int status) {
                    super.checkExit(status);
                    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                    String className = stackTraceElements[stackTraceElements.length - 1].getClassName();
                    if (!"java.awt.EventDispatchThread".equals(className))
                        throw new SecurityException("System.exit is forbidden for " + className);
                }
            };
            System.setSecurityManager(securityManager);
        }

        @Override
        protected Void call() throws Exception {
            try {
                URL appUrl = file.toURI().toURL();
                URL aspectsUrl = ProfilerTask.class.getProtectionDomain().getCodeSource().getLocation();
                URL[] classURLs = new URL[]{appUrl, aspectsUrl};
                URL[] aspectURLs = new URL[]{aspectsUrl};

                ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
                ClassLoader weavingClassLoader = new WeavingURLClassLoader(classURLs, aspectURLs, defaultClassLoader);
                Thread.currentThread().setContextClassLoader(weavingClassLoader);

                Class<?> app = weavingClassLoader.loadClass(mainClass);
                Method run = app.getMethod("main", String[].class);
                run.invoke(null, (Object) args);

                Thread.currentThread().setContextClassLoader(defaultClassLoader);
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }
}
