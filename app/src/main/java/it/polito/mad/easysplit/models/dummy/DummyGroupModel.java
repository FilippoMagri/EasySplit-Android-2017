package it.polito.mad.easysplit.models.dummy;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;
import it.polito.mad.easysplit.models.ExpenseModel;

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
    private ArrayList<ExpenseModel> expenses;

    public DummyGroupModel() {
        members = new HashSet<>();
        expenses = new ArrayList<>();

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        int numMembers = 1 + rand.nextInt(10);
        for (int i = 0; i < numMembers; i++) {
            members.add(new DummyPersonModel(randomName(rand), this));
        }

        int numExps = 5 + rand.nextInt(10);
        addSomeExpenses(rand, numExps);
    }

    public void addSomeExpenses(int numExps) {
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        addSomeExpenses(rand, numExps);
    }

    void addSomeExpenses(Random rand, int numExps) {
        Calendar time = new GregorianCalendar();
        Currency currency = Currency.getInstance("EUR");
        Object people[] = members.toArray();

        for (int i = 0; i < numExps; i++) {
            int personIndex = Math.abs(rand.nextInt(members.size()));

            time.add(Calendar.DAY_OF_MONTH, Math.abs(rand.nextInt(10)));
            DummyExpenseModel exp = new DummyExpenseModel(
                    (Calendar) time.clone(),
                    new Money(currency, (long) rand.nextInt(10000)),
                    (PersonModel) people[personIndex],
                    this);
            expenses.add(exp);
        }

        notifyChanged();
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
    public List<ExpenseModel> getExpenses() {
        return expenses;
    }
}
