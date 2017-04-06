package it.polito.mad.easysplit;

import android.app.Application;

import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;

/**
 * Created by fgiobergia on 06/04/17.
 */

public class MyApplication extends Application {
    private GroupModel groupModel;
    private ExpenseModel currentExpense;

    public ExpenseModel getCurrentExpense() {
        return currentExpense;
    }

    public void setCurrentExpense(ExpenseModel currentExpense) {
        this.currentExpense = currentExpense;
    }

    public GroupModel getGroupModel() {
        return groupModel;
    }

    public void setGroupModel(GroupModel groupModel) {
        this.groupModel = groupModel;
    }
}