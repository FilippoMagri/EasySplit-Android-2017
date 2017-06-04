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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import it.polito.mad.easysplit.models.GroupBalanceModel;
import it.polito.mad.easysplit.models.Money;


public class Group extends AppCompatActivity {
    private static String TAG = Group.class.getName();
    private ListView mGroupListView;
    private TextView mUserNameText;
    private TextView mNoGroupsText;
    private AuthListener mAuthListener;
    private LinearLayout mLinearLayout;

    // counterMBalances is used internally when the user ask to perform the merge of all groups balance
    final ConcurrentLinkedQueue<Float> counterMBalances = new ConcurrentLinkedQueue<>();

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
                    final long nBelongedGroups4User = groupsSnapshot.getChildrenCount();
                    initializeProgressBar();
                    for (DataSnapshot group : groupsSnapshot.getChildren()) {
                        final String groupKey = group.getKey();
                        DatabaseReference groupRef = root.child("groups").child(groupKey);
                        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot groupSnapshot) {
                                final Uri mGroupUri = Utils.getUriFor(Utils.UriType.GROUP, groupKey);
                                Currency localCurrency = ConversionRateProvider.getLocaleCurrency();
                                GroupBalanceModel groupBalanceModel = GroupBalanceModel.forGroup(mGroupUri,localCurrency.getCurrencyCode());
                                MyGroupBalanceListener myGroupBalanceListener = new MyGroupBalanceListener(nBelongedGroups4User);
                                mListeners.put(groupBalanceModel,myGroupBalanceListener);
                                groupBalanceModel.addListener(myGroupBalanceListener);
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
                            counterMBalances.clear();
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

    /** Do all of the necessary conversions to the Local currency */
    private Task<Void> convertToLocalCurrency(final GroupBalanceModel groupBalanceModel) {
        final ConversionRateProvider converter = ConversionRateProvider.getInstance();
        final TaskCompletionSource completion = new TaskCompletionSource();

        new Thread() {
            @Override
            public void run() {
                synchronized (groupBalanceModel) {
                    try {
                        GroupBalanceModel.MemberRepresentation userMemberRepresentation = groupBalanceModel.getmBalance().get(FirebaseAuth.getInstance().getCurrentUser().getUid());
                        Money userMemberResidue = userMemberRepresentation.getResidue();
                        userMemberRepresentation.setConvertedResidue(Tasks.await(converter.convertFromBase(userMemberResidue, ConversionRateProvider.getLocaleCurrency())));
                        completion.setResult(null);
                    } catch (InterruptedException | ExecutionException exc) {
                        completion.setException(exc);
                    }
                }
            }
        }.start();

        return completion.getTask();
    }

   public  class MyGroupBalanceListener implements GroupBalanceModel.Listener {
       FirebaseUser user;
       long nBelongedGroups4User;
       public MyGroupBalanceListener(long nBelongedGroups4User) {
           user = FirebaseAuth.getInstance().getCurrentUser();
           this.nBelongedGroups4User = nBelongedGroups4User;
       }
       @Override
        public void onBalanceChanged(Map<String, GroupBalanceModel.MemberRepresentation> balance) {
           final GroupBalanceModel.MemberRepresentation memberInGroupModel= balance.get(user.getUid());
           // We check the size because we wanna avoid to enter in this piece of code when the
           // balance has just initialized
           if (balance.size() > 0) {
               counterMBalances.add(new Float(memberInGroupModel.getConvertedResidue().getAmount().toString()));
               // We check the size of "counterMBalances" that is a ConcurrentLinkedQueue ,
               // because we wanna be sure that all balances of all groups has been computed
               // and of course also converted in the relative currency we are interested in
               if (counterMBalances.size() == nBelongedGroups4User) {
                   Float totalAmountPositive = new Float("0.00");
                   Float totalAmountNegative = new Float("0.00");
                   Float totalAmount = new Float("0.00");
                   Iterator<Float> iterator= counterMBalances.iterator();
                   while (iterator.hasNext()) {
                       Float singleBalance = iterator.next();
                       if (singleBalance.compareTo(new Float("0.00")) > 0) {
                           totalAmountPositive = totalAmountPositive + singleBalance;
                       } else if (singleBalance.compareTo(new Float("0.00")) < 0) {
                           totalAmountNegative = totalAmountNegative + singleBalance;
                       }
                   }

                   totalAmount = totalAmountPositive + totalAmountNegative;
                   TextView tvAmountToOWn = (TextView) findViewById(R.id.amountToOwn);
                   TextView tvAmountToReceive = (TextView) findViewById(R.id.amountToReceive);
                   TextView tvAmountTotal = (TextView) findViewById(R.id.amountTotal);

                   String symbolConvertedCurrency = memberInGroupModel.getConvertedResidue().getCurrency().getSymbol();
                   tvAmountToOWn.setText(totalAmountNegative.toString() + " "+symbolConvertedCurrency);
                   tvAmountToReceive.setText(totalAmountPositive.toString() + " "+symbolConvertedCurrency);
                   tvAmountTotal.setText(totalAmount.toString() + " "+symbolConvertedCurrency);

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
        }
    }
}
