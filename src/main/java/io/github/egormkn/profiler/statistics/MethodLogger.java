package io.github.egormkn.profiler.statistics;

import io.github.egormkn.profiler.Controller;
import javafx.application.Platform;
import org.aspectj.lang.Signature;

import java.util.Set;

public class MethodLogger {

    private static MethodLogger instance = new MethodLogger();
    private static int currentLevel = 0;
    private Controller controller;
    private Set<String> selection;

    private MethodLogger() {
    }

    public static MethodLogger getInstance() {
        return instance;
    }

    public void push(Signature signature) {
        if (isFiltered(signature.getDeclaringTypeName())) return;
        final long thread = Thread.currentThread().getId();
        final int level = currentLevel++;
        final long time = System.nanoTime();
        Platform.runLater(() -> controller.startMethod(thread, level, signature, time));
    }

    public void attachController(Controller controller) {
        this.controller = controller;
    }

    public void pop(Signature signature) {
        if (isFiltered(signature.getDeclaringTypeName())) return;
        final long thread = Thread.currentThread().getId();
        final int level = --currentLevel;
        final long time = System.nanoTime();
        Platform.runLater(() -> controller.endMethod(thread, level, signature, time));
    }

    public void setSelection(Set<String> selection) {
        this.selection = selection;
    }

    private boolean isFiltered(String packageName) {
        return selection != null && !selection.contains(packageName);
    }
}
