package it.polito.mad.easysplit.models;

import java.util.List;
import java.util.Set;

public interface GroupModel extends Observable {
    String getName();
    Set<PersonModel> getMembers();
    List<TransactionModel> getTransactionList();
}
