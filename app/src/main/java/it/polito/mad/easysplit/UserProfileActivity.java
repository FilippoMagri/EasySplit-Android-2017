package it.polito.mad.easysplit;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UserProfileActivity extends AppCompatActivity {
    private DatabaseReference mUser;

    private EditText mNameEdit;
    private TextView mEmailText;
    private ImageButton mImageButton;
    private ProgressBar mProfilePicProgressBar;

    private ProfilePictureManager mPicManager;
    private ProfilePictureManager.Listener mPicListener;

    private void setInProgress(boolean inProgress) {
        setProgressBarIndeterminateVisibility(inProgress);
        mImageButton.setVisibility(inProgress ? View.INVISIBLE : View.VISIBLE);
        mProfilePicProgressBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_user_profile);

        mNameEdit = (EditText) findViewById(R.id.nameEdit);
        mEmailText = (TextView) findViewById(R.id.emailText);
        mImageButton = (ImageButton) findViewById(R.id.imageButton);
        mProfilePicProgressBar = (ProgressBar) findViewById(R.id.profilePicProgressBar);

        setProgressBarIndeterminateVisibility(true);
        mImageButton.setVisibility(View.INVISIBLE);
        mProfilePicProgressBar.setVisibility(View.VISIBLE);

        Uri userUri = getIntent().getData();
        String userId = Utils.getIdFor(Utils.UriType.USER, userUri);

        mUser = Utils.findByUri(userUri);
        mUser.child("name").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot nameSnap) {
                mNameEdit.setText(nameSnap.getValue(String.class));
                setProgressBarIndeterminateVisibility(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showDatabaseError(databaseError);
            }
        });

        mUser.child("email").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot emailSnap) {
                mEmailText.setText(emailSnap.getValue(String.class));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                showDatabaseError(databaseError);
            }
        });


        mPicListener = new ProfilePictureManager.Listener() {
            @Override
            public void onThumbnailReceived(Bitmap pic) {

            }

            @Override
            public void onPictureReceived(Bitmap pic) {
                mProfilePicProgressBar.setVisibility(View.INVISIBLE);
                mImageButton.setVisibility(View.VISIBLE);
                if (pic != null)
                    mImageButton.setImageBitmap(pic);
                else
                    mImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_default_profile_pic));
            }

            @Override
            public void onFailure(Exception e) {
                mProfilePicProgressBar.setVisibility(View.INVISIBLE);
                mImageButton.setVisibility(View.VISIBLE);
                mImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_default_profile_pic));
            }
        };

        mPicManager = ProfilePictureManager.forUser(this, userId);
        mPicManager.addListener(mPicListener);

        mImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeProfilePic();
            }
        });
    }

    void showDatabaseError(DatabaseError databaseError) {
        setInProgress(false);
        new AlertDialog.Builder(UserProfileActivity.this)
                .setCancelable(false)
                .setTitle("We're sorry!")
                .setMessage("There was a problem: " + databaseError.getMessage())
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        UserProfileActivity.this.finish();
                    }
                })
                .show();
    }


    @Override
    protected void onPause() {
        super.onPause();
        updateName();
    }

    private void updateName() {
        String newName = mNameEdit.getText().toString();
        mUser.child("name").setValue(newName);
    }

    private static final int RESULT_PROFILE_PIC = 1;

    public void changeProfilePic() {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        i.putExtra("crop", "true");
        i.putExtra("return-data", true);
        i.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(i, RESULT_PROFILE_PIC);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null || resultCode != RESULT_OK)
            return;

        if (requestCode == RESULT_PROFILE_PIC) {
            Bundle extras = data.getExtras();
            Bitmap selectedBitmap = extras.getParcelable("data");
            mPicManager.setPicture(selectedBitmap);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
