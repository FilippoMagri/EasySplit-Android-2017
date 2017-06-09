package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Currency;
import java.util.HashMap;
import java.util.concurrent.Callable;

public class CreationGroup extends AppCompatActivity {
    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    private Handler mHandler;

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
        final Spinner currencySpinner = (Spinner) findViewById(R.id.currencySpinner);
        final ImageView submit = (ImageView) findViewById(R.id.valid);

        CurrencySpinnerAdapter mCurrenciesAdapter = new CurrencySpinnerAdapter(this);
        currencySpinner.setAdapter(mCurrenciesAdapter);

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                if (msg.what == ValidationException.EMPTY_TITLE) {
                    Toast.makeText(CreationGroup.this, R.string.error_validate_title_empty, Toast.LENGTH_LONG)
                        .show();
                    return true;
                }

                return false;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String groupName = groupNameEdit.getText().toString();
                final Currency currency = (Currency) currencySpinner.getSelectedItem();

                Tasks.call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        try {
                            createGroup(groupName, currency.getCurrencyCode());
                        } catch (ValidationException validationExc) {
                            mHandler.sendEmptyMessage(validationExc.getKind());
                        }
                        return null;
                    }
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        ActivityUtils.confirmDiscardChanges(this);
    }

    static class ValidationException extends Exception {
        public static final int EMPTY_TITLE = 1;

        private int kind;

        public ValidationException(int kind) {
            super();
            this.kind = kind;
        }

        public int getKind() {
            return kind;
        }
    }

    private void createGroup(String groupName, String currencyCode) throws ValidationException {
        groupName = groupName.trim();
        if (groupName.length() == 0)
            throw new ValidationException(ValidationException.EMPTY_TITLE);

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
        map.put("members_ids", groupMembers);

        childUpdates.put("/groups/" + groupKey, map);
        mRoot.updateChildren(childUpdates);

        finish();
    }
}