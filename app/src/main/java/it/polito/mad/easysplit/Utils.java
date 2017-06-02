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
        GROUP, EXPENSE, USER, PAYMENT;

        @Nullable
        String getTag() {
            switch (this) {
                case USER:
                    return "users";
                case EXPENSE:
                    return "expenses";
                case GROUP:
                    return "groups";
                case PAYMENT:
                    return "payments";
            }
            return null;
        }
    }

    public static class InvalidUriException extends RuntimeException {
        private Uri mUri;

        InvalidUriException(Uri uri) {
            super("Invalid content URI");
            mUri = uri;
        }

        public Uri getUri() {
            return mUri;
        }
    }

    @Nullable
    static List<String> splitUri(Uri uri) throws InvalidUriException {
        if (! uri.getScheme().equals("content") ||
                ! uri.getHost().equals(CANONICAL_HOSTNAME))
            throw new InvalidUriException(uri);

        List<String> path = uri.getPathSegments();
        if (path.size() < 2)
            throw new InvalidUriException(uri);

        return path;
    }

    public static DatabaseReference findByUri(Uri uri) {
        return findByUri(uri, FirebaseDatabase.getInstance().getReference());
    }

    public static DatabaseReference findByUri(Uri uri, DatabaseReference root) {
        List<String> path = splitUri(uri);

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
        if (! uriType.getTag().equals(path.get(0)))
            return null;
        return path.get(1);
    }

    private Utils() { }
}
