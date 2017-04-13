package it.polito.mad.easysplit.models.dummy;

import android.support.annotation.Nullable;

import java.util.Calendar;
import java.util.List;

import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.PersonModel;

public class DummyExpenseModel extends DummyDataModel implements ExpenseModel {
    private Calendar timestamp;
    private Money amount;
    private PersonModel payer;
    private GroupModel group;
    private String name;
    private List<PersonModel> participants;
    private String mId = nextId();

    private static long LAST_ID = 1;
    private static String nextId() {
        LAST_ID++;
        return Long.toString(LAST_ID);
    }

    public DummyExpenseModel(String name, Calendar timestamp, Money amount, PersonModel payer, GroupModel group, List<PersonModel> participants) {
        this.name = name;
        this.timestamp = timestamp;
        this.amount = amount;
        this.payer = payer;
        this.group = group;
        this.participants = participants;
    }

    @Nullable
    @Override
    public String getIdentifier() {
        return mId;
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
