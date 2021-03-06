package com.example.moodswing.customDataTypes;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moodswing.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import static com.example.moodswing.customDataTypes.MoodEventUtility.TOTAL_MOOD_TYPE_COUNTS;


/**
 * this class is an adapter for selecting the mood
 */

public class SelectMoodAdapter extends RecyclerView.Adapter<SelectMoodAdapter.MyViewHolder> {

//    private List<Integer> moodID;
//    private List<String> moodText;
//    private LayoutInflater mInflater;
//    private ItemClickListener mClickListener;
//    private Integer selectedPosition = -1;
//
//
//
//
//    // data is passed into the constructor
//    SelectMoodAdapter(Context context, List<Integer> moodID_, List<String> moodText_) {
//        this.mInflater = LayoutInflater.from(context);
//        this.moodID = moodID_;
//        this.moodText = moodText_;
//    }
    private ArrayList<Integer> moodTypes;
    private Integer selectedPosition;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        TextView moodTypeText;
        View holderView;
        ImageView moodImage;
        CardView card;
        FloatingActionButton color;

        public MyViewHolder(View view){
            super(view);
            this.holderView = view;
            this.moodTypeText = view.findViewById(R.id.moodType_Text);
            this.moodImage = view.findViewById(R.id.moodIcon);
            this.card = view.findViewById(R.id.selectCard);
            this.color = view.findViewById(R.id.selectMoodContent_color);
        }
    }

    /**
     * initializes an array of numbers that correspond to each mood in the view, sets the one selected to null
     */
    public SelectMoodAdapter() {
        selectedPosition = null;
        moodTypes = new ArrayList<>();
        for (int i = 1; i <= TOTAL_MOOD_TYPE_COUNTS; i++){
            moodTypes.add(i);
        }
    }

    public SelectMoodAdapter(int moodType) {
        this();
        this.selectedPosition = getSelectedPosition(moodType);
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder (ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_selectmood, parent, false);
        return new MyViewHolder(view);
    }

    /**
     * Sets the viewable card on screen to have the mood text(ie HAPPY), and the
     * associated picture(ie. a happy face)
     */
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        TextView moodTypeText= holder.moodTypeText;
        ImageView moodImage= holder.moodImage;
        int moodType = moodTypes.get(position);
        moodTypeText.setText(MoodEventUtility.getMoodType(moodType));
        moodImage.setImageResource(MoodEventUtility.getMoodDrawableInt(moodType));
        holder.color.setBackgroundTintList(
                ColorStateList.valueOf(holder
                        .holderView
                        .getResources()
                        .getColor(MoodEventUtility.getMoodColorResInt(moodType))));

        // preSelect
        if (selectedPosition != null) {
            if (selectedPosition == position){
                holder.card.setElevation(2f);
                holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
            }
        }

        holder.card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedPosition == null) {
                    selectedPosition = holder.getLayoutPosition();
                    holder.card.setElevation(2f);
                    holder.card.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                }else if (selectedPosition == holder.getLayoutPosition()){
                    selectedPosition = null;
                    holder.card.setElevation(5f);
                    holder.card.setCardBackgroundColor(Color.parseColor("#FFFFFF"));
                }
            }
        });
    }

    // useful for its subclass
    public ArrayList<Integer> getMoodTypes() {
        return moodTypes;
    }

    @Override
    public int getItemCount() {
        return moodTypes.size();
    }

    /**
     * returns the selected mood
     * @return the selected mood
     */
    public Integer getSelectedMoodType(){
        if (selectedPosition != null) {
            return moodTypes.get(selectedPosition);
        }else{
            return null;
        }
    }

    private Integer getSelectedPosition(int moodType){
        return moodTypes.indexOf(moodType);
    }
}