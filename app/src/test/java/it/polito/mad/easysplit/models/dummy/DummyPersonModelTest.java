package it.polito.mad.easysplit.models.dummy;

import junit.framework.Assert;

import org.junit.Test;

import java.util.List;

public class DummyPersonModelTest {

    @Test
    public void getMemberships() throws Exception {
        DummyGroupIdentity group = new DummyGroupIdentity();
        for (PersonState person : group.getMembers())
            Assert.assertTrue(person.getMemberships().contains(group));
    }

    @Test
    public void getTransactions() throws Exception {
        DummyGroupIdentity group = new DummyGroupIdentity();
        for (PersonState person : group.getMembers()) {
            List<ExpenseState> personTxs = person.getExpenses();

            int found = 0;
            for (ExpenseState tx : group.getExpenses()) {
                if (tx.getPayer() != person)
                    continue;

                Assert.assertTrue(personTxs.indexOf(tx) != -1);
                found++;
            }

            Assert.assertEquals(found, personTxs.size());
        }
    }

}