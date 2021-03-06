package com.example.moodswing.Fragments;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.moodswing.R;
import com.example.moodswing.customDataTypes.FirestoreUserDocCommunicator;
import com.example.moodswing.customDataTypes.MoodEvent;
import com.example.moodswing.customDataTypes.MoodEventUtility;
import com.example.moodswing.customDataTypes.UserJar;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

/**
 * The moodDetail screen for followers, different than for the user's own detail view
 */

public class MoodDetailFollowingFragment extends Fragment{
    private FirestoreUserDocCommunicator communicator;
    UserJar userJar;
    MoodEvent moodEvent;

    private TextView dateText;
    private TextView timeText;
    private TextView moodText;
    private TextView reasonText;
    private TextView socialText;
    private TextView locationText;

    private TextView usernameText;

    private FloatingActionButton backButton;
    private ImageView moodImage;
    private ImageView locationImg;
    private ImageView socialIcon;
    private ImageView photoImage;

    public MoodDetailFollowingFragment(){}

    public MoodDetailFollowingFragment(int userJarPosition) {
        this.communicator = FirestoreUserDocCommunicator.getInstance();
        this.userJar = communicator.getUserJar(userJarPosition);
        this.moodEvent = userJar.getMoodEvent();
        // moodEvent

    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mood_detail_fragment_following, container, false);

        // find view
        dateText = root.findViewById(R.id.moodDetail_following_dateText);
        timeText = root.findViewById(R.id.moodDetail_following_timeText);
        moodText = root.findViewById(R.id.moodDetail_following_moodText);
        reasonText = root.findViewById(R.id.detailedView_following_reasonText);
        backButton = root.findViewById(R.id.detailedView_following_back);
        moodImage = root.findViewById(R.id.detailedView_following_moodImg);
        socialText = root.findViewById(R.id.moodDetail_following_SocialText);
        locationImg = root.findViewById(R.id.moodDetail_following_locationImg);
        socialIcon = root.findViewById(R.id.moodDetail_following_socialSitIcon);
        locationText = root.findViewById(R.id.moodDetail_following_locationText);
        photoImage = root.findViewById(R.id.moodDetail_following_image_place_holder);
        usernameText = root.findViewById(R.id.moodDetail_following_username);

        initialElements();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFrag();
            }
        });

        return root;
    }

    /**
     * closes the fragment
     */
    private void closeFrag(){
        getFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
                .remove(this)
                .commit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            initialElements();
        }
    }


    /**
     * initializes all the fields to their views
     */
    private void initialElements(){
        dateText.setText(MoodEventUtility.getDateStr(moodEvent.getTimeStamp()));
        timeText.setText(MoodEventUtility.getTimeStr(moodEvent.getTimeStamp()));
        moodText.setText(MoodEventUtility.getMoodType(moodEvent.getMoodType()));
        moodImage.setImageResource(MoodEventUtility.getMoodDrawableInt(moodEvent.getMoodType()));
        setReasonText();
        setSocialSituation();
        usernameText.setText(userJar.getUsername());
        locationText.setText("");
        if (userJar.getMoodEvent().getLatitude() == null) {
            locationImg.setImageResource(R.drawable.ic_location_off_grey_24dp);
        }else{
            locationImg.setImageResource(R.drawable.ic_location_on_accent_red_24dp);
            setLocationStrFromLocation();
        }
        setUpPhoto();
    }

    private void setUpPhoto(){
        if (moodEvent.getImageId() != null){
            // exist
            communicator.getPhoto(moodEvent.getImageId(), photoImage, userJar.getUID());
        }else{
            photoImage.setImageDrawable(null);
        }
    }

    /**
     * gets the location and converts it into a string
     */
    private void setLocationStrFromLocation(){
        communicator.getAsynchronousTask()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        updateLocationStr();
                    }
                });
    }

    /**
     * updates the location
     */
    private void updateLocationStr(){
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        if (moodEvent.getLatitude() != null){
            try {
                List<Address> firstAddressList = geocoder.getFromLocation(moodEvent.getLatitude(),moodEvent.getLongitude(),1);
                if (firstAddressList != null){
                    if (firstAddressList.isEmpty()){
                        // error
                    }else{
                        Address address = firstAddressList.get(0);
                        String locationForDisplay = address.getThoroughfare();
                        if (locationForDisplay == null){
                            locationForDisplay = address.getPremises();
                            if (locationForDisplay == null){
                                locationForDisplay = address.getLocality();
                                if (locationForDisplay == null){
                                    locationForDisplay = address.getCountryName();
                                    if (locationForDisplay == null){
                                        locationForDisplay = address.getCountryName();
                                        if (locationForDisplay == null){
                                            locationForDisplay = "Can't find address";
                                        }
                                    }
                                }
                            }
                        }
                        locationText.setText(locationForDisplay);
                    }
                }else {
                    // error
                }
            } catch (Exception e) {
                // display error msg
                e.printStackTrace();
            }
        }else{
            //
        }
    }

    /**
     * simple setter for reason
     */
    private void setReasonText(){
        if (moodEvent.getReason() != null){
            this.reasonText.setVisibility(View.VISIBLE);
            this.reasonText.setText(String.format(Locale.getDefault(), "\"%s\"",(moodEvent.getReason())));
        }else{
            this.reasonText.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * social situation has 3 buttons, depending on which is clicked is the one that is set
     */
    private void setSocialSituation(){
        Integer socialSituation = moodEvent.getSocialSituation();
        switch (socialSituation){
            case 0:
                this.socialText.setVisibility(View.INVISIBLE);
                this.socialIcon.setVisibility(View.INVISIBLE);
                break;
            case 1:
                this.socialText.setVisibility(View.VISIBLE);
                this.socialIcon.setVisibility(View.VISIBLE);
                this.socialText.setText("Alone");
                this.socialIcon.setImageResource(R.drawable.ic_person_black_24dp);
                break;
            case 2:
                this.socialText.setVisibility(View.VISIBLE);
                this.socialIcon.setVisibility(View.VISIBLE);
                this.socialText.setText("Company");
                this.socialIcon.setImageResource(R.drawable.ic_people_black_24dp);
                break;
            case 3:
                this.socialText.setVisibility(View.VISIBLE);
                this.socialIcon.setVisibility(View.VISIBLE);
                this.socialText.setText("Party");
                this.socialIcon.setImageResource(R.drawable.ic_account_group);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if(getView() == null){
            return;
        }

        getView().setFocusableInTouchMode(true);
        getView().requestFocus();
        getView().setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK){
                    closeFrag();
                    return true;
                }
                return false;
            }
        });
    }
}