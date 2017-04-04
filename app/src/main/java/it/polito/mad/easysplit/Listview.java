package it.polito.mad.easysplit;

import android.app.ListActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import it.polito.mad.easysplit.R;

public class Listview extends ListActivity {

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] values = new String[] { "Flat", "Trip in France", "Clara's birthday",
                "Festival Budapest" };
        // use your custom layout
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.activity_listview, R.id.label, values);
        setListAdapter(adapter);
    }
    }

