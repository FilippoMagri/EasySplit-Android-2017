package it.polito.mad.easysplit.models;

import java.util.Calendar;
import java.util.List;

public interface ExpenseModel extends DataModel, Amountable {
    String getName();
    Calendar getTimestamp();
    Money getAmount();
    PersonModel getPayer();
    GroupModel getGroup();
    List<PersonModel> getParticipants();
}
