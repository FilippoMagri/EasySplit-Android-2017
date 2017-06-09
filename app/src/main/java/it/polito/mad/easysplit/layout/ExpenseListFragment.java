package it.polito.mad.easysplit.layout;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Comparator;

import it.polito.mad.easysplit.EditExpenseActivity;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.Utils;
import it.polito.mad.easysplit.Utils.UriType;
import it.polito.mad.easysplit.models.Money;

public class ExpenseListFragment extends Fragment {

    private String mGroupId;

    public static ExpenseListFragment newInstance(Uri groupUri) {
        ExpenseListFragment frag = new ExpenseListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);

        return frag;
    }


    private DatabaseReference mExpensesRef;
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private String mGroupUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mGroupUri = (String) args.getCharSequence("groupUri");
        DatabaseReference groupRef = Utils.findByUri(Uri.parse(mGroupUri), mRoot);
        mGroupId = groupRef.getKey();
        mExpensesRef = groupRef.child("expenses");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(id.expensesList);

        ExpenseListAdapter adapter = new ExpenseListAdapter(getContext());
        mExpensesRef.orderByChild("timestamp").addChildEventListener(adapter);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem expense = (ListItem) parent.getItemAtPosition(position);
                Intent showExpense = new Intent(getContext(), ExpenseDetailsActivity.class);
                showExpense.setData(Utils.getUriFor(UriType.EXPENSE, expense.id));
                showExpense.putExtra("name", expense.name);
                showExpense.putExtra("amount", expense.amount);
                showExpense.putExtra("timestamp", expense.timestamp);
                showExpense.putExtra("groupId", mGroupId);
                showExpense.putExtra("payerId", expense.payerId);
                startActivity(showExpense);
            }
        });

        /// TODO Support multiple groups
        final FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(id.add_button_expense);
        btnAdd.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getContext(), EditExpenseActivity.class);
                String groupId = Uri.parse(mGroupUri).getPathSegments().get(1);
                i.putExtra("groupId", groupId);
                startActivity(i);
            }
        });
        lv.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                int btn_initPosY=btnAdd.getScrollY();
                if (scrollState == SCROLL_STATE_TOUCH_SCROLL || scrollState == SCROLL_STATE_FLING) {
                    btnAdd.animate().cancel();
                    btnAdd.animate().translationYBy(200);
                } else {
                    btnAdd.animate().cancel();
                    btnAdd.animate().translationY(btn_initPosY);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {

            }
        });
        return view;
    }

    private static final class ListItem {
        String id, name, amount, payerId;
        long timestamp;

        public ListItem(String id, String name, String amount, String payerId, long timestamp) {
            this.id = id;
            this.name = name;
            this.amount = amount;
            this.timestamp = timestamp;
            this.payerId = payerId;
        }
    }

    private class ExpenseListAdapter extends ArrayAdapter<ListItem> implements ChildEventListener {

        private Comparator<? super ListItem> mComparator = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem lhs, ListItem rhs) {
                // Reverse order!
                if (lhs.timestamp > rhs.timestamp)
                    return -1;
                if (lhs.timestamp < rhs.timestamp)
                    return 1;
                return 0;
            }
        };

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
        public void onChildAdded(DataSnapshot expenseSnap, String previousChildName) {
            addExpense(expenseSnap);
            sort(mComparator);
        }

        @Override
        public void onChildChanged(DataSnapshot expenseSnap, String previousChildName) {
            removeByKey(expenseSnap.getKey());
            onChildAdded(expenseSnap, previousChildName);
        }

        @Override
        public void onChildRemoved(DataSnapshot expenseSnap) {
            removeByKey(expenseSnap.getKey());
        }

        void removeByKey(String key) {
            for (int i=0; i < getCount(); i++) {
                ListItem item = getItem(i);
                if (item.id.equals(key)) {
                    remove(item);
                    break;
                }
            }
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            sort(mComparator);
        }

        void addExpense(DataSnapshot expenseSnap) {
            String expenseId = expenseSnap.getKey();
            String name = expenseSnap.child("name").getValue(String.class);
            String baseAmountStdStr = expenseSnap.child("amount").getValue(String.class);

            String origAmountStdStr = expenseSnap.child("amount_original").getValue(String.class);
            if (origAmountStdStr == null)
                origAmountStdStr = baseAmountStdStr;

            String convAmountStdStr = expenseSnap.child("amount_converted").getValue(String.class);
            if (convAmountStdStr == null)
                convAmountStdStr = baseAmountStdStr;

            Money amountOriginal = Money.parseOrFail(origAmountStdStr);
            Money amountConverted = Money.parseOrFail(convAmountStdStr);
            String amountText = amountConverted.toString();
            if (! amountOriginal.getCurrency().equals(amountConverted.getCurrency()))
                amountText += " (" + amountOriginal.toString() + ")";

            Long timestamp = expenseSnap.child("timestamp").getValue(Long.class);
            String payerId = expenseSnap.child("payer_id").getValue(String.class);

            add(new ListItem(expenseId, name, amountText, payerId, timestamp));
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyDataSetInvalidated();
        }
    }

}
