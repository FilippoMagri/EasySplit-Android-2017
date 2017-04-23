package it.polito.mad.easysplit;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class Group extends AppCompatActivity {
    private static String TAG = Group.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseAuth.getInstance().addAuthStateListener(new AuthListener());

        ActivityUtils.requestLogin(this);
    }

    private final class AuthListener implements FirebaseAuth.AuthStateListener {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();

            ListView listView = (ListView) findViewById(R.id.group_list);
            TextView userName = (TextView) findViewById(R.id.user_name);

            if (user == null) {
                listView.setAdapter(null);
                listView.setOnItemClickListener(null);
                userName.setText("(not logged in)");
                return;
            }

            SubscribedGroupListAdapter adapter = new SubscribedGroupListAdapter(Group.this, user.getUid());

            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SubscribedGroupListAdapter.Item item = (SubscribedGroupListAdapter.Item) parent.getItemAtPosition(position);
                    Intent i = new Intent(Group.this, GroupDetailsActivity.class);
                    i.setData(Utils.getUriFor(Utils.UriType.GROUP, item.id));
                    startActivity(i);
                }
            });

            Log.d(TAG, "setUser: " + user.getDisplayName() + "[" + user.getEmail() + "]");
            userName.setText(user.getDisplayName());
        }
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
