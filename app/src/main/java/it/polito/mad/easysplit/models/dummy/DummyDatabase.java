package it.polito.mad.easysplit.models.dummy;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import it.polito.mad.easysplit.models.Database;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;

/**
 * Created by sebastiano on 09/04/17.
 */

public class DummyDatabase implements Database {
    private PersonModel mUser;
    private HashMap<String, GroupModel> mGroups = new HashMap<>();

    public DummyDatabase() {
        GroupModel group = new DummyGroupModel();
        addGroup(group);

        Random rand = new Random(System.currentTimeMillis());
        mUser = group.getMembers().get(rand.nextInt(group.getMembers().size()));
    }

    private void addGroup(GroupModel group) {
        mGroups.put(group.getIdentifier(), group);
    }

    @Nullable
    @Override
    public PersonModel getUser() {
        return mUser;
    }

    private boolean isUriFor(String basePath, Class<? extends DummyDataModel> cls) {
        return DummyDataModel.getUriBasePath(cls).equals(basePath);
    }

    @Override
    public <T> T findByUri(Uri uri, Class<T> retCls) {
        if (! uri.getScheme().equals("content"))
            return null;

        if (! uri.getHost().equals("it.polito.mad.easysplit"))
            return null;

        List<String> path = uri.getPathSegments();
        String basePath = path.get(0);
        String id = path.get(1);

        if (basePath == null || id == null)
            return null;

        if (isUriFor(basePath, DummyGroupModel.class)) {
            if (retCls.isAssignableFrom(GroupModel.class))
                return (T) mGroups.get(id);
        } else if (isUriFor(basePath, DummyPersonModel.class)) {
            if (retCls.isAssignableFrom(PersonModel.class)) {
                for (GroupModel group : mGroups.values()) {
                    PersonModel person = group.getMember(id);
                    if (person != null)
                        return (T) person;
                }
            }
        }

        return null;
    }
}
