package it.polito.mad.easysplit.models;

import java.util.WeakHashMap;

public class ObservableBase implements Observable {
    private WeakHashMap<Observer, Boolean> observers = new WeakHashMap<>();

    @Override
    public void registerObserver(Observer observer) {
        observers.put(observer, true);
    }

    @Override
    public void unregisterObserver(Observer observer) {
        observers.remove(observer);
    }

    protected void notifyChanged() {
        for (Observer observer : observers.keySet())
            observer.onChanged();
    }

    protected void notifyInvalidated() {
        for (Observer observer : observers.keySet())
            observer.onInvalidated();
    }
}