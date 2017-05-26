package it.polito.mad.easysplit;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.easysplit.cloudMessaging.MyFirebaseInstanceIdService;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_PHONE_STATE;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_READ_PHONE_STATE = 1;
    private static boolean READ_PHONE_STATE_GUARANTEED =false;

    /// TODO Allow to cancel the login currently in progress

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private LinearLayout mLinearLayout;
    final String defaultTemporaryUserRegistrationPassword = "CA_FI_SE_FL_AN_MAD_2017";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //retrieve LinearLayout
        mLinearLayout = (LinearLayout) findViewById(R.id.login_activity_ll);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        Button mEmailRegisterButton = (Button) findViewById(R.id.email_register_button);
        mEmailRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to create the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual register attempt is made.
     */
    private void attemptRegister() {
        // Store values at the time of the register attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        //Check if it is a temporaryPassiveUser
        //If it is passive , updatePassword
        //Otherwise register normally
        auth.signInWithEmailAndPassword(email,defaultTemporaryUserRegistrationPassword).addOnCompleteListener(new LoginTemporaryUserTaskListener());
    }

    private class LoginTemporaryUserTaskListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            showProgress(false);

            if (task.isSuccessful()) {
                //Temporary User needs to be converted into definitive user
                AuthResult authResult = task.getResult();
                String password = mPasswordView.getText().toString();
                authResult.getUser().updatePassword(password);
                Log.d(TAG,"Passive User , Password Aggiornata");
                if(authResult.getUser().isEmailVerified()) {
                    LoginActivity.this.finish();
                } else {
                    authResult.getUser().sendEmailVerification();
                    AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Waiting for the email verification")
                            .setMessage("We have sent an email to you, please confirm your address and then sign in ")
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    dialog.show();
                }
            } else {
                //Is not a temporary user just try to register
                FirebaseAuth auth = FirebaseAuth.getInstance();
                String email = mEmailView.getText().toString();
                String password = mPasswordView.getText().toString();
                auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new RegisterTaskListener());
            }
        }
    }

    private class RegisterTaskListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            showProgress(false);

            if (task.isSuccessful()) {
                AuthResult authResult = task.getResult();
                if(!authResult.getUser().isEmailVerified()) {
                    // TODO Check if the user is already in the DB , in this case you have not to actualize again into dthe DB
                    // TODO This will be done to prevent the stupid user press two times register button without accepting the invitation mail before
                    actualizeUserIntoDB();
                    authResult.getUser().sendEmailVerification();
                    AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Waiting for the email verification")
                            .setMessage("We have sent an email to you, please confirm your address and then sign in ")
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    dialog.show();
                } else {
                    LoginActivity.this.finish();
                }
            } else {
                Exception exc = task.getException();
                AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Couldn't Register!")
                        .setMessage(exc.getLocalizedMessage())
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.show();
            }
        }
    }

    private void actualizeUserIntoDB() {
        // Write the new user inside the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference mRoot = database.getReference();
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> update = new HashMap<>();
        String email = mEmailView.getText().toString();
        EditText nameUserEditText = (EditText) findViewById(R.id.name_loginUser);
        String nameUser = nameUserEditText.getText().toString();
        update.put("email", email);
        if(nameUser.equals("")) {
            update.put("name",email);
        } else {
            update.put("name",nameUser);
        }
        Log.d(TAG,"Sto attualizzando su : mRoot/users/"+userId+"/");
        mRoot.child("users").child(userId).updateChildren(update).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (! task.isSuccessful()) {
                    String msg = task.getException().getLocalizedMessage();
                    Log.d(TAG,msg);
                    return;
                } else {
                    Log.d(TAG,"Update Child Effettuato");
                }
            }
        });
    }

    /**
     * Attempts to sign the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new LoginTaskListener());
        showProgress(true);
    }

    private class LoginTaskListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(@NonNull Task<AuthResult> task) {
            showProgress(false);

            if (task.isSuccessful()) {
                AuthResult authResult = task.getResult();
                if(authResult.getUser().isEmailVerified()) {
                    //Save Informations about email e password Internally to the phone
                    SharedPreferences sharedPref = getSharedPreferences("MyPreferences",Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("signin_email",mEmailView.getText().toString());
                    editor.putString("signin_password",mPasswordView.getText().toString());
                    editor.commit();
                    if (READ_PHONE_STATE_GUARANTEED) {
                        new MyFirebaseInstanceIdService(getApplicationContext())
                                .sendFCMRegistrationToServer();
                    }
                    LoginActivity.this.finish();
                } else {
                    authResult.getUser().sendEmailVerification();
                    AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                            .setTitle("Waiting for the email verification")
                            .setMessage("We have sent an email to you, please confirm your address and then sign in ")
                            .setPositiveButton(android.R.string.ok, null)
                            .create();
                    dialog.show();
                }
            } else {
                Exception exc = task.getException();
                AlertDialog dialog = new AlertDialog.Builder(LoginActivity.this)
                        .setTitle("Couldn't login!")
                        .setMessage(exc.getLocalizedMessage())
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                dialog.show();
            }
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }
        if(!mayRequestPhoneState()) {
            return;
        }
        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestPhoneState() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            READ_PHONE_STATE_GUARANTEED = true;
            return true;
        }
        if (checkSelfPermission(READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            READ_PHONE_STATE_GUARANTEED = true;
            return true;
        }
        if (checkSelfPermission(READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            //Ask Permission First Time
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        }

        return false;

    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }

        if (requestCode == REQUEST_READ_PHONE_STATE) {
            switch (grantResults[0]) {
                case PackageManager.PERMISSION_GRANTED:
                    READ_PHONE_STATE_GUARANTEED = true;
                    break;
                case PackageManager.PERMISSION_DENIED:
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,READ_PHONE_STATE)) {
                        //If "never ask again" isn't pressed try to ask permission again
                        Snackbar snackbar = Snackbar.make(mLinearLayout, R.string.permission_imei, Snackbar.LENGTH_INDEFINITE);
                        snackbar.setAction(android.R.string.ok, new View.OnClickListener() {
                            @Override
                            @TargetApi(Build.VERSION_CODES.M)
                            public void onClick(View v) {
                                requestPermissions(new String[]{READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
                            }
                        });
                        snackbar.show();
                    }
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }
}

