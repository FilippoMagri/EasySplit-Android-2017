package it.polito.mad.easysplit.models.dummy;

import android.net.Uri;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import it.polito.mad.easysplit.models.DataModel;
import it.polito.mad.easysplit.models.ObservableBase;


abstract class DummyDataModel extends ObservableBase implements DataModel {
    @Override
    public void save() {
        // Do nothing.
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface UriPath {
        String value();
    }

    public static String getUriBasePath(Class<? extends DummyDataModel> cls) {
        UriPath annot = cls.getAnnotation(UriPath.class);
        return annot != null ? annot.value() : "stuff";
    }

    @Override
    public Uri getUri() {
        String basePath = getUriBasePath(getClass());
        return Uri.parse(String.format("content://it.polito.mad.easysplit/%s/%s",
                Uri.encode(basePath),
                Uri.encode(getIdentifier())));
    }
}
