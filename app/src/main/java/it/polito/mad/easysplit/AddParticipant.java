package it.polito.mad.easysplit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

/**
 * Created by isen on 05/04/2017.
 */

public class AddParticipant extends Activity{
    final String EXTRA_PARTICIPANT = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_creation_group);


        Intent i1 = getIntent();
        TextView loginDisplays = (TextView) findViewById(R.id.Participant);

        if (i1!=null) {
            loginDisplays.setText(i1.getStringExtra(EXTRA_PARTICIPANT));

        }
    };}
