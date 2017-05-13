package it.polito.mad.easysplit;

import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public final class Utils {
    private final static String CANONICAL_HOSTNAME = "it.polito.mad.easysplit";

    public enum UriType {
        GROUP, EXPENSE, USER;

        @Nullable
        String getTag() {
            switch (this) {
                case USER:
                    return "users";
                case EXPENSE:
                    return "expenses";
                case GROUP:
                    return "groups";
            }
            return null;
        }
    }

    @Nullable
    static List<String> splitUri(Uri uri) {
        if (! uri.getScheme().equals("content") ||
                ! uri.getHost().equals(CANONICAL_HOSTNAME))
            return null;

        List<String> path = uri.getPathSegments();
        if (path.size() < 2)
            return null;

        return path;
    }

    public static DatabaseReference findByUri(Uri uri) {
        return findByUri(uri, FirebaseDatabase.getInstance().getReference());
    }

    public static DatabaseReference findByUri(Uri uri, DatabaseReference root) {
        List<String> path = splitUri(uri);
        if (path == null)
            return null;

        String tag = path.get(0);
        String id = path.get(1);
        return root.child(tag).child(id);
    }

    public static Uri getUriFor(UriType type, String id) {
        String basePath = type.getTag();
        if (basePath == null)
            return null;

        return new Uri.Builder().scheme("content")
                .authority(CANONICAL_HOSTNAME)
                .appendPath(basePath).appendPath(id)
                .build();
    }

    public static String getPathFor(UriType uriType, String id) {
        return getPathFor(getUriFor(uriType, id));
    }

    public static String getPathFor(Uri uri) {
        List<String> path = splitUri(uri);
        return path == null ? null : TextUtils.join("/", path);
    }

    public static String getIdFor(UriType uriType, Uri uri) {
        List<String> path = splitUri(uri);
        if (path == null || uriType.getTag() != path.get(0))
            return null;
        return path.get(1);
    }

    private Utils() { }
}
