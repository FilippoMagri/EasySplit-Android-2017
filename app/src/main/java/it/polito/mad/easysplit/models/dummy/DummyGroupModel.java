package it.polito.mad.easysplit.models.dummy;

import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import it.polito.mad.easysplit.models.ConstraintException;
import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.ObservableBase;
import it.polito.mad.easysplit.models.PersonModel;

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

    private static DummyGroupModel sInstance = null;
    public static DummyGroupModel getInstance() {
        if (sInstance == null)
            sInstance = new DummyGroupModel();
        return sInstance;
    }
    private String name;
    private ArrayList<PersonModel> members;
    private ArrayList<ExpenseModel> expenses;
    private GroupBalanceModel balance;

    public DummyGroupModel (String groupName) {
        name = groupName;
        members = new ArrayList<>();
        expenses = new ArrayList<>();
    }

    public DummyGroupModel() {
        name = "Gruppo MAD";
        members = new ArrayList<>();
        expenses = new ArrayList<>();

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());

        int numMembers = 1 + rand.nextInt(10);
        for (int i = 0; i < numMembers; i++) {
            members.add(new DummyPersonModel(randomName(rand), this));
        }

        int numExps = 5 + rand.nextInt(10);
        addSomeExpenses(rand, numExps);

        balance = new GroupBalanceModel(this);
    }

    public void addSomeExpenses(int numExps) {
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        addSomeExpenses(rand, numExps);
    }

    void addSomeExpenses(Random rand, int numExps) {
        Calendar time = new GregorianCalendar();
        Currency currency = Currency.getInstance("EUR");

        String[] namePool = { "Groceries", "Utilities", "Birthday present", "Opera tickets", "Uber ride" };

        for (int i = 0; i < numExps; i++) {
            int personIndex = Math.abs(rand.nextInt(members.size()));
            int nameIndex = rand.nextInt(namePool.length);

            time.add(Calendar.DAY_OF_MONTH, Math.abs(rand.nextInt(10)));
            DummyExpenseModel exp = new DummyExpenseModel(
                    namePool[nameIndex],
                    (Calendar) time.clone(),
                    new Money(currency, (long) rand.nextInt(10000)),
                    (PersonModel) members.get(personIndex),
                    this,
                    members);
            expenses.add(exp);
        }

        notifyChanged();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) throws ConstraintException {
        this.name = name;
        notifyChanged();
    }

    @Override
    public List<PersonModel> getMembers() {
        return members;
    }

    @Override
    public void addMember(PersonModel person) throws ConstraintException {
        if (members.contains(person))
            return;
        members.add(person);
        notifyChanged();
    }

    @Override
    public void removeMember(PersonModel person) throws ConstraintException {
        if (members.remove(person))
            notifyChanged();
    }

    @Override
    public List<ExpenseModel> getExpenses() {
        return expenses;
    }

    @Override
    public void addExpense(ExpenseModel expense) throws ConstraintException {
        if (expenses.contains(expense))
            return;
        expenses.add(expense);
        notifyChanged();
    }

    @Override
    public void removeExpense(ExpenseModel expense) throws ConstraintException {
        if (expenses.remove(expense))
            notifyChanged();
    }

    @Override
    public String toJSON () {
        JSONObject jsonObject_group= new JSONObject();
        JSONObject pnObj_persons = new JSONObject();
        try {
            jsonObject_group.put("group_name", name);
            jsonObject_group.put("number_of_members", this.getMembers().size());
            for (int i=0;i<this.getMembers().size();i++) {
                pnObj_persons.put(String.valueOf(i), this.getMembers().get(i).getIdentifier());
            }
            jsonObject_group.put("members", pnObj_persons);
            return jsonObject_group.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public PersonModel getMember(String id) {
        for (PersonModel person : members) {
            if (person.getIdentifier().equals(id)) {
                return person;
            }
        }
        return null;
    }

    public static DummyGroupModel fromJSONstatic(String result) {
        DummyGroupModel dgm =null;
        try {
            //Retrieve GroupInfo From External Json Object
            JSONObject jsonObject_group_info = new JSONObject(result);
            //Manage Info Internal Json Object
            String gm_name = jsonObject_group_info.getString("group_name");
            dgm = new DummyGroupModel(gm_name);
            int size = jsonObject_group_info.getInt("number_of_members");
            JSONObject jsonObject_group_members = jsonObject_group_info.getJSONObject("members");
            for (int i=0;i<size;i++) {
                String name = jsonObject_group_members.getString(String.valueOf(i));
                DummyPersonModel dummyPersonModel = new DummyPersonModel(name,dgm);
                dgm.getMembers().add(dummyPersonModel);
            }
        }
        catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dgm;
    }

    public GroupBalanceModel getBalance() {
        return balance;
    }
}
