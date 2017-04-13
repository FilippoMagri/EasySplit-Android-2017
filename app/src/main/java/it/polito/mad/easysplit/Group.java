package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import it.polito.mad.easysplit.models.Database;
import it.polito.mad.easysplit.models.GroupModel;
import it.polito.mad.easysplit.models.PersonModel;


public class Group extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Database db = ((MyApplication) getApplication()).getDatabase();
        /// TODO Check that the user is logged-in
        PersonModel user = db.getUser();

        SubscribedGroupListAdapter adapter = new SubscribedGroupListAdapter(this, user);

        ListView listView = (ListView) findViewById(R.id.group_list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                GroupModel group = (GroupModel) parent.getItemAtPosition(position);
                Intent i = new Intent(Group.this, GroupDetailsActivity.class);
                i.setData(group.getUri());
                startActivity(i);
            }
        });

        TextView userName = (TextView) findViewById(R.id.user_name);
        userName.setText(user.getName());
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

