package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class Group extends AppCompatActivity {
    private static String TAG = Group.class.getName();
    private ListView mGroupListView;
    private TextView mUserNameText;
    private TextView mNoGroupsText;
    private AuthListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        setSupportActionBar(toolbar);

        mGroupListView = (ListView) findViewById(R.id.group_list);
        mUserNameText = (TextView) findViewById(R.id.user_name);
        mNoGroupsText = (TextView) findViewById(R.id.noGroupsText);
        mAuthListener = new AuthListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        ActivityUtils.requestLogin(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseAuth.getInstance().removeAuthStateListener(mAuthListener);
    }

    private final class AuthListener implements FirebaseAuth.AuthStateListener {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null)
                onLogOut();
            else
                onLogIn(user);
        }

        void onLogIn(final FirebaseUser user) {
            final SubscribedGroupListAdapter adapter =
                    new SubscribedGroupListAdapter(Group.this, user.getUid());

            adapter.registerDataSetObserver(new DataSetObserver() {
                @Override
                public void onChanged() {
                    mNoGroupsText.setVisibility(adapter.getCount() == 0 ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onInvalidated() {
                }
            });

            mGroupListView.setAdapter(adapter);
            mGroupListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SubscribedGroupListAdapter.Item item = (SubscribedGroupListAdapter.Item) parent.getItemAtPosition(position);
                    Intent i = new Intent(Group.this, GroupDetailsActivity.class);
                    i.setData(Utils.getUriFor(Utils.UriType.GROUP, item.id));
                    startActivity(i);
                }
            });

            DatabaseReference root = FirebaseDatabase.getInstance().getReference();
            DatabaseReference userRef = root.child("users").child(user.getUid());
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot userSnap) {
                    Log.d(TAG, "setUser: " + user.getDisplayName() + "[" + user.getEmail() + "]");
                    String userName = userSnap.child("name").getValue(String.class);
                    mUserNameText.setText(userName);

                    //Save Informations about email e password Internally to the phone
                    SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("signin_complete_name", userName);
                    editor.commit();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    mUserNameText.setText(user.getEmail());
                }
            });
        }

        void onLogOut() {
            mGroupListView.setAdapter(null);
            mGroupListView.setOnItemClickListener(null);
            mUserNameText.setText("(not logged in)");

            /// TODO Button for logging in
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_list_groups, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            Intent i =new Intent(this, LoginActivity.class);
            startActivity(i);
        } else if (id == R.id.action_create_group) {
            Intent intent = new Intent(Group.this, CreationGroup.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
