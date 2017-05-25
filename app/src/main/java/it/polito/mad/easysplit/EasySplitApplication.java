package it.polito.mad.easysplit;

import android.app.Application;


public class EasySplitApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ConversionRateProvider.setupInstance(this);
    }
}
