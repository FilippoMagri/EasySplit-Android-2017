package it.polito.mad.easysplit;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.models.IndirectValueEventListener;


public class ExpenseDetailsActivity extends AppCompatActivity {
    private TextView mPayerName;
    private TextView mExpenseName;
    private TextView mCreationDate;

    private DatabaseReference mRef;
    private ValueEventListener mListener;
    private IndirectValueEventListener mPayerNameListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);
        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        */

        mPayerName = (TextView) findViewById(R.id.payerName);
        mExpenseName = (TextView) findViewById(R.id.expenseName);
        mCreationDate = (TextView) findViewById(R.id.expenseCreationDate);

        mRef = Utils.findByUri(getIntent().getData());

        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mExpenseName.setText(dataSnapshot.child("name").getValue(String.class));
                mCreationDate.setText(dataSnapshot.child("timestamp").getValue(String.class));
                /// TODO Participants list adapter
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                /// TODO
            }
        };
        mRef.addValueEventListener(mListener);

        mPayerNameListener = new IndirectValueEventListener(mRef.child("payer_id")) {
            @Override
            protected DatabaseReference getTarget(String id, DatabaseReference root) {
                return root.child("users").child(id).child("name");
            }

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mPayerName.setText(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
    }

    @Override
    protected void onDestroy() {
        mRef.removeEventListener(mListener);
        mPayerNameListener.detach();
        super.onDestroy();
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
