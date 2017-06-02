package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import it.polito.mad.easysplit.cloudMessaging.MessagingUtils;
import it.polito.mad.easysplit.email.GMailSender;

public class InvitePerson extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "InvitePersonActivity";
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    private final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();
    final DatabaseReference usersUriRef = mRoot.child("users");
    final DatabaseReference groupsUriRef = mRoot.child("groups");
    static String defaultTemporaryUserRegistrationPassword = "CA_FI_SE_FL_AN_MAD_2017";

    CoordinatorLayout mCoordinatorLayout;

    String emailToCheck;
    String groupName;
    String idGroup;
    String existingUserId;
    String userNameExistingUser;

    EditText mEmail;

    ValueEventListener emailValueEventListener;
    ValueEventListener userGroupIdsValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_person);

        Intent i = getIntent();
        idGroup = i.getStringExtra("groupId");
        root.child("groups").child(idGroup)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot groupSnap) {
                        groupName = groupSnap.child("name").getValue(String.class);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_invite_person);
        mEmail = (EditText) findViewById(R.id.email_invite_person);
        emailToCheck = mEmail.getText().toString();

        createEventListeners();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Group.class));
            }
        });

    }


    public void createEventListeners () {
        emailValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean exists = false;
                String idExistingUser = "";
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> model = (Map<String, Object>) child.getValue();
                    //Log.d(TAG,"Value model.get"+model.get("email").toString());
                    if(model.get("email")!=null) {
                        Log.d(TAG,"Value model.get"+model.get("email").toString());
                        if (model.get("email").equals(emailToCheck)) {
                            userNameExistingUser = model.get("name").toString();
                            idExistingUser = child.getKey();
                            exists = true;
                            break;
                        }
                    }
                }
                if(exists) {
                    // This user already exists in firebase.
                    Log.d(TAG,"User Exists into DB");
                    Log.d(TAG,"Il Suo ID è: "+idExistingUser);
                    existingUserId = idExistingUser;
                    userPresentIntoDB(idExistingUser);
                }
                else {
                    // This user doesn't exists in firebase.
                    Log.d(TAG,"User doesn't exists into DB");
                    userNotPresentIntoDb();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        userGroupIdsValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean userMemberOfThisGroup=false;
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    String id_key =child.getKey();
                    if (id_key.equals(idGroup)) {
                        userMemberOfThisGroup =true;
                        Log.d(TAG,"User is Member of this Group");
                        Snackbar.make(mCoordinatorLayout,"User is Already Member of this Group",Snackbar.LENGTH_SHORT).show();
                        break;
                    }
                    Log.d(TAG,"Key Of the group of this users: "+id_key);
                }
                if(!userMemberOfThisGroup) {
                    Log.d(TAG,"User is NOT Member of this Group");
                    HashMap<String,Object> childUpdates = new HashMap<>();
                    childUpdates.put("/groups/" + idGroup + "/members_ids/" + existingUserId, userNameExistingUser); // add user to group
                    childUpdates.put("/users/" + existingUserId + "/groups_ids/" + idGroup, groupName); // add group to user
                    database.getReference().updateChildren(childUpdates);

                    // Retrieve fields for the notification to the new Member
                    Map<String, String> newMembersToNotify = new HashMap<String, String>();
                    newMembersToNotify.put(existingUserId,userNameExistingUser);
                    String title4Notification = getResources().getString(R.string.join_group);
                    String message4Notification = getResources().getString(R.string.new_group_created);
                    MessagingUtils.sendPushUpNotifications(mRoot, idGroup, title4Notification, newMembersToNotify, message4Notification);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
    }

    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.invite_person_button) {
            emailToCheck = mEmail.getText().toString();
            Log.d(TAG,"MyRef: "+usersUriRef.toString());
            Log.d(TAG,"OnClick: Add EventListener");
            usersUriRef.addListenerForSingleValueEvent(emailValueEventListener);
        }

    }

    private void userPresentIntoDB(String idExistingUser) {
        DatabaseReference groups_ids_reference = usersUriRef.child(idExistingUser).child("groups_ids");
        groups_ids_reference.addListenerForSingleValueEvent(userGroupIdsValueEventListener); // updated so as not to constantly update information!
    }

    private void userNotPresentIntoDb() {
        //Send the Invitation email
        sendInvitationMail();

        //Register user with temporaryPassword inside Authentication Structure of Firebase
        attemptRegister();

        Snackbar.make(mCoordinatorLayout,"User has been added temporary to the group.\n" +
                                         "We've sent an email for the registration to become\n" +
                                         "an interactive user",4000).show();

    }

    private void sendInvitationMail() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    GMailSender sender = new GMailSender(
                            "easysplitmad2017@gmail.com",
                            "qwertyuiop12345");

                    String htmlBody = "<h1 style=\"color: #5e9ca0; text-align: left;\">Welcome To EasySplit&nbsp;!</h1><h2 style=\"color: #2e6c80;\"><img src=\"https://firebasestorage.googleapis.com/v0/b/progetto-prova-firebase.appspot.com/o/email_attachment.jpg?alt=media&amp;token=bc14c2e6-937a-4e14-b08d-2b3ffff21cb3\" alt=\"\" width=\"416\" height=\"282\" /></h2><h2 style=\"color: #2e6c80;\">Somebody Invited you to the Group: "+groupName+".&nbsp;<br />If you want to share costs , just register by <br />clicking on Subscribe.</h2><p><a href=\"https://firebasestorage.googleapis.com/v0/b/easysplit-853e4.appspot.com/o/last_version_app-debug.apk?alt=media&token=dfbb1e2b-4f8d-42ab-a267-9240daf5a105\"><img style=\"float: left;\" src=\"https://firebasestorage.googleapis.com/v0/b/progetto-prova-firebase.appspot.com/o/Subscribe-PNG-7.png?alt=media&amp;token=c8b66185-ca0d-4578-9fe7-d8d62c4e8434\" alt=\"interactive connection\" width=\"392\" height=\"122\" /></a></p><p><strong>&nbsp;</strong></p><p>&nbsp;</p>";
                    sender.sendMail("["+groupName+"] [This User Wanna Share Expenses With You]", htmlBody ,
                            "easysplitmad2017@gmail.com",emailToCheck);
                } catch (Exception e) {
                    Snackbar.make(mCoordinatorLayout,"Error",Snackbar.LENGTH_SHORT).show();
                }

            }

        }).start();


    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG,"OnStop: Remove EventListener");
        usersUriRef.removeEventListener(emailValueEventListener);
    }

    /**
     * Attempts to create the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister() {
        // Store values at the time of the register attempt.
        String email = emailToCheck;
        String password = defaultTemporaryUserRegistrationPassword;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(new InvitePerson.RegisterTaskListener());
    }

    private class RegisterTaskListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {

            if (task.isSuccessful()) {
                /* get newly created user */
                FirebaseUser user = task.getResult().getUser();
                HashMap<String, Object> childUpdates = new HashMap<>();
                HashMap<String, Object> newUser = new HashMap<>();
                String userId = user.getUid();

                /* create new user in database */
                newUser.put("email", emailToCheck);
                newUser.put("name", emailToCheck); // use email as the name (until specified by the user upon registration)
                HashMap<String, String> groupsMap = new HashMap<>();
                groupsMap.put(idGroup, groupName);
                newUser.put("groups_ids", groupsMap); // add group to user
                childUpdates.put("/users/" + userId, newUser); // add new user to database
                childUpdates.put("/groups/" + idGroup + "/members_ids/" + userId, emailToCheck); // add user to group
                /* here, if we really wanted to be consistent,
                 * one may want to figure out how to delete the user
                 * from the authentication part, if things go awry
                 * with the creation of the new account in the database
                  */
                database.getReference().updateChildren(childUpdates);

                //The user has been added with temporaryPassword inside Authentication Structure of Firebase
                //Do Nothing Because The Verification-Mail Will be sent only after the real Registration
                //Performed by the user

                SharedPreferences sharedPref = getSharedPreferences("MyPreferences",Context.MODE_PRIVATE);
                String signin_email = sharedPref.getString("signin_email",null);
                String signin_password = sharedPref.getString("signin_password",null);
                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.signInWithEmailAndPassword(signin_email,signin_password);
            } else {
                Exception exc = task.getException();
                AlertDialog dialog = new AlertDialog.Builder(InvitePerson.this)
                        .setTitle("Couldn't Register!")
                        .setMessage(exc.getLocalizedMessage())
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.show();
            }
        }
    }

}
