package it.polito.mad.easysplit.models;

import java.util.HashSet;

public class ObservableBase implements Observable {
    private HashSet<Observer> observers = new HashSet<>();

    @Override
    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterObserver(Observer observer) {
        observers.remove(observer);
    }

    protected void notifyChanged() {
        for (Observer observer : observers)
            observer.onChanged();
    }

    protected void notifyInvalidated() {
        for (Observer observer : observers)
            observer.onInvalidated();
    }
}