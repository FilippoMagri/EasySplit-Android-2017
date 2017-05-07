package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.EditExpenseActivity;
import java.util.Comparator;

import it.polito.mad.easysplit.AddExpenses;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.Utils;

public class ExpenseListFragment extends Fragment {
    public static ExpenseListFragment newInstance(Uri groupUri) {
        ExpenseListFragment frag = new ExpenseListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);

        return frag;
    }


    private DatabaseReference mExpenseIdsRef;
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private String mGroupUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mGroupUri = (String) args.getCharSequence("groupUri");
        DatabaseReference groupRef = Utils.findByUri(Uri.parse(mGroupUri), mRoot);
        mExpenseIdsRef = groupRef.child("expenses");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(R.id.expensesList);

        ExpenseListAdapter adapter = new ExpenseListAdapter(getContext());
        mExpenseIdsRef.orderByChild("timestamp_number").addValueEventListener(adapter);
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
                Intent i = new Intent(getContext(), EditExpenseActivity.class);
                String groupId = Uri.parse(mGroupUri).getPathSegments().get(1);
                i.putExtra("groupId", groupId);
                startActivity(i);
            }
        });

        return view;
    }

    private static final class ListItem {
        String id, name, amount;
        Long timeStamp_number;

        public ListItem(String id, String name, String amount, Long timeStamp_number) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.timeStamp_number=timeStamp_number;
        }
    }

    private class ExpenseListAdapter extends ArrayAdapter<ListItem> implements ValueEventListener {
        public ExpenseListAdapter(Context ctx) {
            super(ctx, R.layout.expense_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.expense_item, parent, false);
            }

            TextView nameText = (TextView) convertView.findViewById(R.id.name);
            TextView amountText = (TextView) convertView.findViewById(R.id.amount);

            ListItem item = getItem(position);
            nameText.setText(item.name);
            amountText.setText(item.amount);

            return convertView;
        }

        @Override
        public synchronized void onDataChange(DataSnapshot expenseIdsSnap) {
            clear();
            for (DataSnapshot child : expenseIdsSnap.getChildren()) {
                final String expenseId = child.getKey();
                mRoot.child("expenses/"+expenseId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot expenseSnap) {
                        String name = expenseSnap.child("name").getValue(String.class);
                        String amount = expenseSnap.child("amount").getValue(String.class);
                        Long timeStamp_number = expenseSnap.child("timestamp_number").getValue(Long.class);
                        add(new ListItem(expenseId, name, amount,timeStamp_number));
                        sort(new Comparator<ListItem>() {
                            @Override
                            public int compare(ListItem listItem1, ListItem listItem2) {
                                return listItem1.timeStamp_number.compareTo(listItem2.timeStamp_number);
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        add(new ListItem(expenseId, "???", "???",new Long("1")));
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
