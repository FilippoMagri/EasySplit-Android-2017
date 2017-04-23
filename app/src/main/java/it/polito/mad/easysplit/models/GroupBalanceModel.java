package it.polito.mad.easysplit.models;

import android.net.Uri;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.Utils;

public class GroupBalanceModel {

    // At the moment, recompute() is called a few more times than strictly necessary.
    // In particular, it's called every time *anything* changes in an expense (e.g. name included),
    // and once for each expense added/removed from a group (even when they're added and/or removed
    // atomically).  Shouldn't be a big problem; probably not worth to fix it.


    public interface Listener {
        void onChanged(Map<String, Money> balances);
        void onCancelled(DatabaseError error);
    }


    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private ExpenseSetListener mExpenseSetListener = new ExpenseSetListener();

    private HashMap<String, DataSnapshot> mExpenseSnaps = new HashMap<>();
    private HashMap<String, DatabaseReference> mExpenseRefs = new HashMap<>();
    private ExpenseListener mExpenseListener = new ExpenseListener();

    private ArrayList<Listener> mListeners = new ArrayList<>();

    public GroupBalanceModel(Uri groupUri) {
        DatabaseReference groupRef = Utils.findByUri(groupUri, mRoot);
        groupRef.child("expenses_ids").addChildEventListener(mExpenseSetListener);
    }


    private final class ExpenseSetListener implements ChildEventListener {
        @Override
        public void onChildAdded(DataSnapshot expenseIdSnap, String s) {
            String expenseId = expenseIdSnap.getKey();
            DatabaseReference expenseRef = mRoot.child("expenses").child(expenseId);
            expenseRef.addValueEventListener(mExpenseListener);
            mExpenseRefs.put(expenseId, expenseRef);
        }

        @Override
        public void onChildChanged(DataSnapshot expenseIdSnap, String s) {
            Boolean value = expenseIdSnap.getValue(Boolean.class);
            if (value == null)
                return;
            if (value == Boolean.TRUE)
                onChildAdded(expenseIdSnap, s);
            else
                onChildRemoved(expenseIdSnap);
        }

        @Override
        public void onChildRemoved(DataSnapshot expenseIdSnap) {
            String expenseId = expenseIdSnap.getKey();
            DatabaseReference ref = mExpenseRefs.get(expenseId);
            if (ref == null)
                return;
            ref.removeEventListener(mExpenseListener);
            mExpenseRefs.remove(expenseId);
            mExpenseSnaps.remove(expenseId);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyCancelled(databaseError);
        }
    }

    private final class ExpenseListener implements ValueEventListener {
        @Override
        public void onDataChange(DataSnapshot expenseSnap) {
            mExpenseSnaps.put(expenseSnap.getKey(), expenseSnap);
            recompute();
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyCancelled(databaseError);
        }
    }


    private void recompute() {
        HashMap<String, Money> balances = new HashMap<>();

        for (Map.Entry<String, DataSnapshot> entry : mExpenseSnaps.entrySet()) {
            String expenseId = entry.getKey();
            DataSnapshot expense = entry.getValue();

            int numPeople = (int) expense.child("members_ids").getChildrenCount();
            if (numPeople == 0)
                continue;

            for (DataSnapshot member : expense.child("members_ids").getChildren())
                if (! balances.containsKey(member.getKey()))
                    balances.put(member.getKey(), new Money(0));

            if (numPeople == 1)
                continue;

            String payerId = expense.child("payer_id").getValue(String.class);
            Money amount = Money.parse(expense.child("amount").getValue(String.class));
            Money quota = amount.div(numPeople);

            for (DataSnapshot member : expense.child("members_ids").getChildren()) {
                String memberId = member.getKey();
                if (memberId.equals(payerId))
                    balances.get(memberId).add(quota.mul(numPeople - 1));
                else
                    balances.get(memberId).add(quota.neg());
            }
        }

        for (Listener listener : new ArrayList<>(mListeners))
            listener.onChanged(balances);
    }

    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        mListeners.remove(mListeners.indexOf(listener));
    }

    private void notifyCancelled(DatabaseError error) {
        for (Listener listener : new ArrayList<>(mListeners))
            listener.onCancelled(error);
    }
}