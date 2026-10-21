package com.example.java26.budget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;




public class Repository<T> {
    private static final Logger LOG = Logger.getLogger(Repository.class.getName());
    private final List<T> items = new ArrayList<>();

    public void add(T item) {
        Objects.requireNonNull(item, "item får ej vara null");
        items.add(item);
        LOG.fine(() -> "Lade till objekt: " + item + " (antal nu: " + items.size() + ")");
    }

    public void addAll(Collection<? extends T> newItems) {
        Objects.requireNonNull(newItems, "newItems får ej vara null");
        for (T item : newItems) {
            Objects.requireNonNull(item, "samlingen får ej innehålla null");
        }
        items.addAll(newItems);
        LOG.fine(() -> "Lade till " + newItems.size() + " objekt (antal nu: " + items.size() + ")");
    }
    public boolean remove(T item) {
        boolean removed = items.remove(item);
        LOG.fine(() -> "Ta bort " + item + ": " + (removed ? "borttagen" : "fanns inte"));
        return removed;
    }
    public List<T> findAll() {
        return List.copyOf(items);
    }


    public List<T> findWhere(Predicate<? super T> condition) {
        Objects.requireNonNull(condition, "condition får ej vara null");
        return items.stream().filter(condition).toList();
    }
    public int size() {
        return items.size();
    }
    public boolean isEmpty() {
        return items.isEmpty();
    }
    public void clear() {
        items.clear();
        LOG.fine("Repository tomt");
    }


}
