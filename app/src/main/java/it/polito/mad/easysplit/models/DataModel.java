package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.support.annotation.Nullable;

/// @brief A data model object that has a Uri and can be saved to/retrieved from a database.
public interface DataModel extends Observable {
    /// @brief Get the database ID for this object.  May return null if the object hasn't been
    ///        saved in the database yet.  Call save() to write to the database and acquire an ID.
    @Nullable String getIdentifier();
    Uri getUri();
    void save();
}
