package it.polito.mad.easysplit.models.dummy;

import java.util.Calendar;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.TransactionModel;

public class DummyTransactionModel extends ObservableBase implements TransactionModel {
    private Calendar timestamp;
    private Money amount;
    private PersonModel payer;
    private GroupModel group;

    public DummyTransactionModel(Calendar timestamp, Money amount, PersonModel payer, GroupModel group) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.payer = payer;
        this.group = group;
    }

    @Override
    public Calendar getTimestamp() {
        return timestamp;
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    @Override
    public PersonModel getPayer() {
        return payer;
    }

    @Override
    public GroupModel getGroup() {
        return group;
    }
}
