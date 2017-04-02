package it.polito.mad.easysplit.models;

import android.database.DataSetObserver;

import org.junit.Assert;
import org.junit.Test;

import it.polito.mad.easysplit.BuildConfig;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;

import static org.junit.Assert.*;

public class GroupBalanceModelTest {
    @Test
    public void recompute() throws Exception {
        DummyGroupModel group = new DummyGroupModel();

        if (BuildConfig.DEBUG) {
            System.err.println("=== Group expenses");
            for (ExpenseModel exp : group.getExpenses())
                System.err.println("  " + exp.getPayer().getIdentifier() + ": " + exp.getAmount());
        }

        GroupBalanceModel balanceModel = new GroupBalanceModel(group);
        testRecomputation(balanceModel);
    }

    private void testRecomputation(GroupBalanceModel balanceModel) {
        Money total = new Money(0);

        if (BuildConfig.DEBUG)
            System.err.println("=== Balance");

        for (PersonModel person : balanceModel.getGroup().getMembers()) {
            Money residue = balanceModel.getResidueFor(person);
            total = total.add(residue);

            if (BuildConfig.DEBUG)
                System.err.println("  " + person.getIdentifier() + ": " + residue);
        }

        Assert.assertTrue(total.getCents() == 0);
    }

    public class TestObserver extends DataSetObserver {
        public boolean changed = false;
        public boolean invalidated = false;

        public void reset() {
            changed = false;
            invalidated = false;
        }

        @Override
        public void onChanged() {
            changed = true;
        }

        @Override
        public void onInvalidated() {
            invalidated = true;
        }
    }

    @Test
    public void observability() throws Exception {
        DummyGroupModel group = new DummyGroupModel();
        GroupBalanceModel balanceModel = new GroupBalanceModel(group);

        TestObserver observer = new TestObserver();
        balanceModel.registerDataSetObserver(observer);

        group.addSomeExpenses(5);
        Assert.assertTrue(observer.changed);

        testRecomputation(balanceModel);
    }
}