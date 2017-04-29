package it.polito.mad.easysplit;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;

import it.polito.mad.easysplit.models.IndirectValueEventListener;
import it.polito.mad.easysplit.models.Money;


public class ExpenseDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ExpenseDetailsActivity";
    private TextView mPayerName;
    private TextView mExpenseName;
    private TextView mCreationDate;
    private ListView mParticipantsListView;

    private DatabaseReference mRef;
    private ValueEventListener mListener;
    private IndirectValueEventListener mPayerNameListener;

    Money moneyAmount=new Money(Currency.getInstance("EUR"),new BigDecimal("0.00"));
    BigDecimal numberOfParticipants=new BigDecimal("0");
    String payerId="Nobody";

    final ArrayList<String> participantsIds = new ArrayList<>();
    final ArrayList<Participant> participants = new ArrayList<>();

    ParticipantsAdapter adapter;

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

        mParticipantsListView = (ListView) findViewById(R.id.participantsList);
        adapter = new ParticipantsAdapter(ExpenseDetailsActivity.this,participants);
        mParticipantsListView.setAdapter(adapter);

        //mRef will be something like "https://easysplit-853e4.firebaseio.com/expenses/-KitTZeY14BFsnsH_rpp"
        //It depends on the expense selected
        mRef = Utils.findByUri(getIntent().getData());

        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mExpenseName.setText(dataSnapshot.child("name").getValue(String.class));
                mCreationDate.setText(dataSnapshot.child("timestamp").getValue(String.class));
                payerId = (String) dataSnapshot.child("payer_id").getValue();
                String rawStringAmount = (String) dataSnapshot.child("amount").getValue();
                moneyAmount = Money.parse(rawStringAmount);
                setTitle(moneyAmount.toString());

                Log.d(TAG,rawStringAmount.toString());
                Log.d(TAG,moneyAmount.toString());
                /// TODO Participants list adapter
                numberOfParticipants = new BigDecimal(dataSnapshot.child("members_ids").getChildrenCount());
                for (final DataSnapshot child :dataSnapshot.child("members_ids").getChildren()) {
                    String participantKey = child.getKey();
                    participantsIds.add(participantKey);
                    Log.d(TAG,participantKey);
                    DatabaseReference userRef = mRef.getParent().getParent().child("users").child(participantKey).getRef();
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String name = dataSnapshot.child("name").getValue().toString();
                            boolean isPayer=false;
                            if (dataSnapshot.getKey().equals(payerId)) isPayer=true;
                            Log.d(TAG,"chiave: "+child.getKey());
                            Log.d(TAG,"name: "+name);
                            Log.d(TAG,"isPayer: "+isPayer);
                            Log.d(TAG,"MoneyAmount: "+moneyAmount);
                            Log.d(TAG,"NumberOfParticipants: "+numberOfParticipants);
                            Participant participant = new Participant(child.getKey(),name,isPayer,moneyAmount,numberOfParticipants);
                            Log.d(TAG,"Residue: "+participant.getParticipationFee());
                            Log.d(TAG,"MoneyBackToThePayer: "+participant.getMoneyBackToThePayer());
                            adapter.add(participant);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                /// TODO
            }
        };
        mRef.addListenerForSingleValueEvent(mListener);

        //mRef.child("payer_id") will be something like "https://easysplit-853e4.firebaseio.com/expenses/-KitTZeY14BFsnsH_rpp/payer_id"
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

    class Participant {
        String id;
        String name;
        boolean isPayer=false;
        Money totalCost;
        BigDecimal numberOfParticipants;
        Money participationFee;
        Money moneyBackToThePayer;

        public Participant(String id,String name,boolean isPayer,Money totalCost,BigDecimal numberOfParticipants) {
            this.id = id;
            this.name = name;
            this.isPayer = isPayer;
            this.totalCost = totalCost;
            this.numberOfParticipants = numberOfParticipants;
            this.participationFee = totalCost.div(numberOfParticipants);
            if (isPayer) {
                moneyBackToThePayer = totalCost.sub(participationFee);
            } else {
                moneyBackToThePayer = new Money(totalCost.getCurrency(),new BigDecimal("0.00"));
            }
        }
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isPayer() {
            return isPayer;
        }

        public void setPayer(boolean payer) {
            isPayer = payer;
        }

        public Money getTotalCost() {
            return totalCost;
        }

        public void setTotalCost(Money totalCost) {
            this.totalCost = totalCost;
        }

        public BigDecimal getNumberOfParticipants() {
            return numberOfParticipants;
        }

        public void setNumberOfParticipants(BigDecimal numberOfParticipants) {
            this.numberOfParticipants = numberOfParticipants;
        }

        public Money getParticipationFee() {
            return participationFee;
        }

        public void setParticipationFee(Money participationFee) {
            this.participationFee = participationFee;
        }

        public Money getMoneyBackToThePayer() {
            return moneyBackToThePayer;
        }

        public void setMoneyBackToThePayer(Money moneyBackToThePayer) {
            this.moneyBackToThePayer = moneyBackToThePayer;
        }
    }

    class ParticipantsAdapter extends ArrayAdapter<Participant> {
        public ParticipantsAdapter(Context context, ArrayList<Participant> participants) {
            super(context,0,participants);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            Participant participant = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_participant, parent, false);
            }
            // Lookup view for data population
            TextView tvNameParticipant = (TextView) convertView.findViewById(R.id.name_participant);
            TextView tvResidueParticipant = (TextView) convertView.findViewById(R.id.residue_participant);
            // Populate the data into the template view using the data object
            tvNameParticipant.setText(participant.getName());
            tvResidueParticipant.setText(participant.getParticipationFee().toString());
            // Return the completed view to render on screen
            return convertView;
        }
    }
}
