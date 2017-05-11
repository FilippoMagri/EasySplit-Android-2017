package it.polito.mad.easysplit;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import it.polito.mad.easysplit.models.Money;

public class AddExpenses extends AppCompatActivity {
    // Used for both the spinner and the checklist
    private final class MemberListItem {
        String id, name;

        MemberListItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private final DateFormat mUIDateFormat = DateFormat.getDateInstance();
    private DatePickerDialog mDatePickerDialog;
    private DateFormat mTimestampFormat;
    private String mGroupId;

    private UnsavedChangesNotifier mNotifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expenses);

        mGroupId = getIntent().getStringExtra("groupId");
        mTimestampFormat = android.text.format.DateFormat.getLongDateFormat(this);

        setupDateEdit();
        setActionOnButtons();
        setupMembersSpinner();
        setupMembersChecklist();

        mNotifier = new UnsavedChangesNotifier(this, this); // prepare unsaved changes notifier
        /// TODO Add calls to mNotifier.setChanged() where relevant
    }

    private void setupDateEdit() {
        final EditText dateEdit = (EditText) findViewById(R.id.dateEdit);

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar cal = Calendar.getInstance();
                cal.set(year, monthOfYear, dayOfMonth);
                dateEdit.setText(mUIDateFormat.format(new Date(cal.getTimeInMillis())));
            }
        };

        Calendar newCalendar = Calendar.getInstance();
        mDatePickerDialog = new DatePickerDialog(this, dateSetListener,
                newCalendar.get(Calendar.YEAR),
                newCalendar.get(Calendar.MONTH),
                newCalendar.get(Calendar.DAY_OF_MONTH));

        // Disable future dates
        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        Calendar newDate = Calendar.getInstance();
        dateEdit.setText(mUIDateFormat.format(newDate.getTime()));
    }

    public void setupMembersSpinner() {
        final Spinner spinner = (Spinner) findViewById(R.id.payerSpinner);
        final ArrayAdapter<MemberListItem> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        DatabaseReference expensesIdsRef = mRoot.child("groups/"+mGroupId+"/members_ids");
        expensesIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersIdsRef) {
                adapter.clear();
                for (DataSnapshot child : membersIdsRef.getChildren()) {
                    final String userId = child.getKey();
                    DatabaseReference nameRef = mRoot.child("users").child(userId).child("name");
                    nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot nameRef) {
                            String userName = nameRef.getValue(String.class);
                            adapter.add(new MemberListItem(userId, userName));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            adapter.add(new MemberListItem(userId, "???"));
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                spinner.setEnabled(false);
                Snackbar.make(spinner, databaseError.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });

        spinner.setAdapter(adapter);
    }

    public void setupMembersChecklist() {
        final ListView listView = (ListView) findViewById(R.id.membersList);
        final ArrayAdapter<MemberListItem> adapter =
                new ArrayAdapter<MemberListItem>(this, android.R.layout.simple_list_item_multiple_choice);

        listView.setAdapter(adapter);

        DatabaseReference expensesIdsRef = mRoot.child("groups/"+mGroupId+"/members_ids");
        expensesIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersIdsRef) {
                for (DataSnapshot child : membersIdsRef.getChildren()) {
                    final String userId = child.getKey();
                    DatabaseReference nameRef = mRoot.child("users").child(userId).child("name");
                    nameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot nameRef) {
                            String userName = nameRef.getValue(String.class);
                            adapter.add(new MemberListItem(userId, userName));
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            adapter.add(new MemberListItem(userId, "???"));
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Snackbar.make(listView, databaseError.getMessage(), Snackbar.LENGTH_LONG)
                        .show();
            }
        });
    }

    public void setActionOnButtons() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /// TODO Setup back/up navigation

        ImageView dateImg = (ImageView) findViewById(R.id.dateImg);
        dateImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDatePickerDialog.show();
            }
        });

        ImageView confirmBtn = (ImageView) findViewById(R.id.img_confirm_add_expenses);
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptExpense();
            }
        });
    }

    private void acceptExpense() {
        final View contentView = findViewById(android.R.id.content);
        EditText dateEdit = (EditText) findViewById(R.id.dateEdit);
        EditText titleEdit = (EditText) findViewById(R.id.titleEdit);
        EditText amountEdit = (EditText) findViewById(R.id.amountEdit);
        Spinner payerSpinner = (Spinner) findViewById(R.id.payerSpinner);
        ListView membersList = (ListView) findViewById(R.id.membersList);
        Spinner spinnerMonney = (Spinner) findViewById(R.id.spinnerMonney);

        String dateStr = dateEdit.getText().toString();
        Date timestamp;
        try {
            timestamp = mUIDateFormat.parse(dateStr);
        } catch (ParseException e) {
            Snackbar.make(contentView, "The date is invalid!", Snackbar.LENGTH_LONG)
                    .show();
            return;
        }

        String title = titleEdit.getText().toString();

        Money amount;
                String currencyCode = (String) spinnerMonney.getSelectedItem();

        try {
//amount take the value of price + currencyCode
            BigDecimal price = BigDecimal.valueOf(Float.parseFloat(amountEdit.getText().toString()));
            Currency cur = Currency.getInstance(currencyCode);
            //Rounding with 2 Numbers After dot
            price = price.divide(new BigDecimal("1.00"),2,RoundingMode.HALF_UP);
            amount = new Money(cur, price);
            amount.div(new BigDecimal("1.00"));
        } catch (NoSuchElementException exc) {
            Snackbar.make(contentView, "Invalid money amount!", Snackbar.LENGTH_LONG).show();
            return;
        }

        MemberListItem payerItem = (MemberListItem) payerSpinner.getSelectedItem();
        String payerId = payerItem.id;

        Map<String, String> memberIds = new HashMap<>();
        int numMembers = membersList.getAdapter().getCount();
        for (int i=0; i < numMembers; i++) {
            MemberListItem item = (MemberListItem) membersList.getItemAtPosition(i);
            if(membersList.isItemChecked(i)) {
                memberIds.put(item.id, item.name);
            }
        }

        Map<String, Object> expense = new HashMap<>();
        expense.put("name", title);
        /// TODO Decide on a standard, strict format for the timestamp
        expense.put("timestamp", mTimestampFormat.format(timestamp));
        expense.put("timestamp_number", -1 * new Date().getTime());
        expense.put("amount", amount.toString());
        expense.put("payer_id", payerId);
        expense.put("group_id", mGroupId);
        expense.put("members_ids", memberIds);

        /// TODO Refactor database write logic to somewhere else
        final String expenseId = mRoot.child("groups").child(mGroupId).child("expenses").push().getKey();
        Map<String, Object> update = new HashMap<>();
        update.put("groups/"+mGroupId+"/expenses/"+expenseId, expense);
        update.put("users/"+payerId+"/expenses_ids_as_payer/"+expenseId, title);
        update.put("expenses/"+expenseId, expense);

        mRoot.updateChildren(update).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (! task.isSuccessful()) {
                    String msg = task.getException().getLocalizedMessage();
                    Snackbar.make(contentView, msg, Snackbar.LENGTH_LONG).show();
                    return;
                }
                /// TODO Go to the expense details (with the newly created URI)
                Intent i = new Intent(getApplicationContext(), ExpenseDetailsActivity.class);
                i.setData(Utils.getUriFor(Utils.UriType.EXPENSE, expenseId));
                startActivity(i);
                finish();
            }
        });
    }


    @Override
    public void onBackPressed() {
        mNotifier.handleBackButton();
    }

}
