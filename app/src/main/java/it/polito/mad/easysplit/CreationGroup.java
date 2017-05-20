package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

public class CreationGroup extends AppCompatActivity {
    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    private static CurrencySpinnerAdapter mCurrenciesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creation_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Group.class));
            }
        });

        setTitle(getString(R.string.title_new_group));

        final EditText groupNameEdit = (EditText) findViewById(R.id.nameGroup);
        final EditText participantsListEdit = (EditText) findViewById(R.id.newGroupParticipantsList);
        final Spinner currencySpinner = (Spinner) findViewById(R.id.currencySpinner);
        ImageView submit = (ImageView) findViewById(R.id.valid);

        mCurrenciesAdapter = new CurrencySpinnerAdapter(this);
        currencySpinner.setAdapter(mCurrenciesAdapter);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            private int getColorFromString(String str) {
                return isValidEmail(str) ? Color.GREEN : Color.RED;
            }

            @Override
            public void afterTextChanged(Editable s) {
                Character[] spacingChars = {' ', '\t', '\n'};

                String str = s.toString();
                SpannableString text = new SpannableString(str);
                int position = participantsListEdit.getSelectionStart();
                int start;
                for (start = 0; start < s.length(); ) {
                    int end = start;
                    while (end < str.length() && !Arrays.asList(spacingChars).contains(str.charAt(end))) {
                        end++;
                    }
                    text.setSpan(new BackgroundColorSpan(getColorFromString(str.substring(start, end))), start, end, 0);
                    start = end + 1;
                    while (start < str.length() && Arrays.asList(spacingChars).contains(str.charAt(start))) {
                        start++;
                    }
                }

                /* set new text (with highlights) */
                participantsListEdit.removeTextChangedListener(this);
                participantsListEdit.setText(text);
                participantsListEdit.addTextChangedListener(this);
                participantsListEdit.setSelection(position); // update to previous position */
            }
        };
        participantsListEdit.addTextChangedListener(tw);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatabaseReference root = FirebaseDatabase.getInstance().getReference();
                root.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot usersSnap) {
                        String participantsStr = participantsListEdit.getText().toString();
                        final List<String> participants = Arrays.asList(participantsStr.split("\\s+"));
                        final String groupName = groupNameEdit.getText().toString();
                        final Currency currency = (Currency) currencySpinner.getSelectedItem();

                        Tasks.call(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                createGroup(groupName, currency.getCurrencyCode(), participants);
                                return null;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

            }
        });

    }

    private boolean isValidEmail(String str) {
        // TODO Add checks on email validity here!
        return str.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-z]+$");
    }

    private void createGroup(String groupName, String currencyCode, List<String> emails) {
        ArrayList<String> externalEmails = new ArrayList<>(emails);
        HashMap<String, String> groupMembers = new HashMap<>();
        HashMap<String, Object> childUpdates = new HashMap<>();

        String groupKey = mRoot.child("groups").push().getKey();

        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String signin_complete_name = sharedPref.getString("signin_complete_name", null);

        // Finally, add current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        groupMembers.put(user.getUid(), signin_complete_name);
        childUpdates.put("/users/" + user.getUid() + "/groups_ids/" + groupKey, groupName);

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", groupName);
        map.put("currency", currencyCode);
        map.put("expenses_ids", new HashMap<String, Boolean>());
        map.put("members_ids", groupMembers);

        childUpdates.put("/groups/" + groupKey, map);
        mRoot.updateChildren(childUpdates);

        finish();
    }
}