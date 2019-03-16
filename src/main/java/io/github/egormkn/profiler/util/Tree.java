package io.github.egormkn.profiler.util;

import java.util.ArrayList;
import java.util.List;

public class Tree<T> {
    private final List<Tree<T>> children = new ArrayList<>();
    private final T content;

    public Tree(T content) {
        this.content = content;
    }

    public Tree<T> add(Tree<T> child) {
        children.add(child);
        return child;
    }

    public List<Tree<T>> getChildren() {
        return children;
    }

    public T getContent() {
        return content;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}
