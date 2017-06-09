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
import java.util.Locale;

import it.polito.mad.easysplit.PaymentDetailsActivity;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.Utils;
import it.polito.mad.easysplit.Utils.UriType;
import it.polito.mad.easysplit.models.Money;

public class PaymentListFragment extends Fragment {

    private String mGroupId;

    public static PaymentListFragment newInstance(Uri groupUri) {
        PaymentListFragment frag = new PaymentListFragment();

        Bundle args = new Bundle();
        args.putCharSequence("groupUri", groupUri.toString());
        frag.setArguments(args);

        return frag;
    }


    private DatabaseReference mPaymentsRef;
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    private String mGroupUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mGroupUri = (String) args.getCharSequence("groupUri");
        DatabaseReference groupRef = Utils.findByUri(Uri.parse(mGroupUri), mRoot);
        mGroupId = groupRef.getKey();
        mPaymentsRef = groupRef.child("payments");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(id.expensesList);

        PaymentListAdapter adapter = new PaymentListAdapter(getContext());
        mPaymentsRef.orderByChild("timestamp").addChildEventListener(adapter);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem payment = (ListItem) parent.getItemAtPosition(position);
                Intent showPayment = new Intent(getContext(), PaymentDetailsActivity.class);
                showPayment.setData(Utils.getUriFor(UriType.PAYMENT, payment.id));
                showPayment.putExtra("name", payment.name);
                showPayment.putExtra("amount", payment.amount);
                showPayment.putExtra("groupId", mGroupId);
                showPayment.putExtra("payerId", payment.payerId);
                startActivity(showPayment);
            }
        });

        /// TODO Support multiple groups
        final FloatingActionButton btnAdd = (FloatingActionButton) view.findViewById(id.add_button_expense);
        btnAdd.hide();

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
            this.payerId = payerId;
            this.timestamp = timestamp;
        }
    }

    private class PaymentListAdapter extends ArrayAdapter<ListItem> implements ChildEventListener {

        private Comparator<? super ListItem> mComparator = new Comparator<ListItem>() {
            @Override
            public int compare(ListItem lhs, ListItem rhs) {
                // Reverse order!
                if (lhs.timestamp < rhs.timestamp)
                    return 1;
                if (lhs.timestamp > rhs.timestamp)
                    return -1;
                return 0;
            }
        };

        public PaymentListAdapter(Context ctx) {
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
        public void onCancelled(DatabaseError databaseError) {
            notifyDataSetInvalidated();
        }

        @Override
        public void onChildAdded(DataSnapshot paymentSnap, String s) {
            String paymentId = paymentSnap.getKey();
            String name = paymentSnap.child("payer_name").getValue(String.class);
            String baseAmountStdStr = paymentSnap.child("amount").getValue(String.class);

            String origAmountStdStr = paymentSnap.child("amount_original").getValue(String.class);
            if (origAmountStdStr == null)
                origAmountStdStr = baseAmountStdStr;

            String convAmountStdStr = paymentSnap.child("amount_converted").getValue(String.class);
            if (convAmountStdStr == null)
                convAmountStdStr = baseAmountStdStr;

            Money amountOriginal = Money.parseOrFail(origAmountStdStr);
            Money amountConverted = Money.parseOrFail(convAmountStdStr);

            String amountText = amountConverted.toString().replace("-","");
            if (! amountOriginal.getCurrency().equals(amountConverted.getCurrency()))
                amountText += " (" + amountOriginal.toString() + ")";

            // Change the message programmatically ,depending on the codeCountry,
            // by avoiding "fragment not attached to activity exception".
            // And avoiding also the problem of duplication of the message Has Payed
            // In case we use concat in the getView Method , of the fragment

            String codeCountry = Locale.getDefault().getDisplayLanguage();
            if (codeCountry.equals("italiano")) {
                String messageHasPayed = "Ha pagato";
                amountText = messageHasPayed.concat(" ").concat(amountText);
            } else if (codeCountry.equals("English")) {
                String messageHasPayed = "Has payed";
                amountText = messageHasPayed.concat(" ").concat(amountText);
            }

            Long timestamp = paymentSnap.child("timestamp").getValue(Long.class);
            String payerId = paymentSnap.child("payer_id").getValue(String.class);

            add(new ListItem(paymentId, name, amountText, payerId, timestamp));
            sort(mComparator);
        }

        @Override
        public void onChildChanged(DataSnapshot paymentSnap, String previousChildName) {
            removeByKey(paymentSnap.getKey());
            onChildAdded(paymentSnap, previousChildName);
        }

        private void removeByKey(String key) {
            for (int i=0; i < getCount(); i++) {
                ListItem item = getItem(i);
                if (item.id.equals(key)) {
                    remove(item);
                    return;
                }
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot paymentSnap) {
            removeByKey(paymentSnap.getKey());
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            sort(mComparator);
        }
    }

}
