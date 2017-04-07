package it.polito.mad.easysplit.models.dummy;

import java.util.Calendar;
import java.util.List;

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
    private List<PersonModel> participants;

    public DummyExpenseModel(String name, Calendar timestamp, Money amount, PersonModel payer, GroupModel group, List<PersonModel> participants) {
        this.name = name;
        this.timestamp = timestamp;
        this.amount = amount;
        this.payer = payer;
        this.group = group;
        this.participants = participants;
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
    public List<PersonModel> getParticipants() { return participants; }

    @Override
    public String getName() { return name; }

}
