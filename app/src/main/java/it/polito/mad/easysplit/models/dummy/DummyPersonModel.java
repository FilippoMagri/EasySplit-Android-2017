package it.polito.mad.easysplit.models.dummy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.TransactionModel;

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
    public List<TransactionModel> getTransactions() {
        ArrayList<TransactionModel> filtered = new ArrayList<>();
        for (TransactionModel tx : group.getTransactionList()) {
            if (tx.getPayer() == this)
                filtered.add(tx);
        }
        return filtered;
    }
}
