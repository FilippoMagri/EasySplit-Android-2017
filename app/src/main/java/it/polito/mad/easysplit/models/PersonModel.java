package it.polito.mad.easysplit.models;

import android.graphics.drawable.Drawable;

import java.util.List;
import java.util.Set;

public interface PersonModel extends Observable {
    String getIdentifier();

    String getName();
    Drawable getProfilePicture();
    Set<GroupModel> getMemberships();
    List<ExpenseModel> getExpenses();
    String toJSON();
    PersonModel fromJSON();
}
