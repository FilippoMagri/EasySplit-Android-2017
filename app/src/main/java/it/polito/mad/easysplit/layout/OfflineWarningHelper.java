package it.polito.mad.easysplit.layout;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.R;

public class OfflineWarningHelper {
    private ValueEventListener mConnectionListener;
    private DatabaseReference mConnectedRef;

    public OfflineWarningHelper(Fragment fragment) {
        this(fragment.getView());
    }

    public OfflineWarningHelper(Activity activity) {
        this(activity.findViewById(android.R.id.content));
    }

    public OfflineWarningHelper(View view) {
        final View connectionWarning = view.findViewById(R.id.offline_warning);
        mConnectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot valueSnap) {
                connectionWarning.setVisibility(
                        valueSnap.getValue(Boolean.class) ?
                                View.GONE : View.VISIBLE);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        };

        mConnectedRef = FirebaseDatabase.getInstance().getReference("/.info/connected");
        mConnectedRef.addValueEventListener(mConnectionListener);
    }

    public void detach() {
        mConnectedRef.removeEventListener(mConnectionListener);
    }
}
