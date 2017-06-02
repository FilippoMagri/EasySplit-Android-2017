package it.polito.mad.easysplit;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.models.GroupBalanceModel;

public class EditGroupActivity extends AppCompatActivity implements GroupBalanceModel.Listener {
    private static final long AUTOSAVE_FREQUENCY = 5000;

    private CurrencySpinnerAdapter mCurrencyAdapter;
    private DatabaseReference mGroupRef;
    private EditText mNameEdit;
    private GroupBalanceModel mBalanceModel;
    private View mCurrencyWarning, mCurrencyLayout;
    private Spinner mCurrencySpinner;
    private Handler mHandler = new Handler();

    public synchronized boolean isRunning() {
        return mIsRunning;
    }

    public synchronized void setIsRunning(boolean value) {
        mIsRunning = value;
    }

    private boolean mIsRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_group);

        mNameEdit = (EditText) findViewById(R.id.nameEdit);
        mCurrencyWarning = findViewById(R.id.currencyWarningText);
        mCurrencyLayout = findViewById(R.id.currencySpinnerLayout);
        mCurrencySpinner = (Spinner) findViewById(R.id.currencySpinner);

        mCurrencyAdapter = new CurrencySpinnerAdapter(this);
        mCurrencySpinner.setAdapter(mCurrencyAdapter);
        mCurrencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateGroup();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Uri groupUri = getIntent().getData();
        mBalanceModel = GroupBalanceModel.forGroup(groupUri);

        mGroupRef = Utils.findByUri(groupUri);
    }

    @Override
    protected void onStart() {
        super.onStart();

        mBalanceModel.addListener(this);

        mGroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                String name = groupSnap.child("name").getValue(String.class);
                mNameEdit.setText(name);

                String currencyCode = groupSnap.child("currency").getValue(String.class);
                int currencyIndex = mCurrencyAdapter.findCurrencyByCode(currencyCode);
                mCurrencySpinner.setSelection(currencyIndex);

                // Set the handler *after* having received the first data from Firebase
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        updateGroup();
                        if (isRunning())
                            mHandler.postDelayed(this, AUTOSAVE_FREQUENCY);
                    }
                }, AUTOSAVE_FREQUENCY);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        setIsRunning(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBalanceModel.removeListener(this);
        setIsRunning(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateGroup();
    }

    public void updateGroup() {
        Map<String, Object> update = new HashMap<>();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String groupId = mGroupRef.getKey();

        String newName = mNameEdit.getText().toString();
        update.put("/users/"+uid+"/groups_ids/"+groupId, newName);
        update.put("/groups/"+groupId+"/name", newName);

        Currency newCurrency = (Currency) mCurrencySpinner.getSelectedItem();
        if (newCurrency != null) {
            String newCurrencyCode = newCurrency.getCurrencyCode();
            update.put("/groups/" + groupId + "/currency", newCurrencyCode);
        }

        FirebaseDatabase.getInstance().getReference().updateChildren(update);
    }

    @Override
    public void onBalanceChanged(Map<String, GroupBalanceModel.MemberRepresentation> balance) {
        for (GroupBalanceModel.MemberRepresentation member : balance.values()) {
            if (! member.getResidue().isZero()) {
                // Somebody's not caught up yet...
                mCurrencyWarning.setVisibility(View.VISIBLE);
                mCurrencyLayout.setVisibility(View.GONE);
                return;
            }
        }

        mCurrencyWarning.setVisibility(View.GONE);
        mCurrencyLayout.setVisibility(View.VISIBLE);
    }

}
