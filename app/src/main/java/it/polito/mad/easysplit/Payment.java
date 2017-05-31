package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;
import java.util.NoSuchElementException;

import it.polito.mad.easysplit.models.Money;

public class Payment extends AppCompatActivity {

    private CurrencySpinnerAdapter mCurrenciesAdapter;
    private TextView mTvCreditor;
    private TextView mTvDebtor;
    private EditText mEditTextPayment;
    private Spinner mCurrencySpinner;
    private CoordinatorLayout mCoordinatorLayout;

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
                Snackbar snack = Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG);
                View viewSnack = snack.getView();
                CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams)viewSnack.getLayoutParams();
                params.gravity = Gravity.TOP;
                viewSnack.setLayoutParams(params);
                snack.setAction("Action", null);
                snack.show();
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
            Log.d("Payment","rootMemberSMoney: "+rootMemberStringMoney);
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
}
