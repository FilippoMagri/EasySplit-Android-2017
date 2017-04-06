package it.polito.mad.easysplit.models.dummy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.ExpenseModel;

public class DummyPersonModel extends ObservableBase implements PersonModel {
    private String name;
    private GroupModel group;

    public DummyPersonModel(String name, GroupModel group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public String getIdentifier() {
        return name;
    }

    @Override
    public Set<GroupModel> getMemberships() {
        HashSet<GroupModel> ret = new HashSet<>();
        ret.add(group);
        return ret;
    }

    @Override
    public List<ExpenseModel> getExpenses() {
        ArrayList<ExpenseModel> filtered = new ArrayList<>();
        for (ExpenseModel tx : group.getExpenses()) {
            if (tx.getPayer() == this)
                filtered.add(tx);
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

        return getIdentifier().equals(((DummyPersonModel)other).getIdentifier()) && this.group.equals(((DummyPersonModel)other).group);
    }
}
