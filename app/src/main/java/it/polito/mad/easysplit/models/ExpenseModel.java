package it.polito.mad.easysplit.models;

import java.util.Calendar;

public interface ExpenseModel extends Observable {
    Calendar getTimestamp();
    Money getAmount();
    PersonModel getPayer();
    GroupModel getGroup();
}
