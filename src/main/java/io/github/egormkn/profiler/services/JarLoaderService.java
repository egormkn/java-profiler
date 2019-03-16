package io.github.egormkn.profiler.services;

import io.github.egormkn.profiler.util.Trie;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class JarLoaderService extends Service<JarLoaderService.Result> {

    private ObjectProperty<File> file = new SimpleObjectProperty<>();

    @Override
    protected Task<JarLoaderService.Result> createTask() {
        return new LoaderTask(getFile());
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

    public static class Result {

        private final String mainClass;

        private final Trie<String> packages;

        public Result(String mainClass, Trie<String> packages) {
            this.mainClass = mainClass;
            this.packages = packages;
        }

        public String getMainClass() {
            return mainClass;
        }

        public Trie<String> getPackages() {
            return packages;
        }
    }

    public static class LoaderTask extends Task<Result> {

        private static final String MAIN_CLASS = "Main-Class";
        private static final String ZIP_SEPARATOR = "/";

        private final File file;

        public LoaderTask(File file) {
            this.file = file;
        }

        @Override
        protected Result call() throws IOException {
            JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file));

            Attributes mainAttributes = jarInputStream.getManifest().getMainAttributes();
            String mainClass = mainAttributes.getValue(MAIN_CLASS);

            Trie<String> packages = new Trie<>("All packages");

            JarEntry jarEntry;
            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                String className = jarEntry.getName();
                if (className.endsWith(".class")) {
                    List<String> path = Arrays.asList(jarEntry.getName().split(ZIP_SEPARATOR));
                    packages.add(path);
                }
            }

            return new Result(mainClass, packages);
        }
    }
}
