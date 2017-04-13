package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.view.View;

import java.util.List;

import it.polito.mad.easysplit.models.ExpenseModel;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.dummy.DummyGroupModel;


public class ExpensesListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expenses_list);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        MyApplication app = (MyApplication) getApplicationContext();
        GroupModel dm = app.getGroupModel(); //new DummyGroupModel();
        if (dm !=null) {
            if (dm.getExpenses().size()>0) {
                List<ExpenseModel> expenses = dm.getExpenses();
                ItemAdapter<ExpenseModel> adapter = new ItemAdapter<>(this, R.layout.expense_item, expenses);
                ListView lv = (ListView) findViewById(R.id.expensesList);
                lv.setAdapter(adapter);

                lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        ExpenseModel expense = ((ItemAdapter<ExpenseModel>) parent.getAdapter()).getItem(position);
                        MyApplication app = (MyApplication) getApplicationContext();
                        app.setCurrentExpense(expense);
                        Intent showExpense = new Intent(view.getContext(), ExpenseDetailsActivity.class);
                        startActivity(showExpense);
                    }
                });
            }

        }
        setTitle("Gruppo MAD");
        ImageView imgView = (ImageView) findViewById(R.id.add_button_expense);
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(), AddExpenses.class);
                startActivity(i);
            }
        });
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
