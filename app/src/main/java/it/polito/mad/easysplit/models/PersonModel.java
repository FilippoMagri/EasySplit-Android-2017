package it.polito.mad.easysplit.models;

import java.util.List;
import java.util.Set;

public interface PersonModel extends Observable {
    String getIdentifier();
    Set<GroupModel> getMemberships();
    List<ExpenseModel> getExpenses();
}
