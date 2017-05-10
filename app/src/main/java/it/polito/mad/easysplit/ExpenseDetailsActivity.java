package it.polito.mad.easysplit;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.models.IndirectValueEventListener;
import it.polito.mad.easysplit.models.Money;


public class ExpenseDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ExpenseDetailsActivity";
    private TextView mPayerName;
    private TextView mExpenseName;
    private TextView mCreationDate;
    private ListView mParticipantsListView;
    private Money singleExpenseRest = new Money(new BigDecimal("0.00"));
    private String idOfThisExpense="";
    private String idGroupOfThisExpense="";
    private ArrayList<Participant> listOfParticipants = new ArrayList<>();

    private DatabaseReference mRef;
    private ValueEventListener mListener;
    private IndirectValueEventListener mPayerNameListener;

    private Money moneyAmount = new Money(Currency.getInstance("EUR"), BigDecimal.ZERO);
    private BigDecimal numberOfParticipants = BigDecimal.ZERO;
    private String mPayerId = null, mGroupId = null;

    final ArrayList<String> participantsIds = new ArrayList<>();
    final ArrayList<Participant> participants = new ArrayList<>();

    ParticipantsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expense_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Group.class));
            }
        });

        final Button editButton = (Button) findViewById(R.id.editButton);

        mPayerName = (TextView) findViewById(R.id.payerName);
        mExpenseName = (TextView) findViewById(R.id.expenseName);
        mCreationDate = (TextView) findViewById(R.id.expenseCreationDate);

        mParticipantsListView = (ListView) findViewById(R.id.participantsList);
        adapter = new ParticipantsAdapter(ExpenseDetailsActivity.this,participants);
        mParticipantsListView.setAdapter(adapter);

        //mRef will be something like "https://easysplit-853e4.firebaseio.com/expenses/-KitTZeY14BFsnsH_rpp"
        //It depends on the expense selected
        mRef = Utils.findByUri(getIntent().getData());
        idOfThisExpense = mRef.toString().replace("https://easysplit-853e4.firebaseio.com/expenses/","");

        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                idGroupOfThisExpense = dataSnapshot.child("group_id").getValue(String.class);
                Log.d(TAG,idGroupOfThisExpense);
                DatabaseReference thisExpenseRef = mRef.getParent().getParent().child("groups").child(idGroupOfThisExpense).child("expenses").child(idOfThisExpense).getRef();
                thisExpenseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mExpenseName.setText(dataSnapshot.child("name").getValue(String.class));
                        mCreationDate.setText(dataSnapshot.child("timestamp").getValue(String.class));
                        payerId = (String) dataSnapshot.child("payer_id").getValue();
                        String rawStringAmount = (String) dataSnapshot.child("amount").getValue();
                        moneyAmount = Money.parse(rawStringAmount);
                        setTitle(moneyAmount.toString());
                        Log.d(TAG, rawStringAmount.toString());
                        Log.d(TAG, moneyAmount.toString());
                        /// TODO Participants list adapter
                        numberOfParticipants = new BigDecimal(dataSnapshot.child("members_ids").getChildrenCount());
                        HashMap<String, Object> mapMembers = (HashMap<String, Object>) dataSnapshot.child("members_ids").getValue();
                        for (Map.Entry<String, Object> entry : mapMembers.entrySet()) {
                            String participantKey = entry.getKey();
                            String name = entry.getValue().toString();
                            boolean isPayer = false;
                            if (participantKey.equals(payerId)) {
                                isPayer = true;
                            }
                            Participant participant = new Participant(participantKey, name, isPayer, moneyAmount, numberOfParticipants);
                            listOfParticipants.add(participant);
                        }

                        editButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Intent i = new Intent(getApplicationContext(), EditExpenseActivity.class);
                                i.putExtra("expenseId", dataSnapshot.getKey());
                                i.putExtra("groupId", dataSnapshot.child("group_id").getValue(String.class));
                                startActivity(i);
                            }
                        });
                        distributeTheRestAmongParticipants(payerId);
                        addParticipantsToAdapter();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                /// TODO
            }
        };

        mRef.addValueEventListener(mListener);

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

    private void addParticipantsToAdapter() {
        adapter.addAll(listOfParticipants);
    }

    private void distributeTheRestAmongParticipants(String payerId) {
        if (singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)!=0) {
            //extract decimal part of singleExpenseRest to understand how many iterations we need
            //in order to achieve the entire amount of the expense
            BigDecimal d = BigDecimal.valueOf(singleExpenseRest.getAmount().doubleValue());
            BigDecimal result = d.subtract(d.setScale(0, RoundingMode.HALF_UP)).movePointRight(d.scale());
            Integer numberOfIteration = result.abs().intValue();
            for (int i=0;i<listOfParticipants.size() && numberOfIteration!=0 ;i++, numberOfIteration--) {
                Log.d(TAG,"ITERATO");
                Participant participant = listOfParticipants.get(i);
                if(participant.getId().equals(payerId)) { numberOfIteration++; continue; }
                if(singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)>0){
                    participant.participationFee = participant.participationFee.add(new Money(new BigDecimal("0.01")));
                }
                if (singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)<0){
                    participant.participationFee = participant.participationFee.add(new Money(new BigDecimal("-0.01")));
                }
            }
        }
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
        getMenuInflater().inflate(R.menu.menu_expense_details, menu);
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
        } else if (id == R.id.action_delete) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.title_confirm)
                    .setMessage(R.string.message_confirm_delete_expense)
                    .setPositiveButton(R.string.button_delete, new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteExpense();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.button_cancel, new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create();
            dialog.show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteExpense() {
        String expenseId = mRef.getKey();
        Map<String, Object> update = new HashMap<>();
        update.put("/expenses/"+expenseId, null);
        update.put("/users/"+mPayerId+"/expenses_ids_as_payer/"+expenseId, null);
        update.put("/groups/"+mGroupId+"/expenses/"+expenseId, null);
        mRef.getRoot().updateChildren(update).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    finish();
                    return;
                }

                AlertDialog dialog = new AlertDialog.Builder(ExpenseDetailsActivity.this)
                        .setTitle(R.string.title_error_dialog)
                        .setMessage(task.getException().getLocalizedMessage())
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                finish();
                            }
                        })
                        .create();
                dialog.show();
            }
        });
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
            Money totalCostCalculated = participationFee.mul(numberOfParticipants);
            //In case the quote is not strictly exact compared to the amount and the number of participants
            //we have to consider a global rest of the single Expense
            if (totalCostCalculated.getAmount().compareTo(totalCost.getAmount())!=0) {
                singleExpenseRest = totalCost.sub(totalCostCalculated);
                Log.d(TAG,singleExpenseRest.toString());
            }
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
