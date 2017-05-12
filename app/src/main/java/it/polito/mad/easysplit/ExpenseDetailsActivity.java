package it.polito.mad.easysplit;

import android.R.string;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.R.drawable;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.models.IndirectValueEventListener;
import it.polito.mad.easysplit.models.Money;


public class ExpenseDetailsActivity extends AppCompatActivity {
    private static final DateFormat sUIDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

    private Money singleExpenseRest = new Money(new BigDecimal("0.00"));
    private final ArrayList<Participant> mParticipants = new ArrayList<>();

    private DatabaseReference mRef;
    private ValueEventListener mListener;
    private IndirectValueEventListener mPayerNameListener;

    private String mGroupId, mPayerId;

    private final ArrayList<Participant> participants = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_expense_details);

        Toolbar toolbar = (Toolbar) findViewById(id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Group.class));
            }
        });

        final Button editButton = (Button) findViewById(id.editButton);
        final TextView payerNameText = (TextView) findViewById(id.payerName);
        final TextView expenseNameText = (TextView) findViewById(id.expenseName);
        final TextView creationDateText = (TextView) findViewById(id.expenseCreationDate);

        ListView participantsList = (ListView) findViewById(id.participantsList);
        final ParticipantsAdapter participantsAdapter = new ParticipantsAdapter(ExpenseDetailsActivity.this,participants);
        participantsList.setAdapter(participantsAdapter);

        // mRef will be something like "https://easysplit-853e4.firebaseio.com/expenses/-KitTZeY14BFsnsH_rpp"
        mRef = Utils.findByUri(getIntent().getData());
        mListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot expenseSnap) {
                mGroupId = expenseSnap.child("group_id").getValue(String.class);
                mPayerId = expenseSnap.child("payer_id").getValue(String.class);

                expenseNameText.setText(expenseSnap.child("name").getValue(String.class));
                Date timestamp = new Date(expenseSnap.child("timestamp").getValue(Long.class));
                creationDateText.setText(sUIDateFormat.format(timestamp));

                String rawStringAmount = (String) expenseSnap.child("amount").getValue();
                Money money = Money.parseOrFail(rawStringAmount);
                setTitle(money.toString());

                /// TODO Participants list adapter
                BigDecimal numParticipants = new BigDecimal(expenseSnap.child("members_ids").getChildrenCount());
                for (DataSnapshot member : expenseSnap.child("members_ids").getChildren()) {
                    String memberId = member.getKey();
                    String memberName = member.getValue(String.class);
                    boolean isPayer = member.getKey().equals(mPayerId);
                    Participant participant = new Participant(memberId, memberName, isPayer, money, numParticipants);
                    mParticipants.add(participant);
                }

                final String expenseId = expenseSnap.getKey();
                editButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(getApplicationContext(), EditExpenseActivity.class);
                        i.putExtra("expenseId", expenseId);
                        i.putExtra("groupId", mGroupId);
                        startActivity(i);
                    }
                });

                distributeTheRestAmongParticipants();
                participantsAdapter.addAll(mParticipants);
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
                payerNameText.setText(dataSnapshot.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        };
    }

    private void distributeTheRestAmongParticipants() {
        if (singleExpenseRest.getAmount().compareTo(BigDecimal.ZERO)!=0) {
            //extract decimal part of singleExpenseRest to understand how many iterations we need
            //in order to achieve the entire amount of the expense
            BigDecimal d = BigDecimal.valueOf(singleExpenseRest.getAmount().doubleValue());
            BigDecimal result = d.subtract(d.setScale(0, RoundingMode.HALF_UP)).movePointRight(d.scale());
            Integer numberOfIteration = result.abs().intValue();
            for (int i = 0; i< mParticipants.size() && numberOfIteration!=0 ; i++, numberOfIteration--) {

                Participant participant = mParticipants.get(i);
                if(participant.getId().equals(mPayerId)) { numberOfIteration++; continue; }
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
        getMenuInflater().inflate(menu.menu_expense_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == id.action_settings) {
            return true;
        } else if (id == id.action_delete) {
            AlertDialog dialog = new Builder(this)
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

                AlertDialog dialog = new Builder(ExpenseDetailsActivity.this)
                        .setTitle(R.string.title_error_dialog)
                        .setMessage(task.getException().getLocalizedMessage())
                        .setPositiveButton(string.ok, new OnClickListener() {
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
        boolean isPayer;
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
                convertView = LayoutInflater.from(getContext()).inflate(layout.list_item_participant, parent, false);
            }
            // Lookup view for data population
            TextView tvNameParticipant = (TextView) convertView.findViewById(id.name_participant);
            TextView tvResidueParticipant = (TextView) convertView.findViewById(id.residue_participant);
            // Populate the data into the template view using the data object
            tvNameParticipant.setText(participant.getName());
            tvResidueParticipant.setText(participant.getParticipationFee().toString());
            // Return the completed view to render on screen
            return convertView;
        }
    }
}
