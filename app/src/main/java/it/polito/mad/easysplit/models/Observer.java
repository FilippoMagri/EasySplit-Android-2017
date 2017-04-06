package it.polito.mad.easysplit.models;

public interface Observer {
    void onChanged();
    void onInvalidated();
}
