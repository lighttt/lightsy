package np.com.manishtuladhar.lightsy.Profile;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.ProviderQueryResult;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;
import com.nostra13.universalimageloader.core.ImageLoader;

import de.hdodenhof.circleimageview.CircleImageView;
import np.com.manishtuladhar.lightsy.Dialogs.ConfirmPasswordDialog;
import np.com.manishtuladhar.lightsy.Models.User;
import np.com.manishtuladhar.lightsy.Models.UserAccountSettings;
import np.com.manishtuladhar.lightsy.Models.UserSettings;
import np.com.manishtuladhar.lightsy.R;
import np.com.manishtuladhar.lightsy.Share.ShareActivity;
import np.com.manishtuladhar.lightsy.Utlis.FirebaseMethods;
import np.com.manishtuladhar.lightsy.Utlis.UniversalImageLoader;

public class EditProfileFragment extends android.support.v4.app.Fragment implements ConfirmPasswordDialog.OnConfirmPasswordListener {


    @Override
    public void onConfirmPassword(String password)
    {
        Log.d(TAG, "onConfirmPassword: got the password" + password);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    // Get auth credentials from the user for re-authentication. The example below shows
    // email and password credentials but there are multiple possible providers,
    // such as GoogleAuthProvider or FacebookAuthProvider.
        AuthCredential credential = EmailAuthProvider
                .getCredential(mAuth.getCurrentUser().getEmail(), password);

    ///////////////////////// Prompt the user to re-provide their sign-in credentials
        mAuth.getCurrentUser().reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful())
                        {
                            Log.d(TAG, "User re-authenticated.");

                            /////////////////////Check to see if the email is not already present in the database
                            mAuth.fetchProvidersForEmail(mEmail.getText().toString()).addOnCompleteListener(new OnCompleteListener<ProviderQueryResult>() {
                                @Override
                                public void onComplete(@NonNull Task<ProviderQueryResult> task) {
                                    if(task.isSuccessful())
                                    {
                                        try
                                        {
                                            if(task.getResult().getProviders().size() == 1)
                                            {
                                                Log.d(TAG, "onComplete: that email is already in use");
                                                Toast.makeText(getActivity(), "That email is already on use", Toast.LENGTH_SHORT).show();
                                            }
                                            else
                                            {
                                                Log.d(TAG, "onComplete: that email is available");
                                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                                                ///////////////////the email is available so update it
                                                mAuth.getCurrentUser().updateEmail(mEmail.getText().toString())
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    Log.d(TAG, "User email address updated.");
                                                                    Toast.makeText(getActivity(), "Email updated", Toast.LENGTH_SHORT).show();
                                                                    mFirebaseMethods.updateEmail(mEmail.getText().toString());
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                        catch (NullPointerException e)
                                        {
                                            Log.e(TAG, "onComplete: NullPointerException" + e.getMessage());
                                        }
                                    }
                                }
                            });
                        }
                       else {
                            Log.d(TAG, "onComplete: reauthentication failed");
                        }
                    }
                });
    }

    private static final String TAG = "EditProfileFragment";

    //Firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;
    private String userID;

    //edit profile fragment widgets
    //widgets
    private EditText mDisplayName,mUsername,mWebsite,mDescription,mEmail,mPhoneNumber;
    private TextView mChangeProfilePhoto;
    private CircleImageView mProfilePhoto;

    //variables
    private UserSettings mUserSettings;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_editprofile,container,false);
        mProfilePhoto = (CircleImageView)view.findViewById(R.id.profile_photo);
        mDisplayName = (EditText)view.findViewById(R.id.display_name);
        mUsername = (EditText)view.findViewById(R.id.username);
        mWebsite = (EditText)view.findViewById(R.id.website);
        mDescription = (EditText)view.findViewById(R.id.description);
        mEmail = (EditText)view.findViewById(R.id.email);
        mPhoneNumber = (EditText)view.findViewById(R.id.phoneNumber);
        mChangeProfilePhoto = (TextView) view.findViewById(R.id.changeProfilePhoto);
        mFirebaseMethods = new FirebaseMethods(getActivity());


        //setProfileImage();
        setupFirebaseAuth();

        //back arrow for navigating back to profileActivity
        ImageView backArrow = (ImageView) view.findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back to ProfileActivity");
                getActivity().finish();
            }
        });

        ImageView checkMark = (ImageView) view.findViewById(R.id.saveChanges);
        checkMark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: attempting to save changes");
                saveProfileSettings();
            }
        });

        return view;
    }

    /**
     * retrieves the data contained in the widgest and submits it to the database
     * before doing so it checks to make s ure the username chosen is unique
     */
    private void saveProfileSettings()
    {
        final String displayName = mDisplayName.getText().toString();
        final String username = mUsername.getText().toString();
        final String website = mWebsite.getText().toString();
        final String description = mDescription.getText().toString();
        final String email = mEmail.getText().toString();
        final long phoneNumber = Long.parseLong(mPhoneNumber.getText().toString());

        //case1: the user didnot change the username
        if(!mUserSettings.getUser().getUsername().equals(username))
        {
            checkIfUsernameExists(username);
        }
        //case2: the user changed their username therefore we need to check for uniqueness
        if(!mUserSettings.getUser().getEmail().equals(email))
        {
            //step 1. Reauthenticate
            //      - confirm the password and email
            ConfirmPasswordDialog dialog = new ConfirmPasswordDialog();
            dialog.show(getFragmentManager(),getString(R.string.confirm_password_dialog));
            //after closing the dialog ka janey bhanera target tetai lagcha natra app restart bhayera MainActivity ma jancha
            dialog.setTargetFragment(EditProfileFragment.this,1);

            //step 2. check if the email is already registered
            //      - fetchProviderForEmail(String email);
            //step 3. change the email
            //      - submit the new email to the database and authentication

        }
        /**
         * change the rest of the settings that donot require uniqueness
         */

        if(!mUserSettings.getSettings().getDisplay_name().equals(displayName))
        {
            //update displayName
            mFirebaseMethods.updateUserAccountSettings(displayName,null,null,0);
        }
        if(!mUserSettings.getSettings().getWebsite().equals(website))
        {
            //update website
            mFirebaseMethods.updateUserAccountSettings(null,website,null,0);
        }
        if(!mUserSettings.getSettings().getDescription().equals(description))
        {
            //update description
            mFirebaseMethods.updateUserAccountSettings(null,null,description,0);
        }
        if(!mUserSettings.getSettings().getProfile_photo().equals(phoneNumber))
        {
            //update phoneNumber
            mFirebaseMethods.updateUserAccountSettings(null,null,null,phoneNumber);
        }

    }



    /**
     * check if @param username already exists in the database
     * @param username
     */
    private void checkIfUsernameExists(final String username) {
        Log.d(TAG, "checkIfUsernameExists: Checking if "+username + "already exists");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

        //using query so that looping chalaunu naparne gari sab user search garna sajilo hos
        //looks node then object and matches the username
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_username))
                .equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if(!dataSnapshot.exists())
                {
                    //add the username
                    mFirebaseMethods.updateUsername(username);
                    Toast.makeText(getActivity(), "Saved username", Toast.LENGTH_SHORT).show();
                }
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren())
                {
                    if(singleSnapshot.exists())
                    {
                        Log.d(TAG, "checkIfUsernameExists: FOUND A MATCH " + singleSnapshot.getValue(User.class).getUsername());
                        Toast.makeText(getActivity(), "That username already exists", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

//        private void setProfileImage()
//        {
//            Log.d(TAG, "setProfileImage: setting profile Image");
//            String imgURL = "www.androidcentral.com/sites/androidcentral.com/files/styles/xlarge/public/article_images/2016/08/ac-lloyd.jpg?itok=bb72IeLf";
//            UniversalImageLoader.setImage(imgURL,mProfilePhoto,null,"https://");
//        }

    private void setProfileWidgets(UserSettings userSettings){
//        Log.d(TAG, "setProfileWidgets: setting widgets with data retrieving from firebase database: " + userSettings.toString());
//        Log.d(TAG, "setProfileWidgets: setting widgets with data retrieving from firebase database: " + userSettings.getSettings().getUsername());


        mUserSettings = userSettings;
        //User user = userSettings.getUser();
        UserAccountSettings settings = userSettings.getSettings();

        UniversalImageLoader.setImage(settings.getProfile_photo(), mProfilePhoto, null, "");

        mDisplayName.setText(settings.getDisplay_name());
        mUsername.setText(settings.getUsername());
        mWebsite.setText(settings.getWebsite());
        mDescription.setText(settings.getDescription());
        mEmail.setText(userSettings.getUser().getEmail());
        mPhoneNumber.setText(String.valueOf(userSettings.getUser().getPhone_number()));
        mChangeProfilePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Changing profile photo");
                Intent intent = new Intent(getActivity(), ShareActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //268435456
                getActivity().startActivity(intent);
                getActivity().finish();
            }
        });

    }

      /*
    --------------------------------------------Firebase----------------------------------
     */


    /*
    setup the firebase auth object
     */
    private void setupFirebaseAuth()
    {
        Log.d(TAG, "setupFirebaseAuth: setting up Firebase Auth");
        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();
        userID = mAuth.getCurrentUser().getUid();

        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                //check the user if logged in

                if(user !=null)
                {
                    //user is signed in
                    Log.d(TAG, "onAuthStateChanged: signed in"+user.getUid());
                }
                else
                {
                    //user is signed out
                    Log.d(TAG, "onAuthStateChanged: signed out");
                }
            }
        };

        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //retrieve user information from the database
                setProfileWidgets(mFirebaseMethods.getUserSettings(dataSnapshot));

                //retrieve image for the user in question
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthStateListener !=null)
        {
            mAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

}

