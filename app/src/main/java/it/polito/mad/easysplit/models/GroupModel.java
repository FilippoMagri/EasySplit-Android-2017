package it.polito.mad.easysplit.models;

import java.util.List;
import java.util.Set;

public interface GroupModel extends Observable {
    String getName();
    void setName(String name) throws ConstraintException;

    /// @brief Return the list of members of the group.  The returned list shouldn't be modified.
    List<PersonModel> getMembers();
    void addMember(PersonModel person) throws ConstraintException;
    void removeMember(PersonModel person) throws ConstraintException;

    List<ExpenseModel> getExpenses();
    void addExpense(ExpenseModel expense) throws ConstraintException;
    void removeExpense(ExpenseModel expense) throws ConstraintException;
    String toJSON();
}
