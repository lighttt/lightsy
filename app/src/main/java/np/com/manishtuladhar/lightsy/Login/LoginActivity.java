package np.com.manishtuladhar.lightsy.Login;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import np.com.manishtuladhar.lightsy.Home.MainActivity;
import np.com.manishtuladhar.lightsy.R;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private Context mContext;
    private ProgressBar mProgressBar;
    private EditText mEmail,mPassword;
    private TextView mPleaseWait;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        mPleaseWait = (TextView)findViewById(R.id.loginingPleaseWait);
        mEmail = (EditText) findViewById(R.id.input_email);
        mPassword = (EditText)findViewById(R.id.input_password);
        mContext = LoginActivity.this;

        Log.d(TAG, "onCreate: started");

        mPleaseWait.setVisibility(View.GONE);
        mProgressBar.setVisibility(View.GONE);

        setupFirebaseAuth();
        init();
    }

    //check null strings
    private boolean isStringNull(String string)
    {
        Log.d(TAG, "isStringNull: checking is string is null");

        if(string.equals(""))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

         /*
        ------------------------------------ Firebase ---------------------------------------------
         */

        private void init()
        {

            //initialize the button for logging in
            Button btnLogin = (Button) findViewById(R.id.btn_login);
            btnLogin.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: attempting to log in.");

                    String email = mEmail.getText().toString();
                    String password = mPassword.getText().toString();

                    if(isStringNull(email) && isStringNull(password))
                    {
                        Toast.makeText(mContext, "You must fill out all the fields", Toast.LENGTH_SHORT).show();
                    }
                    else
                     {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mPleaseWait.setVisibility(View.VISIBLE);

                        mAuth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                    @Override
                                    public void onComplete(@NonNull Task<AuthResult> task) {
                                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
                                        FirebaseUser user = mAuth.getCurrentUser();

                                        // If sign in fails, display a message to the user. If sign in succeeds
                                        // the auth state listener will be notified and logic to handle the
                                        // signed in user can be handled in the listener.
                                        if (!task.isSuccessful())
                                        {
                                            Log.w(TAG, "signInWithEmail:failed", task.getException());

                                            Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                                            mProgressBar.setVisibility(View.GONE);
                                            mPleaseWait.setVisibility(View.GONE);
                                        }
                                        else
                                        {
                                            try{
                                                if(user.isEmailVerified())
                                                {
                                                    Log.d(TAG, "onComplete: success. email is verified");
                                                    Intent intent = new Intent(LoginActivity.this,MainActivity.class);
                                                    startActivity(intent);
                                                }
                                                else
                                                {
                                                    Toast.makeText(mContext, "Email is not verified \n Check your email inbox.", Toast.LENGTH_SHORT).show();
                                                    mProgressBar.setVisibility(View.GONE);
                                                    mPleaseWait.setVisibility(View.GONE);
                                                    mAuth.signOut();
                                                }
                                            }
                                            catch (NullPointerException e)
                                            {
                                                Log.d(TAG, "onComplete: NullPointerException" + e.getMessage());
                                            }
                                        }
                                    }
                                    // ...
                                });
                     }
                }
            });

            TextView linkSignUp = (TextView) findViewById(R.id.link_signup);
            linkSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "onClick: navigating to register screen");
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                }
            });

             /*
             If the user is logged in then navigate to MainActivity and call 'finish()'
              */
            if(mAuth.getCurrentUser() != null){
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }

    /**
     * Setup the firebase auth object
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

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

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}


