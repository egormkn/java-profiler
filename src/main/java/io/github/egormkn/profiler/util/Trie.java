package io.github.egormkn.profiler.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Trie<T> {

    private final Map<T, Trie<T>> children = new TreeMap<>();
    private final T content;

    public Trie(T content) {
        this.content = content;
    }

    public Trie<T> add(T content) {
        Trie<T> trie = new Trie<>(content);
        return children.computeIfAbsent(content, key -> trie);
    }

    public Trie<T> add(List<T> contents) {
        Trie<T> node = this;
        for (T content : contents) {
            node = node.add(content);
        }
        return node;
    }

    public Trie<T> get(T content) {
        return children.get(content);
    }

    public Trie<T> get(List<T> contents) {
        Trie<T> node = this;
        for (T content : contents) {
            node = node.get(content);
            if (node == null) break;
        }
        return node;
    }

    public Map<T, Trie<T>> getChildren() {
        return Collections.unmodifiableMap(children);
    }

    public T getContent() {
        return content;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }
}