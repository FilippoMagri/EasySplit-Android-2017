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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import it.polito.mad.easysplit.EditExpenseActivity;
import it.polito.mad.easysplit.ExpenseDetailsActivity;
import it.polito.mad.easysplit.Payment;
import it.polito.mad.easysplit.PaymentDetailsActivity;
import it.polito.mad.easysplit.R;
import it.polito.mad.easysplit.R.id;
import it.polito.mad.easysplit.R.layout;
import it.polito.mad.easysplit.Utils;
import it.polito.mad.easysplit.Utils.UriType;
import it.polito.mad.easysplit.models.Money;

public class PaymentListFragment extends Fragment {
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
        mPaymentsRef = groupRef.child("payments");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(layout.content_expenses_list, container, false);
        ListView lv = (ListView) view.findViewById(id.expensesList);

        PaymentListAdapter adapter = new PaymentListAdapter(getContext());
        mPaymentsRef.orderByChild("timestamp").addValueEventListener(adapter);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem payment = (ListItem) parent.getItemAtPosition(position);
                Intent showPayment = new Intent(getContext(), PaymentDetailsActivity.class);
                showPayment.setData(Utils.getUriFor(UriType.PAYMENT, payment.id));
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
        String id, name, amount;

        public ListItem(String id, String name, String amount) {
            this.id = id;
            this.name = name;
            this.amount = amount;
        }
    }

    private class PaymentListAdapter extends ArrayAdapter<ListItem> implements ValueEventListener {
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
        public synchronized void onDataChange(DataSnapshot paymentIdsSnap) {
            ArrayList<ListItem> items = new ArrayList<>();

            for (DataSnapshot child : paymentIdsSnap.getChildren()) {
                String paymentId = child.getKey();
                String name = child.child("payer_name").getValue(String.class);
                String baseAmountStdStr = child.child("amount").getValue(String.class);

                String origAmountStdStr = child.child("amount_original").getValue(String.class);
                if (origAmountStdStr == null)
                    origAmountStdStr = baseAmountStdStr;

                String convAmountStdStr = child.child("amount_converted").getValue(String.class);
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

                items.add(new ListItem(paymentId, name, amountText));
            }

            Collections.reverse(items);
            clear();
            addAll(items);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            notifyDataSetInvalidated();
        }
    }

}
