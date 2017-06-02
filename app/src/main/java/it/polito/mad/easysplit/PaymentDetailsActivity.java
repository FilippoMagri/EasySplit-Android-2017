package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;

import it.polito.mad.easysplit.models.Money;

public class PaymentDetailsActivity extends AppCompatActivity {
    private static final DateFormat sUIDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);

    private DatabaseReference mRef;

    private ValueEventListener mListener;
    private String mGroupId, mPayerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Group.class));
            }
        });

        // mRef will be something like "https://easysplit-853e4.firebaseio.com/payments/-KitTZeY14BFsnsH_rpp"
        mRef = Utils.findByUri(getIntent().getData());
        mListener = new PaymentDetailsActivity.PaymentListener();
        mRef.addValueEventListener(mListener);
    }

    private class PaymentListener implements ValueEventListener {
        private final TextView paymentStandardNameText = (TextView) findViewById(R.id.paymentStandardName);
        private final TextView creationDateText = (TextView) findViewById(R.id.paymentCreationDate);
        private final TextView payerNameText = (TextView) findViewById(R.id.paymentPayerName);
        private final TextView receiverNameText = (TextView) findViewById(R.id.receiver_payment);


        @Override
        public void onDataChange(DataSnapshot paymentSnap) {
            if (paymentSnap.getValue() == null) {
                PaymentDetailsActivity.this.finish();
                return;
            }

            mGroupId = paymentSnap.child("group_id").getValue(String.class);
            mPayerId = paymentSnap.child("payer_id").getValue(String.class);

            payerNameText.setText(paymentSnap.child("payer_name").getValue(String.class));
            paymentStandardNameText.setText(getString(R.string.standard_payment));
            Date timestamp = new Date(paymentSnap.child("timestamp").getValue(Long.class));
            creationDateText.setText(sUIDateFormat.format(timestamp));

            final String paymentId = paymentSnap.getKey();

            String totalStr = paymentSnap.child("amount").getValue(String.class);

            String originalTotalStr = paymentSnap.child("amount_original").getValue(String.class);
            if (originalTotalStr == null)
                originalTotalStr = totalStr;

            String convertedTotalStr = paymentSnap.child("amount_converted").getValue(String.class);
            if (convertedTotalStr == null)
                convertedTotalStr = totalStr;

            Money convertedTotal = Money.parseOrFail(convertedTotalStr);
            Money originalTotal = Money.parseOrFail(originalTotalStr);

            if (convertedTotal.getCurrency().equals(originalTotal.getCurrency()))
                setTitle(originalTotal.toString().replace("-",""));
            else
                setTitle(convertedTotal.toString().replace("-","") + " (" + originalTotal.toString() + ")");

            long numParticipants = paymentSnap.child("members_ids").getChildrenCount();
            if (numParticipants == 0)
                return;

            for (DataSnapshot member : paymentSnap.child("members_ids").getChildren()) {
                String memberId = member.getKey();
                String memberName = member.getValue(String.class);
                receiverNameText.setText(memberName);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            /// TODO
        }
    }
}
