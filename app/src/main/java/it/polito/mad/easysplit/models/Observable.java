package it.polito.mad.easysplit.models;

import android.database.DataSetObserver;

/// @brief An Android-compatible Observable object that notifies DataSetObservable instances.
public interface Observable {
    /// @brief Register an observer that is called when changes happen to the data used by this adapter.
    void registerDataSetObserver(DataSetObserver observer);

    /// @brief Unregister an observer that has previously been registered with this adapter via registerDataSetObserver(DataSetObserver).
    void unregisterDataSetObserver(DataSetObserver observer);
}