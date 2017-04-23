package it.polito.mad.easysplit.models.dummy;

import junit.framework.Assert;

import org.junit.Test;

public class DummyGroupModelTest {
    @Test
    public void membersAreConsistent() throws Exception {
        DummyGroupIdentity group = new DummyGroupIdentity();
        for (PersonState person : group.getMembers())
            Assert.assertTrue(person.getMemberships().contains(group));
    }

    @Test
    public void getTransactionList() throws Exception {
        DummyGroupIdentity group = new DummyGroupIdentity();
        for (ExpenseState tx : group.getExpenses())
            Assert.assertEquals(tx.getGroup(), group);
    }
}