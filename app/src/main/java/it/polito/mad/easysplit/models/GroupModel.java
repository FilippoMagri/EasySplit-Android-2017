package it.polito.mad.easysplit.models;

import android.support.annotation.NonNull;

import java.util.List;

public interface GroupModel extends DataModel {
    @NonNull String getName();
    void setName(@NonNull String name) throws ConstraintException;

    /// @brief Return the list of members of the group.  The returned list shouldn't be modified.
    List<PersonModel> getMembers();
    void addMember(PersonModel person);
    void removeMember(PersonModel person);
    PersonModel getMember (String id);

    List<ExpenseModel> getExpenses();
    void addExpense(ExpenseModel expense) throws ConstraintException;
    void removeExpense(ExpenseModel expense) throws ConstraintException;

    String toJSON();

    GroupBalanceModel getBalance();
}
