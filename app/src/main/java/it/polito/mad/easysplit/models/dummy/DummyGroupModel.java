package it.polito.mad.easysplit.models.dummy;

import android.database.DataSetObserver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.TransactionModel;

public class DummyGroupModel extends ObservableBase implements GroupModel  {
    private static String NAMES[] = {
            "Marco", "Filippo", "Sebastiano", "Camille", "Flavio", "Anna",
            "Saro", "Stefano", "Pasquale"
    };
    private static String SURNAMES[] = {
            "Falsaperla", "Rossi", "Passalacqua", "Pulvirenti", "Barrera", "Magrì",
            "Giobergia", "Zappalà"
    };
    private static String randomName(Random rand) {
        int nameIndex = rand.nextInt(NAMES.length);
        int surnameIndex = rand.nextInt(SURNAMES.length);
        return NAMES[nameIndex] + " " + SURNAMES[surnameIndex];
    }

    private HashSet<PersonModel> members;
    private ArrayList<TransactionModel> transactions;

    public DummyGroupModel() {
        members = new HashSet<>();
        transactions = new ArrayList<>();

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        int numMembers = rand.nextInt(10);
        DummyPersonModel[] people = new DummyPersonModel[numMembers];
        for (int i=0; i < numMembers; i++) {
            people[i] = new DummyPersonModel(randomName(rand), this);
            members.add(people[i]);
        }

        int numTxs = rand.nextInt(100);
        for (int i=0; i < numTxs; i++) {
            Calendar time = new GregorianCalendar();
            Currency currency = Currency.getInstance("EUR");

            for(int j=0; j < 10; j++) {
                int personIndex = Math.abs(rand.nextInt(members.size()));

                time.add(Calendar.DAY_OF_MONTH, Math.abs(rand.nextInt(10)));
                DummyTransactionModel tx = new DummyTransactionModel(
                        (Calendar) time.clone(),
                        new Money(currency, (long) rand.nextInt(10000)),
                        people[personIndex],
                        this);
                transactions.add(tx);
            }
        }
    }

    @Override
    public String getName() {
        return "Gruppo MAD";
    }

    @Override
    public Set<PersonModel> getMembers() {
        return members;
    }

    @Override
    public List<TransactionModel> getTransactionList() {
        return transactions;
    }
}
