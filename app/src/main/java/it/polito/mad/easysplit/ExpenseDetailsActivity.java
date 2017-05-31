package it.polito.mad.easysplit;

import android.R.string;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
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
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.R.drawable;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.models.Money;


public class ExpenseDetailsActivity extends AppCompatActivity {
    private static final DateFormat sUIDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

    private DatabaseReference mRef;

    private ValueEventListener mListener;
    private ParticipantsAdapter mParticipantsAdapter;
    private String mGroupId, mPayerId;

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

        mParticipantsAdapter = new ParticipantsAdapter(this);
        ListView participantsList = (ListView) findViewById(id.participantsList);
        participantsList.setAdapter(mParticipantsAdapter);

        // mRef will be something like "https://easysplit-853e4.firebaseio.com/expenses/-KitTZeY14BFsnsH_rpp"
        mRef = Utils.findByUri(getIntent().getData());
        mListener = new ExpenseListener();
    }

    @Override
    protected void onResume() {
        mRef.addValueEventListener(mListener);
        super.onResume();
    }

    @Override
    protected void onPause() {
        mRef.removeEventListener(mListener);
        super.onPause();
    }


    private class ExpenseListener implements ValueEventListener {
        private final Button editButton = (Button) findViewById(id.editButton);
        private final TextView expenseNameText = (TextView) findViewById(id.expenseName);
        private final TextView creationDateText = (TextView) findViewById(id.expenseCreationDate);
        private final TextView payerNameText = (TextView) findViewById(id.payerName);

        @Override
        public void onDataChange(DataSnapshot expenseSnap) {
            if (expenseSnap.getValue() == null) {
                ExpenseDetailsActivity.this.finish();
                return;
            }

            mGroupId = expenseSnap.child("group_id").getValue(String.class);
            mPayerId = expenseSnap.child("payer_id").getValue(String.class);

            payerNameText.setText(expenseSnap.child("payer_name").getValue(String.class));
            expenseNameText.setText(expenseSnap.child("name").getValue(String.class));
            Date timestamp = new Date(expenseSnap.child("timestamp").getValue(Long.class));
            creationDateText.setText(sUIDateFormat.format(timestamp));

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

            String totalStr = expenseSnap.child("amount").getValue(String.class);

            String originalTotalStr = expenseSnap.child("amount_original").getValue(String.class);
            if (originalTotalStr == null)
                originalTotalStr = totalStr;

            String convertedTotalStr = expenseSnap.child("amount_converted").getValue(String.class);
            if (convertedTotalStr == null)
                convertedTotalStr = totalStr;

            Money convertedTotal = Money.parseOrFail(convertedTotalStr);
            Money originalTotal = Money.parseOrFail(originalTotalStr);

            /// TODO Move the total somewhere else on the UI
            if (convertedTotal.getCurrency().equals(originalTotal.getCurrency()))
                setTitle(originalTotal.toString());
            else
                setTitle(convertedTotal.toString() + " (" + originalTotal.toString() + ")");


            mParticipantsAdapter.clear();

            long numParticipants = expenseSnap.child("members_ids").getChildrenCount();
            if (numParticipants == 0)
                return;

            Money quota = convertedTotal.div(numParticipants);
            Money totalCostCalculated = quota.mul(numParticipants);
            Money rest = Money.zero();

            /// TODO Participants list adapter
            for (DataSnapshot member : expenseSnap.child("members_ids").getChildren()) {
                // In case the quota is not strictly equal to the amount divided by the
                // number of participants, we have to consider a global rest of the single Expense
                if (totalCostCalculated.compareTo(convertedTotal) != 0)
                    rest = convertedTotal.sub(totalCostCalculated);

                String memberId = member.getKey();
                String memberName = member.getValue(String.class);
                Participant participant = new Participant(memberId, memberName, quota);
                mParticipantsAdapter.add(participant);
            }

            distributeRest(rest);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            /// TODO
        }
    }

    private void distributeRest(Money rest) {
        if (rest.isZero())
            return;

        //extract decimal part of rest to understand how many iterations we need
        //in order to achieve the entire amount of the expense
        BigDecimal d = rest.getAmount();
        int numberOfIteration = d.subtract(d.setScale(0, RoundingMode.HALF_UP))
                .movePointRight(d.scale())
                .abs().intValue();

        int numParticipants = mParticipantsAdapter.getCount();
        for (int i = 0; i < numParticipants && numberOfIteration != 0; i++, numberOfIteration--) {
            Participant participant = mParticipantsAdapter.getItem(i);
            int cmp = rest.getAmount().compareTo(BigDecimal.ZERO);
            if (cmp > 0) {
                participant.quota = participant.quota.add(new BigDecimal("+0.01"));
            } else if (cmp < 0) {
                participant.quota = participant.quota.add(new BigDecimal("-0.01"));
            }
        }
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
            new AlertDialog.Builder(this)
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
                    .create().show();
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

    private static final class Participant {
        final String id;
        final String name;
        Money quota;

        Participant(String id, String name, Money quota) {
            this.id = id;
            this.name = name;
            this.quota = quota;
        }
    }

    private class ParticipantsAdapter extends ArrayAdapter<Participant> {
        ParticipantsAdapter(Context context) {
            super(context, 0);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            Participant participant = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_participant, parent, false);
            }

            TextView tvNameParticipant = (TextView) convertView.findViewById(R.id.name_participant);
            TextView tvResidueParticipant = (TextView) convertView.findViewById(R.id.residue_participant);

            tvNameParticipant.setText(participant.name);
            tvResidueParticipant.setText(participant.quota.toString());

            return convertView;
        }
    }
}
