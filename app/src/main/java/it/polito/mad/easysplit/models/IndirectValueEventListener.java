package it.polito.mad.easysplit.models;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public abstract class IndirectValueEventListener implements ValueEventListener {
    private DatabaseReference mRef = null;

    protected abstract DatabaseReference getTarget(String key, DatabaseReference root);

    public IndirectValueEventListener(final DatabaseReference idRef) {
        idRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getValue(String.class);
                if (mRef != null)
                    mRef.removeEventListener(this);
                mRef = getTarget(key, idRef.getRoot());
                mRef.addValueEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                IndirectValueEventListener.this.onCancelled(databaseError);
            }
        });
    }

    public void detach() {
        if (mRef != null)
            mRef.removeEventListener(this);
    }
}
