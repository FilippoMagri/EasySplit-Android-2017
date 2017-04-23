package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.AddExpenses;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.Utils;

public class ExpenseListFragment extends Fragment {
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    public static ExpenseListFragment newInstance(Uri groupUri) {
        ExpenseListFragment frag = new ExpenseListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);

        return frag;
    }


    private DatabaseReference mExpenseIdsRef;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = savedInstanceState;
        if (savedInstanceState == null)
            args = getArguments();

        String groupUri = (String) args.getCharSequence("groupUri");
        DatabaseReference groupRef = Utils.findByUri(Uri.parse(groupUri), mRoot);
        mExpenseIdsRef = groupRef.child("expenses_ids");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(R.id.expensesList);

        ExpenseListAdapter adapter = new ExpenseListAdapter(getContext());
        mExpenseIdsRef.addValueEventListener(adapter);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem expense = (ListItem) parent.getItemAtPosition(position);
                Intent showExpense = new Intent(getContext(), ExpenseDetailsActivity.class);
                showExpense.setData(Utils.getUriFor(Utils.UriType.EXPENSE, expense.id));
                startActivity(showExpense);
            }
        });

        /// TODO Support multiple groups
        View btnAdd = view.findViewById(R.id.add_button_expense);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), AddExpenses.class);
                startActivity(i);
            }
        });

        return view;
    }

    private static final class ListItem {
        String id, name;

        public ListItem(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private class ExpenseListAdapter extends ArrayAdapter<ListItem> implements ValueEventListener {
        public ExpenseListAdapter(Context ctx) {
            super(ctx, R.layout.expense_item);
        }

        @Override
        public synchronized void onDataChange(DataSnapshot expenseIdsSnap) {
            clear();
            for (DataSnapshot child : expenseIdsSnap.getChildren()) {
                final String expenseId = child.getKey();
                mRoot.child("expenses/"+expenseId+"/name").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot nameSnap) {
                        String expenseName = nameSnap.getValue(String.class);
                        add(new ListItem(expenseId, expenseName));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        add(new ListItem(expenseId, "???"));
                    }
                });
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyDataSetInvalidated();
        }
    }

}
