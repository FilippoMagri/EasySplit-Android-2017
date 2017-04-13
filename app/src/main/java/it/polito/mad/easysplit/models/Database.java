package it.polito.mad.easysplit.models;

import android.net.Uri;
import android.support.annotation.Nullable;


public interface Database {
    /// @brief Get the current logged-in user. All accessible data objects should be accessed
    ///        through the returned PersonModel instance.
    /// @return The PersonModel representing the user, or null if the user isn't logged in.
    @Nullable PersonModel getUser();

    /// @brief Get the object corresponding to the given content:// URI.  The correct class for the
    ///        desired object must be specified; results are undefined otherwise.
    /// @return The object corresponding to the given URI.
    <T> T findByUri(Uri data, Class<T> cls);
}
