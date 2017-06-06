package it.polito.mad.easysplit;

import android.R.layout;
import android.R.string;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

import com.android.volley.NoConnectionError;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.Utils.UriType;
import it.polito.mad.easysplit.cloudMessaging.MessagingUtils;
import it.polito.mad.easysplit.models.Money;


/** An activity that can add a new expense or edit an existing one.
 *
 * If the intent contains a string extra for key "expenseId", the activity will edit that activity;
 * otherwise, a new one will be created. In both cases, the "groupId" extra is mandatory.
 */
public class EditExpenseActivity extends AppCompatActivity {
    private static final DecimalFormat mDecimalFormat = new DecimalFormat("#,##0.00");

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
    private final DateFormat mUIDateFormat = DateFormat.getDateTimeInstance();
    private DateTimePicker mDateTimePicker;
    private String mGroupId;
    private String mGroupCurrencyCode = ConversionRateProvider.getBaseCurrency().getCurrencyCode();
    private CurrencySpinnerAdapter mCurrenciesAdapter;

    // only used in edit mode
    private DataSnapshot mInitialExpense;
    private View mProgressBarOverlay;
    private UnsavedChangesNotifier mNotifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expenses);

        // Can't be done before onCreate()
        mCurrenciesAdapter = new CurrencySpinnerAdapter(this);

        mProgressBarOverlay = findViewById(id.progress_bar_overlay);
        mNotifier = new UnsavedChangesNotifier(this, this);

        Intent i = getIntent();
        mGroupId = i.getStringExtra("groupId");
        mRoot.child("groups").child(mGroupId).child("currency").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot currencySnap) {
                String currencyCode = currencySnap.getValue(String.class);
                if (currencyCode != null)
                    mGroupCurrencyCode = currencyCode;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ActivityUtils.showDatabaseError(EditExpenseActivity.this, databaseError);
            }
        });

        String expenseId = i.getStringExtra("expenseId");
        if (expenseId != null) {
            // edit mode
            setTitle(getString(R.string.title_edit_expenes));
            mNotifier.setChanged();
            mProgressBarOverlay.setVisibility(View.VISIBLE);
            mRoot.child("expenses").child(expenseId).addListenerForSingleValueEvent(new ExpenseListener());
        } else {
            // add mode: setup the (empty) view immediately
            mProgressBarOverlay.setVisibility(View.GONE);
            setupView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reset the decimal format configuration, in case the system's locale configuration
        // has changed while the Activity was not running
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('\'');
        if (Locale.getDefault().getDisplayLanguage().equals("italiano")) {
            symbols.setDecimalSeparator(',');
        } else if (Locale.getDefault().getDisplayLanguage().equals("English")) {
            symbols.setDecimalSeparator('.');
        }
        mDecimalFormat.setDecimalFormatSymbols(symbols);
        mDecimalFormat.setParseBigDecimal(true);
    }

    @Override
    public void onBackPressed() {
        mNotifier.handleBackButton();
    }

    private class ExpenseListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot expenseSnap) {
            mProgressBarOverlay.setVisibility(View.GONE);
            mInitialExpense = expenseSnap;
            setupView();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            mProgressBarOverlay.setVisibility(View.GONE);
            AlertDialog dialog = new Builder(EditExpenseActivity.this)
                    .setTitle("Error")
                    .setMessage(databaseError.getMessage())
                    .setNegativeButton(string.cancel, new AlertDialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog1, int which) {
                            EditExpenseActivity.this.finish();
                        }
                    })
                    .show();
        }
    }

    private boolean isEditing() {
        return mInitialExpense != null;
    }

    public void setupView() {
        setupDateEdit();
        setActionOnButtons();
        setupPayerSpinner();
        setupMembersChecklist();

        Spinner currencySpinner = (Spinner) findViewById(id.currencySpinner);
        currencySpinner.setAdapter(mCurrenciesAdapter);

        if (isEditing()) {
            EditText titleEdit = (EditText) findViewById(id.titleEdit);
            titleEdit.setText(mInitialExpense.child("name").getValue(String.class));

            EditText amountEdit = (EditText) findViewById(id.amountEdit);
            Money amount = Money.parseOrFail(mInitialExpense.child("amount_original").getValue(String.class));
            amountEdit.setText(amount.getAmount().toString());

            int position = mCurrenciesAdapter.findCurrencyByCode(amount.getCurrency().getCurrencyCode());
            currencySpinner.setSelection(position);
        }
    }

    private void setupDateEdit() {
        final Button dateButton = (Button) findViewById(id.dateButton);

        Long timestamp = isEditing() ?
                mInitialExpense.child("timestamp").getValue(Long.class) :
                null;

        mDateTimePicker = new DateTimePicker(this, timestamp);
        mDateTimePicker.setListener(new DateTimePicker.Listener() {
            @Override
            public void onDateTimeChanged(Calendar calendar) {
                dateButton.setText(mUIDateFormat.format(calendar.getTime()));
            }
        });
    }

    public void setupPayerSpinner() {
        final ArrayAdapter<MemberListItem> adapter =
                new ArrayAdapter<>(this, layout.simple_spinner_item);
        adapter.setDropDownViewResource(layout.simple_spinner_dropdown_item);

        final Spinner spinner = (Spinner) findViewById(id.payerSpinner);
        spinner.setAdapter(adapter);

        final String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String currentUserName = sharedPref.getString("signin_complete_name", null);

        adapter.clear();
        adapter.add(new MemberListItem(currentUid, currentUserName));

        final DatabaseReference membersIdsRef = mRoot.child("groups/"+mGroupId+"/members_ids");
        membersIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersIdsSnap) {
                // Make the owner of the phone the first member in the spinner
                ArrayList<String> keys = new ArrayList<>();
                for (DataSnapshot child : membersIdsSnap.getChildren())
                    keys.add(child.getKey());

                String currentPayerId = null;
                if (isEditing())
                    currentPayerId = mInitialExpense.child("payer_id").getValue(String.class);

                int index = 0;
                for (DataSnapshot child : membersIdsSnap.getChildren()) {
                    String userId = child.getKey();
                    String userName = child.getValue(String.class);

                    if (userId.equals(currentUid))
                        continue;

                    adapter.add(new MemberListItem(userId, userName));
                    if (userId.equals(currentPayerId))
                        spinner.setSelection(index);

                    index++;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                spinner.setEnabled(false);
                ActivityUtils.showDatabaseError(EditExpenseActivity.this, databaseError);
            }
        });
    }

    public void setupMembersChecklist() {
        final ListView listView = (ListView) findViewById(id.membersList);
        final ArrayAdapter<MemberListItem> adapter =
                new ArrayAdapter<MemberListItem>(this, layout.simple_list_item_multiple_choice);

        listView.setAdapter(adapter);

        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String currentUserName = sharedPref.getString("signin_complete_name", null);
        adapter.add(new MemberListItem(currentUid, currentUserName));

        DatabaseReference membersIdsRef = mRoot.child("groups/"+mGroupId+"/members_ids");
        membersIdsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot membersIdsSnap) {
                adapter.clear();
                for (DataSnapshot child : membersIdsSnap.getChildren()) {
                    String userId = child.getKey();
                    String userName = child.getValue(String.class);
                    adapter.add(new MemberListItem(userId, userName));
                }

                if (isEditing()) {
                    for (int i = 0; i < adapter.getCount(); i++) {
                        MemberListItem item = adapter.getItem(i);
                        boolean checked = mInitialExpense.child("members_ids").child(item.id).exists();
                        listView.setItemChecked(i, checked);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ActivityUtils.showDatabaseError(EditExpenseActivity.this, databaseError);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_expense, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == id.action_accept) {
            acceptExpense();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setActionOnButtons() {
        Toolbar toolbar = (Toolbar) findViewById(id.toolbar);
        setSupportActionBar(toolbar);

        /// TODO Setup back/up navigation

        Button timestampButton = (Button) findViewById(id.dateButton);
        timestampButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mDateTimePicker.show();
            }
        });
    }

    public void validateData() throws ValidationException {
        EditText titleEdit = (EditText) findViewById(id.titleEdit);
        if (titleEdit.getText().toString().trim().length() == 0)
            throw new ValidationException(getString(R.string.error_validate_title_empty));
    }

    private static final int CONVERSION_TIMEOUT_SECS = 5;

    /**
     * Save the existing expense or create the new one
     */
    public synchronized void acceptExpense() {
        try {
            validateData();
        } catch (ValidationException exc) {
            Snackbar.make(findViewById(android.R.id.content), exc.getMessage(), Snackbar.LENGTH_LONG)
                .show();
            return;
        }

        // Everything needs to executed in a separate thread, because we need to call Tasks.await
        // in it, and doing that on the main thread is forbidden (although we would only suffer a
        // minor delay if we did, since we have a timeout).  It's a hack, but hey
        new Thread(new Runnable() {
            @Override
            public void run() {
                final View contentView = findViewById(android.R.id.content);
                EditText titleEdit = (EditText) findViewById(id.titleEdit);
                EditText amountEdit = (EditText) findViewById(id.amountEdit);
                Spinner payerSpinner = (Spinner) findViewById(id.payerSpinner);
                ListView membersList = (ListView) findViewById(id.membersList);
                Spinner currencySpinner = (Spinner) findViewById(id.currencySpinner);

                final Date timestamp = mDateTimePicker.getCalendar().getTime();

                final String title = titleEdit.getText().toString();

                Money amountOriginal;
                Currency currency = (Currency) currencySpinner.getSelectedItem();
                try {
                    // amount take the value of price + currencyCode
                    String codeCountry = Locale.getDefault().getDisplayLanguage();
                    String amountEditString = amountEdit.getText().toString();
                    if (codeCountry.equals("italiano")) {
                        amountEditString = amountEditString.replace(".", ",");
                    } else if (codeCountry.equals("English")) {
                        amountEditString = amountEditString.replace(",", ".");
                    }
                    BigDecimal price = (BigDecimal) mDecimalFormat.parse(amountEditString);
                    //Rounding with 2 Numbers After dot
                    price = price.divide(new BigDecimal("1.00"), 2, RoundingMode.HALF_UP);
                    amountOriginal = new Money(currency, price);
                } catch (NoSuchElementException | ParseException exc) {
                    Snackbar.make(contentView, "Invalid money amount!", Snackbar.LENGTH_LONG).show();
                    return;
                }


                ConversionRateProvider conversionProvider = ConversionRateProvider.getInstance();
                final Money amountBase, amountConverted;

                try {
                    Task<Money> conversionToBase = conversionProvider.convertToBase(amountOriginal);
                    amountBase = Tasks.await(conversionToBase, CONVERSION_TIMEOUT_SECS, TimeUnit.SECONDS);

                    Currency groupCurrency = Currency.getInstance(mGroupCurrencyCode);
                    Task<Money> conversionToGroup = conversionProvider.convertFromBase(amountBase, groupCurrency);
                    amountConverted = Tasks.await(conversionToGroup, CONVERSION_TIMEOUT_SECS, TimeUnit.SECONDS);
                } catch (ExecutionException exc) {
                    String message = exc.getMessage();
                    if (exc.getCause() instanceof NoConnectionError)
                        message = getString(R.string.error_conversion_require_connection);

                    Snackbar.make(contentView, message, Snackbar.LENGTH_LONG).show();
                    return;
                } catch (Exception e) {
                    Snackbar.make(contentView,
                            "Error while converting: " + e.getLocalizedMessage(),
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                MemberListItem payerItem = (MemberListItem) payerSpinner.getSelectedItem();
                final Map<String, String> memberIds = new HashMap<>();
                int numMembers = membersList.getAdapter().getCount();
                for (int i = 0; i < numMembers; i++) {
                    MemberListItem item = (MemberListItem) membersList.getItemAtPosition(i);
                    if (membersList.isItemChecked(i))
                        memberIds.put(item.id, item.name);
                }

                Map<String, Object> expense = new HashMap<>();
                expense.put("name", title);
                /// TODO Decide on a standard, strict format for the timestamp
                expense.put("timestamp", timestamp.getTime());
                expense.put("timestamp_number", -1 * timestamp.getTime());
                expense.put("amount_original", amountOriginal.toStandardFormat());
                expense.put("amount", amountBase.toStandardFormat());
                expense.put("amount_converted", amountConverted.toStandardFormat());
                expense.put("payer_id", payerItem.id);
                expense.put("payer_name", payerItem.name);
                expense.put("group_id", mGroupId);
                expense.put("members_ids", memberIds);

                /// TODO Refactor database write logic to somewhere else
                final String expenseId;
                if (isEditing())
                    expenseId = mInitialExpense.getKey();
                else
                    expenseId = mRoot.child("groups").child(mGroupId).child("expenses").push().getKey();

                Map<String, Object> update = new HashMap<>();
                update.put("groups/" + mGroupId + "/expenses/" + expenseId, expense);
                update.put("users/" + payerItem.id + "/expenses_ids_as_payer/" + expenseId, title);
                update.put("expenses/" + expenseId, expense);
                if (isEditing()) {
                    // remove expense from the old payer's expense list
                    String oldPayerId = mInitialExpense.child("payer_id").getValue(String.class);
                    update.put("users/" + oldPayerId + "/expenses_ids_as_payer/" + expenseId, null);
                }

                final String payerId4Notification = payerItem.id;

                mRoot.updateChildren(update).addOnCompleteListener(EditExpenseActivity.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            String msg = task.getException().getLocalizedMessage();
                            Snackbar.make(contentView, msg, Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if (!isEditing()) {
                            /// TODO Go to the expense details (with the newly created URI)
                            Intent i = new Intent(getApplicationContext(), ExpenseDetailsActivity.class);
                            i.setData(Utils.getUriFor(UriType.EXPENSE, expenseId));
                            i.putExtra("name", title);
                            i.putExtra("amount", amountConverted.toString());
                            i.putExtra("timestamp", timestamp.getTime());
                            startActivity(i);
                            String message4Notification = getResources().getString(R.string.new_expense_notification);
                            MessagingUtils.sendPushUpNotifications(mRoot, mGroupId, title, memberIds, message4Notification);
                        }
                        if(isEditing()) {
                            String message4Notification = getResources().getString(R.string.expense_modified);
                            MessagingUtils.sendPushUpNotifications(mRoot, mGroupId, title, memberIds,message4Notification);
                        }
                    }
                });
                finish();
            }
        }).start();
    }

    private class ValidationException extends Throwable {
        public ValidationException(String message) {
            super(message);
        }
    }
}
