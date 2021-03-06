package com.sparkmap.sparkmap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;
    private Boolean isNewUser = false;
    private Boolean notValid = false;
    private boolean cancel = false;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    //Firebase variables
    private FirebaseAuth mAuth; //FirebaseAuth Object
    private FirebaseAuth.AuthStateListener mAuthListener; //AuthStateListener object

    private static final String TAG = "LoginActivity"; //TAG Object

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
        populateAutoComplete();

        //Initializing the firebaseauth object
        mAuth = FirebaseAuth.getInstance();

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

        final Button mEmailSignInButton = (Button) findViewById(R.id.email_sign_in_button);
        final CheckBox newUserCheckBox = (CheckBox) findViewById(R.id.isNewUserCheckBox);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!newUserCheckBox.isChecked()) {
                    attemptLogin();
                }else{
                    isNewUser = true;
                    attemptNewUser(newUserCheckBox, mEmailSignInButton);
                }
            }
        });


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        //Authstatelistener method, tracks whenever the user signs in or out
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };
    }
    //Attaches the firebaseauth instance when the activity is started.
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        mAuth.addAuthStateListener(mAuthListener);
        FirebaseUser currentUser = mAuth.getCurrentUser();
    }

    private void populateAutoComplete() {

    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     * @param newUserCheckBox
     * @param mEmailSignInButton
     */
    private void attemptNewUser(CheckBox newUserCheckBox, Button mEmailSignInButton){
        //Update the UI to reflect a new user being created successfully
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        View focusView = checkValid(email, password);

        if(cancel){
            cancel = false;
            focusView.requestFocus();
        }else {
            //Firebase method to create a new user and add the user to our app's firebase online console
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                //Account creation successful, Prompt the user to verify their email address before logging in
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Toast.makeText(LoginActivity.this, "Account created successfully, Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                                //Method to send verification email to newly minted account
                                user.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Log.d(TAG, "Email sent.");
                                                }
                                            }
                                        });
                            } else {
                                // If account creation, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Account creation failure.", Toast.LENGTH_SHORT).show();
                            }
                            // ...
                        }
                    });
            newUserCheckBox.toggle();
            //mEmailSignInButton.performClick();
        }
    }

    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();


        View focusView = checkValid(email, password);

        // Check for a valid password, if the user entered one.


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            cancel = false;
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            mAuthTask = new UserLoginTask(email, password);
            mAuthTask.execute((Void) null);

        }
    }

    private View checkValid(String email, String password){
        View focusView = null;
        if(TextUtils.isEmpty(password)){
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }else if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }
        return focusView;
    }


    private boolean isEmailValid(String email) {
        //todo
        //Just checks if follows right "something"@"something".com format, need to verify email in future
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    private boolean containUppercase(String password) {
        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (ch >= 65 || ch <= 90) {
                return true;
            }
        }
        return false;
    }

    private boolean containnumber(String password){
        for(int i=0; i<password.length();i++){
            char ch= password.charAt(i);
            if(ch>=48||ch<= 57) {
                return true;
            }
        }
        return false;
    }
    private boolean containsymbol(String password) {

        for (int i = 0; i < password.length(); i++) {
            char ch = password.charAt(i);
            if (ch >= 33 || ch <= 47) {
                return true;
            }
        }
        return false;
    }
    private boolean isPasswordValid(String password) {
        if (password.length() < 6) {
            return false;
        }
        if(containUppercase(password)==false) {
            return false;
        }
        if(containnumber(password)==false) {
            return false;
        }
        if(containsymbol(password)==false) {
            return false;
        }
        return true;
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

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        //Entered email and password
        private final String mEmail;
        private final String mPassword;
        //Variable to determine if authentication was successful 0=failure 1=success
        public Integer auth = null;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Firebase attempt login authentication against a network service.
            mAuth.signInWithEmailAndPassword(mEmail, mPassword)
                    .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, grant the user access to the application
                                Log.d(TAG, "signInWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                auth = 1;
                                //If its the user's first time logging in, display tutorial text (And user is verified)
                                if(isNewUser && user != null && user.isEmailVerified()) {
                                    //welcome text
                                    CharSequence text_welcome = "Welcome to SparkMap!";
                                    int duration = Toast.LENGTH_SHORT;
                                    Toast toast_welcome = Toast.makeText(getApplicationContext(), text_welcome, duration);
                                    toast_welcome.setGravity(Gravity.CENTER_HORIZONTAL, 0, -100);
                                    toast_welcome.show();

                                    //tools text
                                    duration = Toast.LENGTH_LONG;
                                    CharSequence text_tools = " Click here to reveal map tools -->";
                                    Toast toast_tools = Toast.makeText(getApplicationContext(), text_tools, duration);
                                    toast_tools.setGravity(Gravity.BOTTOM | Gravity.CENTER, -50, 100);
                                    toast_tools.show();

                                    duration = Toast.LENGTH_LONG;
                                    CharSequence text_nav = " ^ Click here to reveal the drawer";
                                    Toast toast_nav = Toast.makeText(getApplicationContext(), text_nav, duration);
                                    toast_nav.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT, 0, -650);
                                    toast_nav.show();

                                }
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication Failure.", Toast.LENGTH_SHORT).show();
                                auth = 0;
                            }
                            // ...
                        }
                    });
            try {
                //Busy wait for 1ms until the authentication process is complete
                while(auth == null){
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                return false;
            }
            //Final check on what to return to onPostExecute, if a user is validated and authenticated log them in, deny them otherwise
            if(auth != null){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(auth == 1){
                    if(user != null && user.isEmailVerified()) {
                        return true;
                    }
                    else {
                        notValid = true;
                        return false;
                    }
                }
                else{
                    return false;
                }
            }
            //Return false by default, this code will only be reached if there is some kind of error
            Toast.makeText(LoginActivity.this, "Authentication Timeout.", Toast.LENGTH_SHORT).show();
            return false;
        }


        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
                new User(mEmail);
                finish();
            }
            //If a user hasn't validated their email address print this error message
            else if(notValid){
                mEmailView.setError(getString(R.string.error_nonvalid_email));
                mEmailView.requestFocus();
            }
            else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.setText("");
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    @Override
    public void onBackPressed() {
        /*do nothing
        *forces a user to login, can not skip this step
        */
    }

    //Removes the firebaseauth instance when the activity is closed
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

}

