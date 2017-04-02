package it.polito.mad.easysplit.models;

import android.database.DataSetObserver;

import java.util.HashSet;

public class ObservableBase implements Observable {
    private HashSet<DataSetObserver> observers = new HashSet<>();

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        observers.remove(observer);
    }

    protected void notifyChanged() {
        for (DataSetObserver observer : observers)
            observer.onChanged();
    }

    protected void notifyInvalidated() {
        for (DataSetObserver observer : observers)
            observer.onInvalidated();
    }
}
