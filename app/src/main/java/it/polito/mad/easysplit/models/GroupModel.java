package it.polito.mad.easysplit.models;

import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public interface GroupModel extends Observable {
    String getName();
    void setName(String name) throws ConstraintException;

    /// @brief Return the list of members of the group.  The returned list shouldn't be modified.
    List<PersonModel> getMembers();
    void addMember(PersonModel person) throws ConstraintException;
    void removeMember(PersonModel person) throws ConstraintException;
    PersonModel getMember (String id);

    List<ExpenseModel> getExpenses();
    void addExpense(ExpenseModel expense) throws ConstraintException;
    void removeExpense(ExpenseModel expense) throws ConstraintException;

    String toJSON();

    void writeIntoJsonFile(File fileDir,String nameFile) throws IOException;
    void readFromJsonFile(File fileDir,String nameFile) throws FileNotFoundException, IOException;

    GroupBalanceModel getBalance();
}
