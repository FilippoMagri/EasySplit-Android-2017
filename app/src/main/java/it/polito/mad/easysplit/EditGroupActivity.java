package it.polito.mad.easysplit;

import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Currency;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.models.GroupBalanceModel;

public class EditGroupActivity extends AppCompatActivity implements GroupBalanceModel.Listener {
    private CurrencySpinnerAdapter mCurrencyAdapter;
    private DatabaseReference mGroupRef;
    private EditText mNameEdit;
    private GroupBalanceModel mBalanceModel;
    private View mCurrencyWarning, mCurrencyLayout;
    private Spinner mCurrencySpinner;

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
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mBalanceModel.removeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateGroup();
    }

    private void validateData() throws ValidationException {
        if (mNameEdit.getText().toString().trim().length() == 0)
            throw new ValidationException(getString(R.string.error_validate_title_empty));
    }

    public void updateGroup() {
        try {
            validateData();
        } catch (ValidationException exc) {
            Toast.makeText(EditGroupActivity.this, exc.getMessage(), Toast.LENGTH_SHORT)
                .show();
            return;
        }

        final String newName = mNameEdit.getText().toString();

        mGroupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupSnap) {
                String groupId = mGroupRef.getKey();

                Map<String, Object> update = new HashMap<>();
                update.put("/groups/"+groupId+"/name", newName);
                for (DataSnapshot member : groupSnap.child("members_ids").getChildren()) {
                    String uid = member.getKey();
                    update.put("/users/"+uid+"/groups_ids/"+groupId, newName);
                }

                Currency newCurrency = (Currency) mCurrencySpinner.getSelectedItem();
                if (newCurrency != null) {
                    String newCurrencyCode = newCurrency.getCurrencyCode();
                    update.put("/groups/" + groupId + "/currency", newCurrencyCode);
                }

                mGroupRef.getRoot().updateChildren(update);
                finish();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                ActivityUtils.showDatabaseError(EditGroupActivity.this, databaseError);
            }
        });
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_group, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_save) {
            updateGroup();
        }

        return super.onOptionsItemSelected(item);
    }

    private class ValidationException extends Throwable {
        public ValidationException(String message) {
            super(message);
        }
    }
}
