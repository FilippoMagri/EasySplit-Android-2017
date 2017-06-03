package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import it.polito.mad.easysplit.cloudMessaging.MessagingUtils;
import it.polito.mad.easysplit.models.Money;

public class Payment extends AppCompatActivity {

    private CurrencySpinnerAdapter mCurrenciesAdapter;
    private TextView mTvCreditor;
    private TextView mTvDebtor;
    private EditText mEditTextPayment;
    private Spinner mCurrencySpinner;
    private CoordinatorLayout mCoordinatorLayout;
    private String mGroupId="";

    //RootMember
    String rootMemberName = "";
    String rootMemberId = "";
    String rootMemberStringMoney = "";
    String rootMemberSymbol = "";
    String rootMemberCurrency = "";
    Money moneyRootMember = new Money(new BigDecimal("0.00"));
    //Sub Member
    String subMemberName = "";
    String subMemberId = "";
    String subMemberStringMoney = "";
    String subMemberSymbol = "";
    String subMemberCurrency = "";
    Money moneySubMember = new Money(new BigDecimal("0.00"));

    private static final DecimalFormat mDecimalFormat = new DecimalFormat("#,##0.00");
    private static final int CONVERSION_TIMEOUT_SECS = 5;
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        resetDecimalFormat();

        mTvCreditor = (TextView) findViewById(R.id.textview_creditor);
        mTvDebtor = (TextView) findViewById(R.id.textview_debtor);
        mEditTextPayment = (EditText) findViewById(R.id.amountPayment);
        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_payment);
        mCurrencySpinner = (Spinner) findViewById(R.id.paymentSpinner);

        retrieveIntentInformations();

        mEditTextPayment.setText(moneySubMember.abs().toString().replace(subMemberSymbol,"").replace(" ",""));

        if (moneyRootMember.cmpZero()>0) {
            //RootMember is a creditor
            mTvCreditor.setText(rootMemberName);
            mTvDebtor.setText(subMemberName);
        } else if (moneyRootMember.cmpZero()<0){
            //SubMember is a creditor
            mTvCreditor.setText(subMemberName);
            mTvDebtor.setText(rootMemberName);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                consistencyCheckMoneySubMember();
                acceptPayment(moneySubMember);
            }
        });
    }

    private void retrieveIntentInformations() {
        Intent intent = getIntent();
        //RootMember
        this.rootMemberName = intent.getStringExtra("RootMemberName");
        this.rootMemberId = intent.getStringExtra("RootMemberId");
        this.rootMemberSymbol = intent.getStringExtra("RootMemberSymbol");
        this.rootMemberStringMoney = intent.getStringExtra("RootMemberMoney").replace(rootMemberSymbol,"").replace(" ","");
        this.rootMemberCurrency = intent.getStringExtra("RootMemberCurrency");
         //Sub Member
        this.subMemberName = intent.getStringExtra("SubMemberName");
        this.subMemberId = intent.getStringExtra("SubMemberId");
        this.subMemberSymbol = intent.getStringExtra("SubMemberSymbol");
        this.subMemberStringMoney = intent.getStringExtra("SubMemberMoney").replace(subMemberSymbol,"").replace(" ","");
        this.subMemberCurrency = intent.getStringExtra("SubMemberCurrency");
        //Set Currency Adapter For Payment
        mCurrenciesAdapter = new CurrencySpinnerAdapter(this);
        mCurrenciesAdapter.setOnlyGroupCurrency(subMemberCurrency);
        mCurrencySpinner.setAdapter(mCurrenciesAdapter);
        //Retrieve Currency From The Spinner
        Currency currency = (Currency) mCurrencySpinner.getSelectedItem();
        //Retrieve and convert Money of the RootMember into moneyRootMember
        try {
            // amount take the value of price + currencyCode
            String codeCountry = Locale.getDefault().getDisplayLanguage();
            if (codeCountry.equals("italiano")) {
                rootMemberStringMoney = rootMemberStringMoney.replace(".", ",");
            } else if (codeCountry.equals("English")) {
                rootMemberStringMoney = rootMemberStringMoney.replace(",", ".");
            }
            BigDecimal price = (BigDecimal) BigDecimal.valueOf(mDecimalFormat.parse(rootMemberStringMoney).doubleValue());
            //Rounding with 2 Numbers After dot
            price = price.divide(new BigDecimal("1.00"), 2, RoundingMode.HALF_UP);
            moneyRootMember = new Money(currency, price);
        } catch (NoSuchElementException | ParseException exc) {
            exc.printStackTrace();
            Snackbar.make(mCoordinatorLayout, "Invalid Root money amount!", Snackbar.LENGTH_LONG).show();
            return;
        }
        //Retrieve and convert Money of the SubMember into moneySubMember
        try {
            // amount take the value of price + currencyCode
            String codeCountry = Locale.getDefault().getDisplayLanguage();
            if (codeCountry.equals("italiano")) {
                subMemberStringMoney = subMemberStringMoney.replace(".", ",");
            } else if (codeCountry.equals("English")) {
                subMemberStringMoney = subMemberStringMoney.replace(",", ".");
            }
            BigDecimal price = (BigDecimal) BigDecimal.valueOf(mDecimalFormat.parse(subMemberStringMoney).doubleValue());
            //Rounding with 2 Numbers After dot
            price = price.divide(new BigDecimal("1.00"), 2, RoundingMode.HALF_UP);
            moneySubMember = new Money(currency, price);
        } catch (NoSuchElementException | ParseException exc) {
            Snackbar.make(mCoordinatorLayout, "Invalid Sub money amount!", Snackbar.LENGTH_LONG).show();
            return;
        }
        //Retrieve the GroupIdOfTheBalance
        this.mGroupId = intent.getStringExtra("GroupId");
    }

    @Override
    protected void onResume() {
        super.onResume();
        resetDecimalFormat();
    }

    private void resetDecimalFormat() {
        // Reset the decimal format configuration, in case the system's locale configuration
        // has changed while the Activity was not running
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        symbols.setGroupingSeparator('\'');
        if (Locale.getDefault().getDisplayLanguage().equals("italiano")) {
            symbols.setDecimalSeparator(',');
        } else if (Locale.getDefault().getDisplayLanguage().equals("English")) {
            symbols.setDecimalSeparator('.');
        }
        mDecimalFormat.setDecimalFormatSymbols(symbols);
        mDecimalFormat.setParseBigDecimal(true);
    }

    private void acceptPayment(final Money amountOriginal) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ConversionRateProvider conversionProvider = ConversionRateProvider.getInstance();
                final Money amountBase, amountConverted;

                try {
                    Task<Money> conversion = conversionProvider.convertToBase(amountOriginal);
                    amountBase = Tasks.await(conversion, CONVERSION_TIMEOUT_SECS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Snackbar.make(mCoordinatorLayout,
                            "Error while converting: " + e.getLocalizedMessage(),
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                try {
                    Currency groupCurrency = (Currency) mCurrencySpinner.getSelectedItem();
                    Task<Money> conversion = conversionProvider.convertFromBase(amountBase, groupCurrency);
                    amountConverted = Tasks.await(conversion, CONVERSION_TIMEOUT_SECS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    Snackbar.make(mCoordinatorLayout,
                            "Error while converting: " + e.getLocalizedMessage(),
                            Snackbar.LENGTH_LONG).show();
                    return;
                }

                Date timestamp = Calendar.getInstance().getTime();
                Map<String, Object> payment = new HashMap<>();
                /// TODO Decide on a standard, strict format for the timestamp
                payment.put("timestamp", timestamp.getTime());
                payment.put("timestamp_number", -1 * timestamp.getTime());
                payment.put("amount_original", amountOriginal.toStandardFormat());
                payment.put("amount", amountBase.toStandardFormat());
                payment.put("amount_converted", amountConverted.toStandardFormat());
                //memberIds inside the payment will represent only the member that will receive the payment
                final Map<String, String> memberIds = new HashMap<>();
                if (moneyRootMember.cmpZero()>0) {
                    //RootMember is a creditor and subMemberHasToPay
                    payment.put("payer_id", subMemberId);
                    payment.put("payer_name", subMemberName);
                    memberIds.put(rootMemberId,rootMemberName);
                } else if (moneyRootMember.cmpZero()<0){
                    //SubMember is a creditor and rootMemberHasToPay
                    payment.put("payer_id", rootMemberId);
                    payment.put("payer_name", rootMemberName);
                    memberIds.put(subMemberId,subMemberName);
                }

                payment.put("group_id", mGroupId);
                payment.put("members_ids", memberIds);

                final String paymentId = mRoot.child("payments").push().getKey();


                Map<String, Object> update = new HashMap<>();

                update.put("payments/"+paymentId , payment);
                update.put("users/" + payment.get("payer_id").toString() + "/payments_ids_as_payer/" + paymentId, payment);
                update.put("groups/" + mGroupId+"/payments/"+paymentId, payment);

                final String payerId4Notification = payment.get("payer_id").toString();

                mRoot.updateChildren(update).addOnCompleteListener(Payment.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (!task.isSuccessful()) {
                            String msg = task.getException().getLocalizedMessage();
                            Snackbar.make(mCoordinatorLayout, msg, Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        if (task.isSuccessful()) {
                            String message4Notification = getResources().getString(R.string.payment_received);
                            MessagingUtils.sendPushUpNotifications(mRoot, mGroupId, amountOriginal.getAmount().abs().toString(),memberIds,message4Notification);
                            Snackbar.make(mCoordinatorLayout, "Payment Effettuato", Snackbar.LENGTH_LONG).show();
                            onBackPressed();
                        }
                        finish();
                    }
                });
            }
        }).start();
    }

    private void consistencyCheckMoneySubMember() {
        // If the user decided to insert a new amount of payment
        // different from what the application is suggesting
        // it's necessary to make again the conversion of the amount
        if (!mEditTextPayment.getText().toString().equals(moneySubMember.abs().toString().replace(subMemberSymbol,"").replace(" ",""))) {
            //Retrieve Currency From The Spinner
            Currency currency = (Currency) mCurrencySpinner.getSelectedItem();
            subMemberStringMoney = mEditTextPayment.getText().toString();
            //Retrieve and convert Money of the SubMember into moneySubMember
            try {
                // amount take the value of price + currencyCode
                String codeCountry = Locale.getDefault().getDisplayLanguage();
                if (codeCountry.equals("italiano")) {
                    subMemberStringMoney = subMemberStringMoney.replace(".", ",");
                } else if (codeCountry.equals("English")) {
                    subMemberStringMoney = subMemberStringMoney.replace(",", ".");
                }
                BigDecimal price = (BigDecimal) BigDecimal.valueOf(mDecimalFormat.parse(subMemberStringMoney).doubleValue());
                //Rounding with 2 Numbers After dot
                price = price.divide(new BigDecimal("1.00"), 2, RoundingMode.HALF_UP);
                moneySubMember = new Money(currency, price);
            } catch (NoSuchElementException | ParseException exc) {
                Snackbar.make(mCoordinatorLayout, "Invalid Sub money amount!", Snackbar.LENGTH_LONG).show();
                return;
            }
        }
        // We have to be sure that regardless
        // what the user is inserting , or what we are receiving from the intent
        // the MoneySubMember internally must be always a number < 0. Otherwise the insert
        // into the DB will not be considered from the groupBalance calculation like something
        // to deduct from the creditor-balance.
        if (moneySubMember.cmpZero()>0) {
            moneySubMember = moneySubMember.neg();
        }
    }
}
