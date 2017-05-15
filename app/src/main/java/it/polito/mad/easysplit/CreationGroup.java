package it.polito.mad.easysplit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class CreationGroup extends AppCompatActivity {
    private static final DatabaseReference mRoot = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_creation_group);

        final EditText groupNameEdit = (EditText) findViewById(R.id.nameGroup);
        final EditText participantsListEdit = (EditText) findViewById(R.id.newGroupParticipantsList);
        ImageView submit = (ImageView) findViewById(R.id.valid);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationIcon(R.drawable.ic_home_white_48dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Group.class));
            }
        });

        setTitle("Group Creation");
        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            private int getColorFromString(String str) {
                return isValidEmail(str) ? Color.GREEN : Color.RED;
            }

            @Override
            public void afterTextChanged(Editable s) {
                Character[] spacingChars = {' ', '\t', '\n'};

                String str = s.toString();
                SpannableString text = new SpannableString(str);
                int position = participantsListEdit.getSelectionStart();
                int start;
                for (start = 0; start < s.length(); ) {
                    int end = start;
                    while (end < str.length() && !Arrays.asList(spacingChars).contains(str.charAt(end))) {
                        end++;
                    }
                    text.setSpan(new BackgroundColorSpan(getColorFromString(str.substring(start, end))), start, end, 0);
                    start = end + 1;
                    while (start < str.length() && Arrays.asList(spacingChars).contains(str.charAt(start))) {
                        start++;
                    }
                }

                /* set new text (with highlights) */
                participantsListEdit.removeTextChangedListener(this);
                participantsListEdit.setText(text);
                participantsListEdit.addTextChangedListener(this);
                participantsListEdit.setSelection(position); // update to previous position */
            }
        };
        participantsListEdit.addTextChangedListener(tw);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatabaseReference root = FirebaseDatabase.getInstance().getReference();
                root.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(final DataSnapshot usersSnap) {
                        String participantsStr = participantsListEdit.getText().toString();
                        final List<String> participants = Arrays.asList(participantsStr.split("\\s+"));
                        final String groupName = groupNameEdit.getText().toString();

                        Tasks.call(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                createGroup(groupName, participants, usersSnap);
                                return null;
                            }
                        });
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

            }
        });

    }

    private boolean isValidEmail(String str) {
        // TODO Add checks on email validity here!
        return str.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-z]+$");
    }

    private void createGroup(String groupName, List<String> emails, DataSnapshot usersSnap) {
        ArrayList<String> externalEmails = new ArrayList<>(emails);
        HashMap<String, String> groupMembers = new HashMap<>();
        HashMap<String, Object> childUpdates = new HashMap<>();

        String groupKey = mRoot.child("groups").push().getKey();

        // create an internal mapping email -> uid
        for (DataSnapshot user : usersSnap.getChildren()) {
            if (!user.hasChild("email"))
                continue;

            String emailAddr = user.child("email").getValue(String.class);
            if (!emails.contains(emailAddr))
                continue;

            // Okay, email required by the user! include it
            // (UID used as key, not the email!)
            String userName = user.child("name").getValue().toString();
            groupMembers.put(user.getKey(), userName);
            childUpdates.put("/users/" + user.getKey() + "/groups_ids/" + groupKey, groupName);
            externalEmails.remove(emailAddr); // user found, no need to register it
        }

        // proceed with the registration of the new email accounts!
        for (String email : externalEmails) {
            if (isValidEmail(email))
                continue;

            FirebaseAuth auth = FirebaseAuth.getInstance();
            Task<AuthResult> task = auth.createUserWithEmailAndPassword(email, InvitePerson.defaultTemporaryUserRegistrationPassword);
            try {
                Tasks.await(task);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

            FirebaseUser user = task.getResult().getUser();
            String userId = user.getUid();

            HashMap<String, String> groupsMap = new HashMap<>();
            groupsMap.put(groupKey, groupName); // add group to the user

            HashMap<String, Object> newUser = new HashMap<>();
            newUser.put("email", email);
            newUser.put("name", email); // use email as the name (until specified by the user upon registration)
            newUser.put("groups_ids", groupsMap);

            childUpdates.put("/users/" + userId, newUser); // add new user to database
            groupMembers.put(userId, email); // add user to the group
            // TODO Because in this case Flavio has expected the functionality of invite a new user by just the email, we'll have to introduce also here the invitation-mail. (Later on)
        }

        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String signin_email = sharedPref.getString("signin_email", null);
        String signin_password = sharedPref.getString("signin_password", null);
        String signin_complete_name = sharedPref.getString("signin_complete_name", null);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        try {
            Tasks.await(auth.signInWithEmailAndPassword(signin_email, signin_password));
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        // Finally, add current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        groupMembers.put(user.getUid(), signin_complete_name);
        childUpdates.put("/users/" + user.getUid() + "/groups_ids/" + groupKey, groupName);

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", groupName);
        map.put("expenses_ids", new HashMap<String, Boolean>());
        map.put("members_ids", groupMembers);

        childUpdates.put("/groups/" + groupKey, map);
        mRoot.updateChildren(childUpdates);

        finish();
    }
}