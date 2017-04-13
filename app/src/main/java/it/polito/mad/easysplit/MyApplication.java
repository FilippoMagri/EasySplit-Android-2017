package it.polito.mad.easysplit;

import android.app.Application;

import it.polito.mad.easysplit.models.Database;
import it.polito.mad.easysplit.models.dummy.DummyDatabase;

/**
 * Created by fgiobergia on 06/04/17.
 */

public class MyApplication extends Application {
    private Database mDatabase = new DummyDatabase();
    public Database getDatabase() {
        return mDatabase;
    }
}