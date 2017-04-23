package it.polito.mad.easysplit;

import android.net.Uri;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public final class Utils {
    private final static String CANONICAL_HOSTNAME = "it.polito.mad.easysplit";

    public enum UriType {
        GROUP, EXPENSE, USER
    }

    public static DatabaseReference findByUri(Uri uri) {
        return findByUri(uri, FirebaseDatabase.getInstance().getReference());
    }

    public static DatabaseReference findByUri(Uri uri, DatabaseReference root) {
        if (! uri.getScheme().equals("content"))
            return null;

        if (! uri.getHost().equals(CANONICAL_HOSTNAME))
            return null;

        List<String> path = uri.getPathSegments();
        if (path.size() < 2)
            return null;

        String tag = path.get(0);
        String id = path.get(1);

        if (tag == null || id == null)
            return null;

        return root.child(tag).child(id);
    }

    public static Uri getUriFor(UriType type, String id) {
        String basePath;

        if (type == UriType.USER)
            basePath = "users";
        else if (type == UriType.EXPENSE)
            basePath = "expenses";
        else if (type == UriType.GROUP)
            basePath = "groups";
        else
            return null;

        return new Uri.Builder().scheme("content")
                .authority(CANONICAL_HOSTNAME)
                .appendPath(basePath).appendPath(id)
                .build();
    }

    private Utils() { }
}
