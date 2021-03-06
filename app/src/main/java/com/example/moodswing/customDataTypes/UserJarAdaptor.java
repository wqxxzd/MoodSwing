package com.example.moodswing.customDataTypes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodswing.MainActivity;
import com.example.moodswing.R;

import java.util.ArrayList;
//

/**
 * This class is an adapter for UserJars to be shown, will be used for following/follower list
 *
 */
public class UserJarAdaptor extends RecyclerView.Adapter<UserJarAdaptor.MyViewHolder> {

    private ArrayList<UserJar> userJars;
//    private Integer selectedPosition; // note: use of this attribute MAY cause bug (not matching) because of realtime listner,
//    // need to invest more later! - Scott (especially on following screen, where the card at position can be changed in realtime)

    /**
     * Initializes the views(time,date,moodtype,username,the mood image, and the card they are in) and relates them to their XML IDs
     */
    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView moodType;
        TextView dateText;
        TextView timeText;
        TextView username;
        ImageView moodImage;
        ImageView locationImage;
        CardView userJarCard;

        public MyViewHolder(View view){
            super(view);
            this.moodType = view.findViewById(R.id.followingList_moodDetail_moodText);
            this.dateText = view.findViewById(R.id.followingList_moodDetail_dateText);
            this.timeText = view.findViewById(R.id.followingList_moodDetail_timeText);
            this.username = view.findViewById(R.id.followingList_username);
            this.moodImage = view.findViewById(R.id.followingList_moodIcon_placeHolder);
            this.locationImage = view.findViewById(R.id.followingList_locationButton_moodListCard);
            this.userJarCard = view.findViewById(R.id.followingList_moodCard);
        }
    }

    /**
     * intializes the arraylist of UserJars for the adapter
     * @param userJars the arraylist to display
     */
    public UserJarAdaptor(ArrayList<UserJar> userJars) {
        //Customize the list
        this.userJars = userJars;
//        this.selectedPosition = null;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder (ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_mood_list_following, parent, false);
        return new MyViewHolder(view);
    }

    /**
     * sets the value of the views(except the mood itself) created in MyViewHolder
     */
    @Override
    public void onBindViewHolder (final MyViewHolder holder, final int position) {
        TextView moodType = holder.moodType;
        TextView dateText = holder.dateText;
        TextView timeText = holder.timeText;
        TextView usernameTextView = holder.username;

        ImageView moodImage = holder.moodImage;
        ImageView locationImage = holder.locationImage;

        UserJar userJar = userJars.get(position);
        MoodEvent moodEvent = userJar.getMoodEvent();

        dateText.setText(MoodEventUtility.getDateStr(moodEvent.getTimeStamp()));
        timeText.setText(MoodEventUtility.getTimeStr(moodEvent.getTimeStamp()));
        printMoodTypeToCard(moodEvent.getMoodType(),moodType, moodImage);
        usernameTextView.setText(userJar.getUsername());

        holder.userJarCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDetailedViewActivity(holder.getLayoutPosition(),v);
            }
        });

        if (moodEvent.getLatitude() == null) {
            locationImage.setImageResource(R.drawable.ic_location_off_grey_24dp);
        }else{
            locationImage.setImageResource(R.drawable.ic_location_on_accent_red_24dp);
        }
    }


    /**
     * starts detailed view from follower list
     * @param cardPosition the position in the recyclerview of the card
     * @param view the view it is in
     */
    private void startDetailedViewActivity (int cardPosition,View view){
        // cardPosition will be passed to detailed view
        ((MainActivity) view.getContext()).toDetailedView_following(cardPosition);
    }

    /**
     * Prints the mood to the CardView
     * @param moodTypeInt the mood number
     * @param moodText the text associated with the mood number
     * @param moodImage the image associated with the mood number
     */
    private void printMoodTypeToCard(int moodTypeInt, TextView moodText, ImageView moodImage) {

        moodText.setText(MoodEventUtility.getMoodType(moodTypeInt));
        moodImage.setImageResource(MoodEventUtility.getMoodDrawableInt(moodTypeInt));
    }

    @Override
    public int getItemCount() {
        return userJars.size();
    }

}