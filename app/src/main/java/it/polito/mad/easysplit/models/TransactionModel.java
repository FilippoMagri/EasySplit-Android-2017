package it.polito.mad.easysplit.models;

import java.util.Calendar;

public interface TransactionModel extends Observable {
    Calendar getTimestamp();
    Money getAmount();
    PersonModel getPayer();
    GroupModel getGroup();
}
