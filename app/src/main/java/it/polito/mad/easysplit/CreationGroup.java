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

        setTitle(R.string.creation_group_title);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String groupName = groupNameEdit.getText().toString();
                createGroup(groupName);

            }
        });

    }

    private void createGroup(String groupName) {
        HashMap<String, String> groupMembers = new HashMap<>();
        HashMap<String, Object> childUpdates = new HashMap<>();

        String groupKey = mRoot.child("groups").push().getKey();

        SharedPreferences sharedPref = getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        String signin_complete_name = sharedPref.getString("signin_complete_name", null);

        // Finally, add current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        groupMembers.put(user.getUid(), signin_complete_name);
        childUpdates.put("/users/" + user.getUid() + "/groups_ids/" + groupKey, groupName);

        HashMap<String, Object> map = new HashMap<>();
        map.put("name", groupName);
        map.put("expenses", new HashMap<String, Boolean>());
        map.put("members_ids", groupMembers);

        childUpdates.put("/groups/" + groupKey, map);
        mRoot.updateChildren(childUpdates);

        finish();
    }
}