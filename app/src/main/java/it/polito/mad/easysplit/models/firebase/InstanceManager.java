package it.polito.mad.easysplit.models.firebase;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

class InstanceManager<T> {
    private HashMap<String, WeakReference<T>> mInstances = new HashMap<>();
    private Class<T> mClass;

    InstanceManager(Class<T> cls) {
        mClass = cls;
    }

    private void cleanup() {
        for (Map.Entry<String, WeakReference<T>> entry : mInstances.entrySet())
            if (entry.getValue().get() == null)
                mInstances.remove(entry.getKey());
    }

    T getInstance(String uid) {
        cleanup();

        WeakReference<T> weakRef = mInstances.get(uid);

        if (weakRef == null || weakRef.get() == null) {
            T ret = null;
            try {
                ret = mClass.getConstructor(String.class).newInstance(uid);
            } catch (Exception e) {
                // The constructor is never supposed to throw
                throw new RuntimeException(e.getLocalizedMessage(), e);
            }

            mInstances.put(uid, new WeakReference<>(ret));
            return ret;
        }

        return weakRef.get();
    }

}
