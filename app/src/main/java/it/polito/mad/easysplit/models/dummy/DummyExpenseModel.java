package it.polito.mad.easysplit.models.dummy;

import java.util.Calendar;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.ExpenseModel;

public class DummyExpenseModel extends ObservableBase implements ExpenseModel {
    private Calendar timestamp;
    private Money amount;
    private PersonModel payer;
    private GroupModel group;
    private String name;

    public DummyExpenseModel(String name, Calendar timestamp, Money amount, PersonModel payer, GroupModel group) {
        this.name = name;
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

    @Override
    public String getName() { return name; }

}
