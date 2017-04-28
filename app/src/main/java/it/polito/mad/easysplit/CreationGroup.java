package it.polito.mad.easysplit;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.AsyncListUtil;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CreationGroup extends AppCompatActivity {
    final CreationGroup self = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_creation_group);

        final EditText groupName = (EditText) findViewById(R.id.nameGroup);
        final EditText participantsList = (EditText) findViewById(R.id.newGroupParticipantsList);
        FloatingActionButton submit = (FloatingActionButton) findViewById(R.id.valid);

        TextWatcher tw = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            private int getColorFromString (String str) {
                if (str.matches("^[\\w\\.]+@[\\w\\.]+\\.[a-z]+$")) {
                    return Color.GREEN;
                }
                else {
                    return Color.RED;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                Character [] spacingChars = { ' ', '\t', '\n' };

                String str = s.toString();
                SpannableString text = new SpannableString(str);
                int position = participantsList.getSelectionStart();
                int start;
                for (start = 0; start < s.length(); ) {
                    int end = start;
                    while (end < str.length() && !Arrays.asList(spacingChars).contains(str.charAt(end))) {
                        end ++;
                    }
                    text.setSpan(new BackgroundColorSpan(getColorFromString(str.substring(start,end))), start, end, 0);
                    start = end + 1;
                    while (start < str.length() && Arrays.asList(spacingChars).contains(str.charAt(start))) {
                        start++;
                    }
                }

                /* set new text (with highlights) */
                participantsList.removeTextChangedListener(this);
                participantsList.setText(text);
                participantsList.addTextChangedListener(this);
                participantsList.setSelection(position); // update to previous position */
            }
        };
        participantsList.addTextChangedListener(tw);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
                //DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");
                ref.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        class CreateGroup extends AsyncTask<Object, Void, Void> {

                            @Override
                            protected Void doInBackground(Object... params) {
                                String name = (String)params[0]; //groupName.getText().toString();
                                // participantsList.getText().toString() -> params[1]

                                ArrayList<String> emails = new ArrayList<>(Arrays.asList(
                                        ((String)params[1]).split("\\s+")
                                ));
                                ArrayList<String> externalEmails = new ArrayList<>(emails);

                                HashMap<String, Boolean> groupMembers = new HashMap<>();
                                HashMap<String, Object> childUpdates = new HashMap<>();
                                String groupKey = ref.child("groups").push().getKey();

                        /* create an internal mapping email -> uid */
                                DataSnapshot dataSnapshot = (DataSnapshot)params[2];
                                if (dataSnapshot.hasChildren()) {
                                    Iterable<DataSnapshot> iter = dataSnapshot.getChildren();
                                    for (DataSnapshot user : iter) {
                                        if (user.hasChild("email")) {
                                            // okay, email found
                                            String emailAddr = (String)user.child("email").getValue();
                                            if (emails.contains(emailAddr)) {
                                                // Okay, email required by the user! include it
                                                // (UID used as key, not the email!)
                                                groupMembers.put(user.getKey(), true);
                                                childUpdates.put("/users/" + user.getKey() + "/groups_ids/" + groupKey, true);
                                                externalEmails.remove(emailAddr); // user found, no need to register it
                                            }
                                        }
                                    }
                                }

                                /* proceed with the registration of the new email accounts! */
                                for (String email : externalEmails) {
                                    if (email.length() > 0) { // add here checks on email validity!

                                        FirebaseAuth auth = FirebaseAuth.getInstance();
                                        Task<AuthResult> task = auth.createUserWithEmailAndPassword(email,InvitePerson.defaultTemporaryUserRegistrationPassword);
                                        try {
                                            Tasks.await(task);
                                            FirebaseUser user = task.getResult().getUser();
                                            String userId = user.getUid();
                                            HashMap < String, Object > newUser = new HashMap<>();

                                            newUser.put("email", email);
                                            newUser.put("name", email); // use email as the name (until specified by the user upon registration)
                                            HashMap<String, Boolean> groupsMap = new HashMap<>();
                                            groupsMap.put(groupKey, true); // add group to the user
                                            newUser.put("groups_ids", groupsMap);
                                            childUpdates.put("/users/" + userId, newUser); // add new user to database
                                            groupMembers.put(userId, true); // add user to the group
                                        } catch (ExecutionException e) {
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
                                String signin_email = sharedPref.getString("signin_email",null);
                                String signin_password = sharedPref.getString("signin_password",null);
                                FirebaseAuth auth = FirebaseAuth.getInstance();
                                try {
                                    Tasks.await(auth.signInWithEmailAndPassword(signin_email,signin_password));
                                } catch (ExecutionException e) {
                                    e.printStackTrace();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                /* Finally, add current user */
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                groupMembers.put(user.getUid(), true);
                                childUpdates.put("/users/" + user.getUid() + "/groups_ids/" + groupKey, true);

                                HashMap<String, Object> map = new HashMap<>();
                                map.put("name", name);
                                map.put("expenses_ids", new HashMap<String,Boolean>());
                                map.put("members_ids", groupMembers);

                                //ref.child("groups").push().setValue(map);

                                childUpdates.put("/groups/" + groupKey, map);
                                ref.updateChildren(childUpdates);


                                self.finish();


                                return null;
                            }
                        }
                        new CreateGroup().execute(groupName.getText().toString(), participantsList.getText().toString(), dataSnapshot);

/*
{
    /groups/-KikQN2Ie4WXJJG5p8hH=
        {name=Sdf, expenses_ids={}, members_ids=
                {m6eA2FsFCQgoIGa49l0YHUkFliK2=true, -KikQN2Ie4WXJJG5p8hI=true}
        },
    /users/m6eA2FsFCQgoIGa49l0YHUkFliK2/groups_ids/-KikQN2Ie4WXJJG5p8hH=true,
    /users/-KikQN2Ie4WXJJG5p8hI=
        {groups_ids={-KikQN2Ie4WXJJG5p8hH=true}, name=, email=}}

*/
                    }



                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }
        });

    }

}