package it.polito.mad.easysplit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import it.polito.mad.easysplit.models.GroupBalanceModel;


public class Group extends AppCompatActivity {
    private static String TAG = Group.class.getName();
    private ListView mGroupListView;
    private TextView mUserNameText;
    private TextView mNoGroupsText;
    private AuthListener mAuthListener;
    private LinearLayout mLinearLayout;

    // counterMBalances is used internally when the user ask to perform the merge of all groups balance
    final ArrayList<Float> counterMBalances = new ArrayList<Float>();

    // This Concurrent Map represent for each GroupBalanceModel its Listener.
    // It will be populated with exactly one listener for each single GroupBalanceModel.
    private final ConcurrentHashMap<GroupBalanceModel,GroupBalanceModel.Listener> mListeners = new ConcurrentHashMap<>();

    // ProgressDialog management
    private ProgressDialog progressBar;
    private int progressBarStatus = 0;
    private Handler progressBarbHandler = new Handler();

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
        mLinearLayout = (LinearLayout) findViewById(R.id.ll_fused_global_balance);

        mAuthListener = new AuthListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().addAuthStateListener(mAuthListener);
        ActivityUtils.requestLogin(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (counterMBalances.size()>0) {
            dismissProgressBar();
            detatchListeners();
            counterMBalances.clear();
            mLinearLayout.clearAnimation();
            mLinearLayout.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mLinearLayout.clearAnimation();
                    mLinearLayout.setVisibility(View.GONE);
                }
            });
        }
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
        } else if (id == R.id.action_show_fused_total_balance) {
            mergeAllBalances();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void mergeAllBalances() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference groupsUserRef = root.child("users").child(user.getUid()).child("groups_ids");
        groupsUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot groupsSnapshot) {
                DatabaseReference root = FirebaseDatabase.getInstance().getReference();
                ArrayList<String> groupsKeys = new ArrayList<String>();
                if (mLinearLayout.getVisibility() == View.GONE) {
                    final long numberOfGroupsOwnership = groupsSnapshot.getChildrenCount();
                    initializeProgressBar();
                    for (DataSnapshot group : groupsSnapshot.getChildren()) {
                        final String groupKey = group.getKey();
                        DatabaseReference groupRef = root.child("groups").child(groupKey);
                        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot groupSnapshot) {
                                final Uri mGroupUri = Utils.getUriFor(Utils.UriType.GROUP, groupKey);
                                GroupBalanceModel groupBalanceModel = new GroupBalanceModel(mGroupUri);
                                GroupBalanceModel.Listener listener = new GroupBalanceModel.Listener() {
                                    @Override
                                    public void onBalanceChanged(Map<String, GroupBalanceModel.MemberRepresentation> balance) {
                                        if (balance.size() > 0) {
                                            String amountString = balance.get(user.getUid()).getResidue().getAmount().toString();
                                            counterMBalances.add(new Float(amountString));
                                        }
                                        if (counterMBalances.size() == numberOfGroupsOwnership) {
                                            Float totalAmountPositive = new Float("0.00");
                                            Float totalAmountNegative = new Float("0.00");
                                            Float totalAmount = new Float("0.00");
                                            for (int i = 0; i < counterMBalances.size(); i++) {
                                                Float singleBalance = counterMBalances.get(i);
                                                if (singleBalance.compareTo(new Float("0.00")) > 0) {
                                                    totalAmountPositive = totalAmountPositive + counterMBalances.get(i);
                                                } else if (singleBalance.compareTo(new Float("0.00")) < 0) {
                                                    totalAmountNegative = totalAmountNegative + counterMBalances.get(i);
                                                }
                                            }
                                            totalAmount = totalAmountPositive + totalAmountNegative;
                                            TextView tvAmountToOWn = (TextView) findViewById(R.id.amountToOwn);
                                            TextView tvAmountToReceive = (TextView) findViewById(R.id.amountToReceive);
                                            TextView tvAmountTotal = (TextView) findViewById(R.id.amountTotal);
                                            tvAmountToOWn.setText(totalAmountNegative.toString() + " €");
                                            tvAmountToReceive.setText(totalAmountPositive.toString() + " €");
                                            tvAmountTotal.setText(totalAmount.toString() + " €");

                                            mLinearLayout.animate().alpha(1.0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                                                @Override
                                                public void onAnimationStart(Animator animation) {
                                                    super.onAnimationStart(animation);
                                                    mLinearLayout.setVisibility(View.VISIBLE);
                                                }
                                            });
                                            detatchListeners();
                                            dismissProgressBar();
                                        }
                                    }
                                };
                                mListeners.put(groupBalanceModel,listener);
                                groupBalanceModel.addListener(listener);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                } else {
                    mLinearLayout.animate().alpha(0.0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mLinearLayout.clearAnimation();
                            mLinearLayout.setVisibility(View.GONE);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void detatchListeners() {
        for (Map.Entry<GroupBalanceModel,GroupBalanceModel.Listener> entry : mListeners.entrySet()) {
            GroupBalanceModel groupBalanceModel = entry.getKey();
            GroupBalanceModel.Listener listener = entry.getValue();
            groupBalanceModel.removeListener(listener);
        }
    }

    private void initializeProgressBar() {
        progressBar = new ProgressDialog(mLinearLayout.getContext());
        progressBar.setCancelable(true);
        progressBar.setMessage(getString(R.string.message_progress_bar_during_fusing)+" ...");
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressBar.setProgress(0);
        progressBar.setMax(100);
        progressBar.show();
        progressBarStatus = 0;
    }

    private void dismissProgressBar() {
        progressBarbHandler.post(new Runnable() {
            public void run() {
                progressBar.setProgress(0);
                progressBar.setMax(0);
                progressBarStatus = 0;
                progressBar.hide();
                progressBar.dismiss();
            }
        });
    }
}
