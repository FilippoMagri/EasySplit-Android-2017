package it.polito.mad.easysplit;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;

import java.util.Currency;

import it.polito.mad.easysplit.models.Money;


public class EasySplitApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ConversionRateProvider.setupInstance(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Money.setLocale(newConfig.locale);
        ConversionRateProvider.setLocaleCurrency(newConfig.locale.getDisplayLanguage());
    }
}
