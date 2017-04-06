package it.polito.mad.easysplit;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class Group extends AppCompatActivity {
    final String EXTRA_NAME= null;

    Button valid;
    Button AddParticipant;
   private String groupName = null;

    final String EXTRA_GROUP=null;
    static CreationGroup CreationGroup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

       // Bundle extras = getIntent().getExtras();
        Intent intent = getIntent();
        TextView loginDisplay = (TextView) findViewById(R.id.essai);

        if (intent != null) {
            loginDisplay.setText(intent.getStringExtra(EXTRA_GROUP));

        }


        /*final TextView label = (TextView) findViewById(R.id.label);

        valid.setOnClickListener(new View.OnClickListener()
        {final TextView label = (TextView)  findViewById(R.id.label);

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                String name = label.getText().toString();
                Intent intent = new Intent(Group.this, CreationGroup.class);
                intent.putExtra(EXTRA_NAME, name);
                startActivity(intent);
                Toast.makeText(getApplicationContext(), "Go in Group", Toast.LENGTH_SHORT).show();


            }});*/


        //tu peux donc utiliser la variable double "d"

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Group.this, CreationGroup.class);
                startActivity(intent);
              /*  Snackbar.make(view,"CreationGroup", Snackbar.LENGTH_LONG)
                        .setAction("CreationGroup", null).show();
            */
            }
        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
              /* //is working
        String[] values = new String[] { "Trip", "iPhone", "WindowsMobile",
                "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
                "Linux", "OS/2" };
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, values);
        setListAdapter(adapter);
*/

