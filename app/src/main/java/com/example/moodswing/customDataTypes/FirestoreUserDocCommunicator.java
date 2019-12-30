package com.example.moodswing.customDataTypes;

import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

/**
 * This class Handles all the functionality related to firestore, a go-between for the app and firestore
 *
 */
public class FirestoreUserDocCommunicator{

    private static FirestoreUserDocCommunicator instance = null;

    private static final String TAG = "FirestoreUserDocCommuni";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private DocumentSnapshot userDocSnapshot;

    private ObservableMoodEventArray moodEvents;
    private ArrayList<UserJar> userJars;
    // reference

    private FirebaseStorage storage;

    // for filter
    private ArrayList<Integer> moodTypeFilterList_moodHistory;
    private ArrayList<Integer> moodTypeFilterList_following;

    // for image
    private RecentImagesBox recentImagesBox;

    // testing
    private String appInstanceID;

    protected FirestoreUserDocCommunicator(){
        // init db
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        this.user = mAuth.getCurrentUser();
        this.userDocSnapshot = null;
        this.moodEvents = new ObservableMoodEventArray();
        this.userJars = new ArrayList<>();

        this.getUserSnapShot();

        // init filter
        moodTypeFilterList_moodHistory = new ArrayList<>();
        moodTypeFilterList_following = new ArrayList<>();
        storage = FirebaseStorage.getInstance();

        // for image
        recentImagesBox = new RecentImagesBox();

        // init moodEvents, testing
        this.getMoodEventListInstance(this.moodEvents);
        this.appInstanceID = generateUniqueID();
        this.setUpAppLock();
    }

    private boolean ifLogin(){
        return user != null;
    }

