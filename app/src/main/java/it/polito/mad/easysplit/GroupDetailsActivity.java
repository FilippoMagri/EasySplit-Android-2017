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
import it.polito.mad.easysplit.layout.OfflineWarningHelper;
import it.polito.mad.easysplit.layout.PaymentListFragment;
import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.Money;


public class GroupDetailsActivity extends AppCompatActivity {
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    private Uri mGroupUri;
    private OfflineWarningHelper mWarningHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Group.class));
            }
        });

        mGroupUri = getIntent().getData();

        DatabaseReference groupRefName = Utils.findByUri(mGroupUri, mRoot).child("name");
        groupRefName.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupNameSnap) {
                toolbar.setTitle(groupNameSnap.getValue(String.class));
            }

            @Override public void onCancelled(DatabaseError databaseError) {
                ActivityUtils.showDatabaseError(GroupDetailsActivity.this, databaseError);
            }
        });

        LocalPagerAdapter pagerAdapter = new LocalPagerAdapter(getSupportFragmentManager());
        ViewPager pager = (ViewPager) findViewById(R.id.group_details_pager);
        pager.setAdapter(pagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(pager);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mWarningHelper = new OfflineWarningHelper(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mWarningHelper.detach();
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
            if (i == 2)
                return PaymentListFragment.newInstance(mGroupUri);
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0)
                return getString(R.string.tab_name_expenses);
            else if (position == 1)
                return getString(R.string.tab_name_balance);
            else if (position == 2)
                return getString(R.string.tab_name_payments);
            return null;
        }

        @Override
        public int getCount() {
            return 3;
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

        if (id == R.id.action_edit) {
            Intent i = new Intent(getApplicationContext(), EditGroupActivity.class);
            i.setData(mGroupUri);
            startActivity(i);
            return true;
        } else if (id == R.id.action_invite) {
            Intent i = new Intent(getApplicationContext(), InvitePerson.class);
            String groupId = Utils.getIdFor(Utils.UriType.GROUP, mGroupUri);
            i.putExtra("groupId", groupId);
            startActivity(i);
            return true;
        } else if (id == R.id.action_leave) {
            mRoot.child("/.info/connected").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot connectedSnap) {
                    boolean connected = connectedSnap.getValue(Boolean.class);
                    if (! connected) {
                        new AlertDialog.Builder(GroupDetailsActivity.this)
                                .setTitle(R.string.error_generic_title)
                                .setMessage(R.string.error_cant_leave_group_Offline)
                                .show();
                        return;
                    }

                    new AlertDialog.Builder(GroupDetailsActivity.this)
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
                                        removeUserFromGroup(user.getUid(), balance.size() == 1);
                                        Intent i = new Intent(GroupDetailsActivity.this, Group.class);
                                        startActivity(i);
                                        finish();
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
                }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            });

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void removeUserFromGroup(String userId, boolean deleteGroup) {
        String userPath = Utils.getPathFor(Utils.UriType.USER, userId);
        String groupPath = Utils.getPathFor(mGroupUri);
        String groupId = Utils.getIdFor(Utils.UriType.GROUP, mGroupUri);

        HashMap<String, Object> updates = new HashMap<>();
        // remove group from user
        updates.put(userPath + "/groups_ids/" + groupId, null);

        if (deleteGroup)
            // remove group altogether
            updates.put(groupPath, null);
        else
            // remove user from group
            updates.put(groupPath + "/members_ids/" + userId, null);

        mRoot.updateChildren(updates);
    }

}
