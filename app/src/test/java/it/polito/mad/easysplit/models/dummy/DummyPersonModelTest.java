package it.polito.mad.easysplit.models.dummy;

import junit.framework.Assert;

import org.junit.Test;

import java.util.List;

import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.TransactionModel;

import static org.junit.Assert.*;

public class DummyPersonModelTest {

    @Test
    public void getMemberships() throws Exception {
        DummyGroupModel group = new DummyGroupModel();
        for (PersonModel person : group.getMembers())
            Assert.assertTrue(person.getMemberships().contains(group));
    }

    @Test
    public void getTransactions() throws Exception {
        DummyGroupModel group = new DummyGroupModel();
        for (PersonModel person : group.getMembers()) {
            List<TransactionModel> personTxs = person.getTransactions();

            int found = 0;
            for (TransactionModel tx : group.getTransactionList()) {
                if (tx.getPayer() != person)
                    continue;

                Assert.assertTrue(personTxs.indexOf(tx) != -1);
                found++;
            }

            Assert.assertEquals(found, personTxs.size());
        }
    }

}