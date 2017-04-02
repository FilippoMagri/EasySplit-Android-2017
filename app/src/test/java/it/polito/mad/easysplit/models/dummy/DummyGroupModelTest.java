package it.polito.mad.easysplit.models.dummy;

import junit.framework.Assert;

import org.junit.Test;

import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.ExpenseModel;

public class DummyGroupModelTest {
    @Test
    public void membersAreConsistent() throws Exception {
        DummyGroupModel group = new DummyGroupModel();
        for (PersonModel person : group.getMembers())
            Assert.assertTrue(person.getMemberships().contains(group));
    }

    @Test
    public void getTransactionList() throws Exception {
        DummyGroupModel group = new DummyGroupModel();
        for (ExpenseModel tx : group.getExpenses())
            Assert.assertEquals(tx.getGroup(), group);
    }
}