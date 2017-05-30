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
import java.util.Currency;

import it.polito.mad.easysplit.models.Money;

public class Payment extends AppCompatActivity {

    private CurrencySpinnerAdapter mCurrenciesAdapter;
    private TextView mTvCreditor;
    private TextView mTvDebtor;
    private EditText mEditTextPayment;
    //RootMember
    String rootMemberName = "";
    String rootMemberId = "";
    String rootMemberMoney = "";
    String rootMemberSymbol = "";
    String rootMemberCurrency = "";
    //Sub Member
    String subMemberName = "";
    String subMemberId = "";
    String subMemberMoney = "";
    String subMemberSymbol = "";
    String subMemberCurrency = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        retrieveIntentInformations();

        Log.d("Payment","RootMemberName: "+rootMemberName);
        Log.d("Payment","SubMemberName: "+subMemberName);
        Log.d("Payment","SubMemberMoney"+subMemberMoney);

        mCurrenciesAdapter = new CurrencySpinnerAdapter(this);
        mCurrenciesAdapter.setOnlyGroupCurrency(subMemberCurrency);

        mTvCreditor = (TextView) findViewById(R.id.textview_creditor);
        mTvDebtor = (TextView) findViewById(R.id.textview_debtor);
        mEditTextPayment = (EditText) findViewById(R.id.amountPayment);

        Money moneyCheck = new Money(Currency.getInstance(rootMemberCurrency),new BigDecimal(rootMemberMoney));
        Money amountPayment = new Money(Currency.getInstance(subMemberCurrency),new BigDecimal(subMemberMoney));
        mEditTextPayment.setText(amountPayment.abs().toString().replace(subMemberSymbol,"").replace(" ",""));

        if (moneyCheck.cmpZero()>0) {
            //RootMember is a creditor
            mTvCreditor.setText(rootMemberName);
            mTvDebtor.setText(subMemberName);
        } else if (moneyCheck.cmpZero()<0){
            //SubMember is a creditor
            mTvCreditor.setText(subMemberName);
            mTvDebtor.setText(rootMemberName);
        }

        Spinner currencySpinner = (Spinner) findViewById(R.id.paymentSpinner);
        currencySpinner.setAdapter(mCurrenciesAdapter);

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
        this.rootMemberMoney = intent.getStringExtra("RootMemberMoney");
        this.rootMemberSymbol = intent.getStringExtra("RootMemberSymbol");
        this.rootMemberCurrency = intent.getStringExtra("RootMemberCurrency");
        //Adjust String formats
        rootMemberMoney = rootMemberMoney.replace(rootMemberSymbol,"");
        rootMemberMoney = rootMemberMoney.replace(" ","");
        //Sub Member
        this.subMemberName = intent.getStringExtra("SubMemberName");
        this.subMemberId = intent.getStringExtra("SubMemberId");
        this.subMemberMoney = intent.getStringExtra("SubMemberMoney");
        this.subMemberSymbol = intent.getStringExtra("SubMemberSymbol");
        this.subMemberCurrency = intent.getStringExtra("SubMemberCurrency");
        //Adjust String formats
        subMemberMoney = subMemberMoney.replace(subMemberSymbol,"");
        subMemberMoney = subMemberMoney.replace(" ","");
    }

}
