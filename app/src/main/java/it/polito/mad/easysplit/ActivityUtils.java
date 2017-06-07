package it.polito.mad.easysplit;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;

import java.util.Currency;
import java.util.HashMap;

public class ActivityUtils {
    private ActivityUtils() { }

    public static void requestLogin(Context ctx) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null)
            return;

        Intent i = new Intent(ctx, LoginActivity.class);
        ctx.startActivity(i);
    }
    
    
    private static HashMap<String, Integer> currencyNames = null;
    public static String getCurrencyName(Context ctx, String currencyCode) {
        if (currencyNames == null) {
            currencyNames = new HashMap<>();
            // Use reflection?
            currencyNames.put("EUR", R.string.currency_EUR);
            currencyNames.put("USD", R.string.currency_USD);
            currencyNames.put("JPY", R.string.currency_JPY);
            currencyNames.put("GBP", R.string.currency_GBP);
            currencyNames.put("AUD", R.string.currency_AUD);
            currencyNames.put("CAD", R.string.currency_CAD);
            currencyNames.put("CHF", R.string.currency_CHF);
            currencyNames.put("CNY", R.string.currency_CNY);
            currencyNames.put("SEK", R.string.currency_SEK);
            currencyNames.put("NZD", R.string.currency_NZD);
            currencyNames.put("MXN", R.string.currency_MXN);
            currencyNames.put("SGD", R.string.currency_SGD);
            currencyNames.put("HKD", R.string.currency_HKD);
            currencyNames.put("NOK", R.string.currency_NOK);
            currencyNames.put("KRW", R.string.currency_KRW);
            currencyNames.put("TRY", R.string.currency_TRY);
            currencyNames.put("RUB", R.string.currency_RUB);
            currencyNames.put("INR", R.string.currency_INR);
            currencyNames.put("BRL", R.string.currency_BRL);
            currencyNames.put("ZAR", R.string.currency_ZAR);
        }
        
        return ctx.getResources().getString(currencyNames.get(currencyCode));
    }

    public static String getCurrencyName(Context ctx, Currency currency) {
        return getCurrencyName(ctx, currency.getCurrencyCode());
    }

    public static void showDatabaseError(final Activity activity, DatabaseError databaseError) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.error_generic_title)
                .setMessage(activity.getString(R.string.error_database_generic) + databaseError.getMessage())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                })
                .show();
    }

    static void confirmDiscardChanges(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.unsaved_confirm_title)
                .setMessage(R.string.unsaved_confirm_message)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    public static void showDatabaseError(final Activity activity, DatabaseError databaseError) {
        new AlertDialog.Builder(activity)
                .setCancelable(false)
                .setTitle(R.string.error_generic_title)
                .setMessage(activity.getString(R.string.error_database_generic) + databaseError.getMessage())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                })
                .show();
    }
}
