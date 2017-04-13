package it.polito.mad.easysplit.models.dummy;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;

@DummyDataModel.UriPath("people")
public class DummyPersonModel extends DummyDataModel implements PersonModel {
    private String mName;
    private HashSet<GroupModel> mGroups = new HashSet<>();

    public DummyPersonModel(String name) {
        mName = name;
    }

    @Override
    public String getIdentifier() {
        return mName;
    }

    public String getName() {
        return mName;
    }

    @Override
    public Drawable getProfilePicture() {
        /// TODO Store and provide profile picture
        return null;
    }

    @Override
    public Set<GroupModel> getMemberships() {
        return mGroups;
    }

    @Override
    public void addGroup(GroupModel group) {
        mGroups.add(group);
        group.addMember(this);
    }

    @Override
    public List<ExpenseModel> getExpenses() {
        ArrayList<ExpenseModel> filtered = new ArrayList<>();
        for (GroupModel group : mGroups) {
            for (ExpenseModel tx : group.getExpenses()) {
                if (tx.getPayer() == this)
                    filtered.add(tx);
            }
        }
        return filtered;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }

        if (!(other instanceof DummyPersonModel)) {
            return false;
        }

        return getIdentifier().equals(((DummyPersonModel)other).getIdentifier()) && this.mGroups.equals(((DummyPersonModel)other).mGroups);
    }
}
