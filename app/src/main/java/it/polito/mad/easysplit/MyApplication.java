package it.polito.mad.easysplit;

import android.app.Application;

import it.polito.mad.easysplit.models.ExpenseModel;

/**
 * Created by fgiobergia on 06/04/17.
 */

public class MyApplication extends Application {
    private ExpenseModel currentExpense;

    public ExpenseModel getCurrentExpense() {
        return currentExpense;
    }

    public void setCurrentExpense(ExpenseModel currentExpense) {
        this.currentExpense = currentExpense;
    }
}