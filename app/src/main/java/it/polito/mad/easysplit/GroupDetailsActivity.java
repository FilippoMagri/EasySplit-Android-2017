package it.polito.mad.easysplit;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.layout.ExpenseListFragment;
import it.polito.mad.easysplit.layout.MemberListFragment;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.Money;


public class GroupDetailsActivity extends AppCompatActivity {
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    private Uri mGroupUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Group.class));
            }
        });

        mGroupUri = getIntent().getData();

        DatabaseReference groupRefName = Utils.findByUri(mGroupUri, mRoot).child("name");
        groupRefName.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupNameSnap) {
                setTitle(groupNameSnap.getValue(String.class));
            }

            @Override public void onCancelled(DatabaseError databaseError) { }
        });

        LocalPagerAdapter pagerAdapter = new LocalPagerAdapter(getSupportFragmentManager());
        ViewPager pager = (ViewPager) findViewById(R.id.group_details_pager);
        pager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
    }

    private class LocalPagerAdapter extends FragmentPagerAdapter {
        private LocalPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == 0)
                return ExpenseListFragment.newInstance(mGroupUri);
            if (i == 1)
                return MemberListFragment.newInstance(mGroupUri);
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_name_expenses);
            else if (position == 1)
                return getString(R.string.tab_name_balance);
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_group_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_invite) {
            Intent i = new Intent(getApplicationContext(), InvitePerson.class);
            i.putExtra("Group Name", getTitle());
            startActivity(i);
            return true;
        } else if (id == R.id.action_leave) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.leave_confirm_title)
                    .setMessage(R.string.leave_confirm_message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Map<String, GroupBalanceModel.MemberRepresentation> balance =
                                    GroupBalanceModel.forGroup(mGroupUri).getBalanceSnapshot();

                            final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            Money residue = balance.get(user.getUid()).getResidue();

                            // delete user from group, and group from user
                            if (residue.isZero()) {
                                removeUserFromGroup(user.getUid());
                                Intent i = new Intent(GroupDetailsActivity.this, Group.class);
                                startActivity(i);
                            } else {
                                new AlertDialog.Builder(GroupDetailsActivity.this)
                                        .setTitle(R.string.balance_problem_title)
                                        .setMessage(R.string.balance_problem_message)
                                        .setPositiveButton(R.string.okay, null).show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void removeUserFromGroup(String userId) {
        String userPath = Utils.getPathFor(Utils.UriType.USER, userId);
        String groupPath = Utils.getPathFor(mGroupUri);
        String groupId = Utils.getIdFor(Utils.UriType.GROUP, mGroupUri);

        HashMap<String, Object> updates = new HashMap<>();
        // remove group from user
        updates.put(userPath + "/groups_ids/" + groupId, null);
        // remove user from group
        updates.put(groupPath + "/members_ids/" + userId, null);

        mRoot.updateChildren(updates);
    }

}
