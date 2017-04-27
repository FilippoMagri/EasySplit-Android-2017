package it.polito.mad.easysplit;

import android.content.Intent;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

public class InvitePerson extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "InvitePersonActivity";
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    final DatabaseReference usersUriRef = database.getReference().child("users");
    final DatabaseReference groupsUriRef = database.getReference().child("groups");
    final String defaultTemporaryUserRegistrationPassword = "CA_FI_SE_FL_AN_MAD_2017";

    CoordinatorLayout mCoordinatorLayout;

    String emailToCheck;
    String groupName;
    String idGroup;

    EditText mEmail;

    ValueEventListener emailValueEventListener;
    ValueEventListener groupNameValueEventListener;
    ValueEventListener userGroupIdsValueEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_person);

        Intent i = getIntent();
        groupName = i.getStringExtra("Group Name");
        setTitle(groupName);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout_invite_person);
        mEmail = (EditText) findViewById(R.id.email_invite_person);
        emailToCheck = mEmail.getText().toString();

        createEventListeners();
        retrieveGroupIdByName();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    private void retrieveGroupIdByName() {
        Log.d(TAG,"Let's find the Group Id of this Group");
        Log.d(TAG,"MyRef: "+groupsUriRef.toString());
        groupsUriRef.addValueEventListener(groupNameValueEventListener);
    }

    public void createEventListeners () {
        emailValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean exists = false;
                String idExistingUser = "";
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> model = (Map<String, Object>) child.getValue();
                    if(model.get("email").equals(emailToCheck)) {
                        idExistingUser = child.getKey();
                        exists = true;
                        break;
                    }
                }
                if(exists) {
                    // This user already exists in firebase.
                    Log.d(TAG,"User Exists into DB");
                    Log.d(TAG,"Il Suo ID è: "+idExistingUser);
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

        groupNameValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    Map<String, Object> model = (Map<String, Object>) child.getValue();
                    if(model.get("name").equals(groupName)) {
                        idGroup = child.getKey();
                        Log.d(TAG,"IdGroup: "+idGroup);
                        break;
                    }
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
                    // TODO Add the User by mail to the Database (with a systemPassword that will be changed later on during user registration ,by himself) @Flavio Giobergia
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
            usersUriRef.addValueEventListener(emailValueEventListener);
        }

    }

    private void userPresentIntoDB(String idExistingUser) {
        DatabaseReference groups_ids_reference = usersUriRef.child(idExistingUser).child("groups_ids");
        groups_ids_reference.addValueEventListener(userGroupIdsValueEventListener);
    }

    private void userNotPresentIntoDb() {
        //Send the Invitation email

        //Register user with temporaryPassword inside Authentication Structure of Firebase
        attemptRegister();

        // TODO Add the User by mail to the Database (with a systemPassword that will be changed later on during user registration ,by himself) @Flavio Giobergia

        Snackbar.make(mCoordinatorLayout,"User has been added temporary to the group.\n" +
                                         "We've sent an email for the registration to become\n" +
                                         "an interactive user",4000).show();

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
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new InvitePerson.RegisterTaskListener());
    }

    private class RegisterTaskListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {

            if (task.isSuccessful()) {
                //The user has been added with temporaryPassword inside Authentication Structure of Firebase
                //Do Nothing Because The Verification-Mail Will be sent only after the real Registration
                //Performed by the user
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
