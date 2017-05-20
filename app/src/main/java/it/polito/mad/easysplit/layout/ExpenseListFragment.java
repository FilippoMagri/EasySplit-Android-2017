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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import it.polito.mad.easysplit.EditExpenseActivity;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.Utils;
import it.polito.mad.easysplit.Utils.UriType;
import it.polito.mad.easysplit.models.Money;

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
        View view = inflater.inflate(layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(id.expensesList);

        ExpenseListAdapter adapter = new ExpenseListAdapter(getContext());
        mExpenseIdsRef.orderByChild("timestamp_number").addValueEventListener(adapter);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem expense = (ListItem) parent.getItemAtPosition(position);
                Intent showExpense = new Intent(getContext(), ExpenseDetailsActivity.class);
                showExpense.setData(Utils.getUriFor(UriType.EXPENSE, expense.id));
                startActivity(showExpense);
            }
        });

        /// TODO Support multiple groups
        View btnAdd = view.findViewById(id.add_button_expense);
        btnAdd.setOnClickListener(new OnClickListener() {
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
            super(ctx, layout.expense_item);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout.expense_item, parent, false);
            }

            TextView nameText = (TextView) convertView.findViewById(id.name);
            TextView amountText = (TextView) convertView.findViewById(id.amount);

            ListItem item = getItem(position);
            nameText.setText(item.name);
            amountText.setText(item.amount);

            return convertView;
        }

        @Override
        public synchronized void onDataChange(DataSnapshot expenseIdsSnap) {
            clear();
            for (DataSnapshot child : expenseIdsSnap.getChildren()) {
                String expenseId = child.getKey();
                String name = child.child("name").getValue(String.class);
                String origAmountStdStr = child.child("amount_original").getValue(String.class);
                String convAmountStdStr = child.child("amount_converted").getValue(String.class);

                Money amountOriginal = Money.parseOrFail(origAmountStdStr);
                Money amountConverted = Money.parseOrFail(convAmountStdStr);
                String amountText = amountConverted.toString();
                if (! amountOriginal.getCurrency().equals(amountConverted.getCurrency()))
                    amountText += " (" + amountOriginal.toString() + ")";

                Long timestampNumber = child.child("timestamp_number").getValue(Long.class);

                add(new ListItem(expenseId, name, amountText, timestampNumber));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyDataSetInvalidated();
        }
    }

}
