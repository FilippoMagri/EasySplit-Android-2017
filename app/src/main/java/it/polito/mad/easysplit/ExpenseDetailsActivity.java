package it.polito.mad.easysplit;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;

import it.polito.mad.easysplit.models.Amountable;
import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.Money;
import it.polito.mad.easysplit.models.PersonModel;


public class ExpenseDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        MyApplication app = (MyApplication) getApplicationContext();
        ExpenseModel expense = app.getCurrentExpense();

        TextView name = (TextView) findViewById(R.id.expenseName);
        TextView creation = (TextView) findViewById(R.id.expenseCreationDate);

        name.setText(expense.getName());
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        creation.setText("Creation date: " + format.format(expense.getTimestamp().getTime()));

        class Participant implements Amountable {
            private PersonModel person;
            private Money owed;

            public Participant (PersonModel person, Money owed) {
                this.person = person;
                this.owed = owed;
            }

            public PersonModel getPerson() {
                return person;
            }

            @Override
            public String getName () {
                return person.getIdentifier();
            }

            @Override
            public Money getAmount() {
                return owed;
            }
        }

        List<Participant> participants = new ArrayList<>();
        List<PersonModel> members = expense.getGroup().getMembers();
        int participantsNumber = members.size();
        Money owed = expense.getAmount().div(participantsNumber).neg();

        for (PersonModel person : members) {
            if (person.equals(expense.getPayer())) { // payer, display amount of money paid
                participants.add(new Participant(person, expense.getAmount()));
            }
            else { // amount of money owed
                participants.add(new Participant(person, owed));
            }
        }
        ItemAdapter<Participant> adapter = new ItemAdapter<>(this, R.layout.participant_item, participants);
        ListView lv = (ListView) findViewById(R.id.participantsList);
        lv.setAdapter(adapter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