    /**
     * Accesses the firestore database and gets a specific user
     */
    private void getUserSnapShot(){
        // throw exception here if not login
        db.collection("users")
                .document(user.getUid())
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            userDocSnapshot = task.getResult();
                            Log.d(TAG, "get userDocSnap success");
                        }else{
                            Log.d(TAG, "get userDocSnap failed with", task.getException());
                }
            }
        });
    }

    private void setUpAppLock(){
        this.appInstanceID = this.generateUniqueID();

        db.collection("users")
                .document(user.getUid())
                .update("mostRecentAppID",this.generateUniqueID());
    }

    /**
     * Signs out the current user
     */
    private void userSignOut(){
        mAuth.signOut();
        user = null;
    }

    /**
     * Gets the current user's username
     * @return A string of the username
     */
    public String getUsername(){
        if (userDocSnapshot != null) {
            return (String) userDocSnapshot.get("username");
        }else{
            return null;
            // something wrong, possible not enough time to finish query
        }
    }

    /**
     * this method returns the reference to the singleton object
     * @return FirestoreUserDoccommunicator reference
     */
    public static FirestoreUserDocCommunicator getInstance() {
        if (instance == null) {
            instance = new FirestoreUserDocCommunicator();
        }
        return instance;
    }

    /**
     * Signs the user out
     */
    public static void destroy(){
        instance.userSignOut();
        instance = null;
    }

    /* moodEvent related methods*/

    /**
     * Generates the users Mood ID
     * @return Returns the ID as a string
     */
    public String generateUniqueID(){
        String refID = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .document()
                .getId();


        return refID;
    }

    /**
     * Adds a mood event to the user's list of moods in firestore
     * @param moodEvent the moodEvent to be added
     */
    public void addMoodEvent(MoodEvent moodEvent) {
        // lacking error returning code here
        //
        // required fields. no handling here, moodEvent class should handle it

        DocumentReference newMoodEventRef = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .document(moodEvent.getUniqueID());

        newMoodEventRef.set(moodEvent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "moodEvent upload successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "moodEvent upload fail");
                    }
                });
        updateRecentMoodToFollowers();

        this.moodEvents.add(moodEvent);
        this.moodEvents.notifyChange();
    }

    /**
     * Deletes a mood event from in the users list and their followers in the firestore database
     * @param moodEvent The mood event to be deleted
     */
    public void removeMoodEvent(MoodEvent moodEvent){

        // remove from moodEvents
        Integer moodPosition = getMoodPosition(moodEvent.getUniqueID());
        if (moodPosition != null){
            moodEvents.remove((int)moodPosition);
            this.moodEvents.notifyChange();
        }

        // remove image if exist
        if (moodEvent.getImageId() != null){
            deleteFirestoreImage(moodEvent.getImageId());
        }

        DocumentReference moodEventRef = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .document(moodEvent.getUniqueID());

        moodEventRef
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "moodEvent delete successful");
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "moodEvent delete fail");
                    }
                });
        updateRecentMoodToFollowers();

    }

    private void getMoodEventListInstance(ObservableMoodEventArray moodEvents) {
        Query moodEventsQuery = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .orderBy("timeStamp", Query.Direction.DESCENDING);
        moodEventsQuery
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        QuerySnapshot querySnapshot= task.getResult();
                        if (!(querySnapshot.isEmpty())){
                            for (DocumentSnapshot moodEventDoc : querySnapshot.getDocuments()){
                                MoodEvent moodEvent = moodEventDoc.toObject(MoodEvent.class);
                                moodEvents.add(moodEvent);
                            }
                        }
                        moodEvents.notifyChange();
                    }
                });
    }

    public void addMoodListObserverClient(ObservableMoodEventArray.ObservableMoodEventArrayClient client){
        this.moodEvents.addClient(client);
    }

    public void removeMoodListObserverClient(ObservableMoodEventArray.ObservableMoodEventArrayClient client){
        this.moodEvents.removeClient(client);
    }

    public boolean containMoodListObserverClient(ObservableMoodEventArray.ObservableMoodEventArrayClient client){
        return this.moodEvents.containClient(client);
    }

    /**
     * Initializes the mood events for the user into the local RecyclerView
     * @param moodList the view the moods are being appended to
     */
    public void initMoodEventsList(final RecyclerView moodList, ArrayList<Integer> unwanttedMoodTypes){
        @NonNull
        MoodAdapter moodAdapter = (MoodAdapter) moodList.getAdapter();
        moodAdapter.clearMoodEvents();
        Collections.sort(this.moodEvents);
        for (MoodEvent moodEvent : this.moodEvents) {
            if(!(unwanttedMoodTypes.contains(moodEvent.getMoodType()))){
                moodAdapter.addToMoods(moodEvent);
            }
            moodAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Updates/edits an existing moodEvent
     * @param moodEvent The moodEvent to edit
     */
    /* user management related methods */
    public void updateMoodEvent(MoodEvent moodEvent){
        DocumentReference moodEventRef = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .document(moodEvent.getUniqueID());
        moodEventRef.set(moodEvent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "moodEvent upload successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "moodEvent upload fail");
                    }
                });
        updateRecentMoodToFollowers();
    }

    public MoodEvent getMoodEvent(int position) {
        return moodEvents.get(position);
    }

    public UserJar getUserJar(int position) {
        return userJars.get(position);
    }

    public Integer getMoodPosition(String moodID){
        int position = 0;
        for (MoodEvent moodEvent : moodEvents) {
            if(moodEvent.getUniqueID() == moodID) {
                return position;
            }
            position ++;
        }
        return null;    // not found
    }

    public Integer getUserJarPosition(String moodID){
        int position = 0;
        for (UserJar userJar : userJars) {
            if(userJar.getMoodEvent().getUniqueID() == moodID) {
                return position;
            }
            position ++;
        }
        return null;    // not found
    }

    // following feature

    /**
     * Sends a following request to another user
     * @param username The username of the user that is going to receive the request
     */
    // mailBox feature
    public void sendFollowingRequest (String username) {
        // should first check if uid exist
        Query findUserColQuery = db
                .collection("users")
                .whereEqualTo("username",username)
                .limit(1);

        findUserColQuery
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().isEmpty()){
                                // do something
                            }else{
                                // not empty, proceed
                                // should be only one
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    String UID = document.getId();
                                    addRequestToMailBox(UID);
                                }
                            }
                        }else{
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    /**
     * given an UID, adds request to the target users mailBox
     * @param targetUID the receiving user's UID
     */
    private void addRequestToMailBox(String targetUID){
        DocumentReference requestRef = db
                .collection("users")
                .document(targetUID) // enter other user's doc
                .collection("mailBox")
                .document(user.getUid()); // doc name is your UID


        UserJar userJar = new UserJar();
        userJar.setUsername(getUsername());
        userJar.setUID(user.getUid());

        requestRef.set(userJar)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "sending request successful");
                        // maybe do something
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "sending request failed");
                    }
                });
    }

    /**
     * Deletes a request from the user's mailbox collection
     * @param userJar The userJar of the user to be deleted
     */
    public void removeRequest (UserJar userJar) {
        // error code need to be created
        DocumentReference requestRef = db
                .collection("users")
                .document(user.getUid())
                .collection("mailBox")
                .document(userJar.getUID());

        requestRef
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "request delete successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "request delete fail");
                    }
                });
    }

    /**
     * Accepts an incoming follow request
     * @param userJar The userJar of the user sending the request
     */
    public void acceptRequest(UserJar userJar) {
        // responding to sender's request
        // two action to do here,
        // 1. adding the uid&username (current entry) to user's permitted list
        // 2. pack uid and RecentMood, send recentMood to sender
        //    a. send uid/username to sender's following list  b. trigger one recentMood update

        removeRequest(userJar);
        DocumentReference myPermittedListRef = db
                .collection("users")
                .document(user.getUid())
                .collection("permittedList")
                .document(userJar.getUID());
        myPermittedListRef
                .set(userJar)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "accept Step 1 Success");
                        addToSendersFollowing(userJar);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "accept Fail");
                    }
                });
    }

    /**
     * Adds a user to the senders following collection after an accepted request
     * @param sendersUserJar The sendering user's userJar
     */
    private void addToSendersFollowing(UserJar sendersUserJar) {
        // at this point UID should be always correct, since it is checked in sendingRequest method
        String sendersUID = sendersUserJar.getUID();
        UserJar myUserJar = new UserJar();
        myUserJar.setUID(user.getUid());
        myUserJar.setUsername(getUsername());

        DocumentReference followingListReference = db
                .collection("users")
                .document(sendersUID)
                .collection("following")
                .document(user.getUid());

        followingListReference.set(myUserJar)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "following upload successful");
                        refreshRecentMoodToUser(sendersUID);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "following upload fail");
                    }
                });
    }

    /**
     * This method refreshes the recent mood(ie. when a user deletes, edits, or adds a new mood to
     * their list) and sends the updated status to firestore
     */
    private void updateRecentMoodToFollowers(){

        CollectionReference permittedList = db
                .collection("users")
                .document(user.getUid())
                .collection("permittedList");

        permittedList.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()){
                            if (task.getResult().isEmpty()){
                                Log.d(TAG, "no followers");
                            }else{
                                for (DocumentSnapshot userJarDoc : task.getResult().getDocuments()){
                                    UserJar userJar = userJarDoc.toObject(UserJar.class);
                                    refreshRecentMoodToUser(userJar.getUID());
                                }
                            }
                        }else{
                            Log.d(TAG, "failed finding permittedList");
                        }
                    }
                });
    }

    /**
     * This method gets the updated status of the user from firestore
     * @param uid UID of the user
     */
    public void refreshRecentMoodToUser (String uid){
        if (moodEvents.isEmpty()){
            pullRecentMoodEventToUser(uid);
        }else{
            pushRecentMoodEventToUser(uid);
        }
    }


    /**
     *  This method handles getting the moodevent locally from the user, and then sending it to the
     *  receiving user
     * @param uid UID of the user receiving the status update
     */
    private void pushRecentMoodEventToUser(String uid){
        // grab recentMoodEvent
        Query mostRecentMoodEventDocQuery = db
                .collection("users")
                .document(user.getUid())
                .collection("MoodEvents")
                .orderBy("timeStamp", Query.Direction.DESCENDING)
                .limit(1);

        mostRecentMoodEventDocQuery
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG,"successfully got most recent Mood");
                            if (task.getResult().isEmpty()) {
                                Log.d(TAG, "something is wrong in refreshRecentMoodToUser method");
                                // this line should never be excuted.
                            } else {
                                MoodEvent mostRecentMoodEvent = task.getResult().toObjects(MoodEvent.class).get(0);
                                // construct UserJar
                                UserJar myUserJarWithMood = new UserJar();
                                myUserJarWithMood.setUsername(getUsername());
                                myUserJarWithMood.setUID(user.getUid());
                                myUserJarWithMood.setMoodEvent(mostRecentMoodEvent);

                                // send it to target
                                DocumentReference followingMoodListDoc = db
                                        .collection("users")
                                        .document(uid)
                                        .collection("followingMoodList")
                                        .document(user.getUid());

                                followingMoodListDoc.set(myUserJarWithMood)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "sending mood to target uid, done");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "sending mood to target uid, failed");
                                            }
                                        });
                            }
                        }else{
                            Log.d(TAG,"get most recent Mood query failed");
                        }
                    }
                });
    }

    /**
     * Grabs the moodEvent from firestore from the sending user and receives it to populate the
     * follower's list
     * @param uid UID of the user who you want to update the status/current mood of
     */
    private void pullRecentMoodEventToUser(String uid) {

        // send it to target
        DocumentReference followingMoodListDoc = db
                .collection("users")
                .document(uid)
                .collection("followingMoodList")
                .document(user.getUid());

        followingMoodListDoc.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "pull mood from target uid, done");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "pull mood from target uid, failed");
                    }
                });
    }

    /**
     * Gets all the users from firestore that the current user is following and populates the local user's following list with them
     * @param userJarList A view of all users the current user is following
     */
    public void initFollowingList(final RecyclerView userJarList, ArrayList<Integer> unwanttedMoodTypes){
        @NonNull
        UserJarAdaptor userJarAdaptor = (UserJarAdaptor) userJarList.getAdapter();

        Query followingMoodListColQuery = db
                .collection("users")
                .document(user.getUid())
                .collection("followingMoodList")
                .orderBy("moodEvent.timeStamp",Query.Direction.DESCENDING);

        followingMoodListColQuery.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                userJarAdaptor.clearUserJars();
                for (QueryDocumentSnapshot userJarDoc : queryDocumentSnapshots){
                    UserJar userJar = userJarDoc.toObject(UserJar.class);
                    if (unwanttedMoodTypes.contains(userJar.getMoodEvent().getMoodType())){
                        continue;
                    }else{
                        userJarAdaptor.addToUserJars(userJar);
                    }
                }
                userJarAdaptor.notifyDataSetChanged();
                userJars = userJarAdaptor.getUserJars();
            }
        });
    }

    /**
     * this method returns an instance of all the userJars in followingMoodlist
     * @return an ArrayList<UserJar> object contains all the userJars
     */
    public ArrayList<UserJar> getUserJars(){
        return this.userJars;
    }

    /**
     * Populates the Following list for the management screen
     * @param userJarList The view to be populated with users that we're following
     */
    public void initManagementFollowingList(final RecyclerView userJarList){
        @NonNull
        SimpleUserJarAdapter userJarAdaptor = (SimpleUserJarAdapter) userJarList.getAdapter();

        CollectionReference followingListColRef = db
                .collection("users")
                .document(user.getUid())
                .collection("following");

        followingListColRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                userJarAdaptor.clearUserJars();
                for (QueryDocumentSnapshot userJarDoc : queryDocumentSnapshots){
                    UserJar userJar = userJarDoc.toObject(UserJar.class);
                    userJarAdaptor.addToUserJars(userJar);
                }
                userJarAdaptor.notifyDataSetChanged();
            }
        });
    }

    /**
     * Initiatizes the request list on the management screen by getting the unresolved request from the users mailbox in firestore
     * @param userJarList the view to be populated with requests
     */
    public void initManagementRequestList(final RecyclerView userJarList){
        @NonNull
        SimpleUserJarAdapter userJarAdaptor = (SimpleUserJarAdapter) userJarList.getAdapter();

        CollectionReference requestListColRef = db
                .collection("users")
                .document(user.getUid())
                .collection("mailBox");

        requestListColRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                userJarAdaptor.clearUserJars();
                for (QueryDocumentSnapshot userJarDoc : queryDocumentSnapshots){
                    UserJar userJar = userJarDoc.toObject(UserJar.class);
                    userJarAdaptor.addToUserJars(userJar);
                }
                userJarAdaptor.notifyDataSetChanged();
            }
        });
    }


    /**
     * unfollows the user given in the userJar
     * @param userJar the user to unfollow
     */
    public void unfollow(UserJar userJar){
        // 从对方的permittedlist中移除自己
        // 把对方从自己的followinglist中移除
        // 把对方从自己的followingMoodList中移除

        DocumentReference followingListUserDocRef = db
                .collection("users")
                .document(user.getUid())
                .collection("following")
                .document(userJar.getUID());

        DocumentReference followingMoodListUserDocRef = db
                .collection("users")
                .document(user.getUid())
                .collection("followingMoodList")
                .document(userJar.getUID());


        DocumentReference targetPermittedListUserJarDocRef = db
                .collection("users")
                .document(userJar.getUID())
                .collection("permittedList")
                .document(user.getUid());

        targetPermittedListUserJarDocRef
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "find user's doc in target's permitted list successful");
                        followingListUserDocRef
                                .delete()
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "remove target from user's followingList successful");
                                        followingMoodListUserDocRef
                                                .delete()
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        Log.d(TAG, "remove target from user's followingMoodList successful");
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        Log.d(TAG, "failed in removing target from user's followingMoodList");
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "failed in removing target from user's followingList");
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "failed in finding user's doc in target's permitted list");
                    }
                });

    }

    /**
     * gets the document for the user
     * @return the user document from firestore to return
     */
    public DocumentReference getUserDocRef(){
        return db
                .collection("users")
                .document(user.getUid());
    }

    /**
     * simple getter
     * @return mood history list
     */
    public ArrayList<Integer> getMoodHistoryFilterList(){
        return this.moodTypeFilterList_moodHistory;
    }

    /**
     * simple getter
     * @return followingfilterlist
     */
    public ArrayList<Integer> getFollowingFilterList(){
        return this.moodTypeFilterList_following;
    }


    /**
     * simple getter
     * @return moodevents
     */
    public ArrayList<MoodEvent> getMoodEvents() {
        return moodEvents;
    }

    /**
     * deletes an image in the firestore storage(not database)
     * @param imageId the ID of the image
     */
    public void deleteFirestoreImage(String imageId){
        if (recentImagesBox.getImage(imageId) != null){
            recentImagesBox.delImage(imageId);
        }

        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        // Create a reference to the file to delete
        StorageReference desertRef = storageRef.child("Images/" + user.getUid() + "/" + imageId);

        // Delete the file
        desertRef
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "File deleted successfully");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "File deleted error");
                    }
                });
    }

    /**
     * uploads a photo to firebase storage
     * @param uniqueImageID the unique image ID
     * @param filePath the local filepath
     * @param imageView imageView for cache
     */
    public void uploadPhotoToStorage(String uniqueImageID, Uri filePath, ImageView imageView) {
        recentImagesBox.addImage(uniqueImageID, imageView);
        StorageReference storageRef = storage.getReference();
        StorageReference storageName = storageRef.child("Images/" + user.getUid() + "/" + uniqueImageID);

        storageName
                .putFile(filePath)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.d(TAG, "onSuccess: upload image");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: upload image");
                    }
                });
    }

    /**
     * gets a photo from firebase storage
     * @param imageId the image id of the photo
     * @param imageView the imageview the photo will be set to
     */
    // retrieve image from firebase storage and set into imageView
    public void getPhoto(String imageId, ImageView imageView){

        ImageView imageViewTemp = recentImagesBox.getImage(imageId);
        if (imageViewTemp != null){
            imageView.setImageDrawable(imageViewTemp.getDrawable());
            return;
        }

        StorageReference storageRef = storage.getReference();
        StorageReference storageName = storageRef.child("Images/" + user.getUid() + "/" + imageId);

        storageName
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String url = uri.toString();
                        Log.d(TAG, "onSuccess: download Photo successful");
                        Picasso.get().load(url).into(imageView);
                        recentImagesBox.addImage(imageId, imageView);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: download Photo");
                    }
                });

    }

    /**
     * gets a photo from firebase storage
     * @param imageId the image ID
     * @param imageView the imageview to display the image
     * @param uid the user's UID
     */
    public void getPhoto(String imageId, ImageView imageView, String uid){

        ImageView imageViewTemp = recentImagesBox.getImage(imageId);
        if (imageViewTemp != null){
            imageView.setImageDrawable(imageViewTemp.getDrawable());
            return;
        }


        StorageReference storageRef = storage.getReference();
        StorageReference storageName = storageRef.child("Images/" + uid + "/" + imageId);

        storageName
                .getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        String url = uri.toString();
                        Log.d(TAG, "onSuccess: download Photo successful");
                        Picasso.get().load(url).into(imageView);
                        recentImagesBox.addImage(imageId, imageView);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: download Photo");
                    }
                });
    }

    /**
     * Starts an async task
     * @return returns the database
     */
    public Task<DocumentSnapshot> getAsynchronousTask(){
        return db
                .collection("users")
                .document(user.getUid())
                .get();
    }
    public ArrayList<UserJar> getFollowingMoodEvents() {
        return userJars;
    }

    public RecentImagesBox getRecentImagesBox(){
        return recentImagesBox;
    }

}
