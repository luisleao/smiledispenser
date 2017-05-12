package br.com.luisleao.things.smiledispenser;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.media.Image;
import android.media.ImageReader;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManagerService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;

public class MainActivity extends Activity{

    private static final String TAG = MainActivity.class.getSimpleName();


    public enum DISPENSER_STATUS {
        IDLE,
        COUNTDOWN,
        TAKING_PICTURE,
        UPLOADING,
        SHOWING_RESULT,
        ERROR
    }

    private static final String RELAY_PIN_NAME = "BCM17"; // GPIO port wired to the RELAY
    private Gpio mRelayGpio;

    private DISPENSER_STATUS CURRENT_STATUS;

    TTSManager ttsManager;
    GestureDetector gestureDetector;




    private DispenserCamera mCamera;

    private Button mBtnTakePicture;
    private TextView mTVCurrentStatus;
    private ProgressBar mPrgsUploading;
    private TextView mTVCountdown;
    private ImageView mIVPreview;
    private ProgressBar mPrgsChangeScreen;
    private TextView tvInstructions;

    private Handler mCameraHandler;
    private HandlerThread mCameraThread;
    private Handler mCloudHandler;


    private int RELEASE_TIMER = 1000;
    private int RESULT_TIMER = 5000;
    private int totalAttempts = 0;




    private FirebaseDatabase mDatabase;


    /**
     * An additional thread for running Cloud tasks that shouldn't block the UI.
     */
    private HandlerThread mCloudThread;





    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
        });

    }

    public static ProgressDialog createProgressDialog(Context mContext) {
        ProgressDialog dialog = new ProgressDialog(mContext);
        try {
            dialog.show();
        } catch (WindowManager.BadTokenException e) {

        }
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        //dialog.setContentView(R.layout.progressdialog);
        // dialog.setMessage(Message);
        return dialog;
    }


    /**
     * Change status for the equipment and update UI
     *
     * @param new_status
     */
    private void changeStatus(final DISPENSER_STATUS new_status) {
        CURRENT_STATUS = new_status;

        DatabaseReference fbStatus = mDatabase.getReference("current_status");
        fbStatus.setValue(new_status.toString());


        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                switch (new_status) {
                    case IDLE:
                        mBtnTakePicture.setVisibility(View.VISIBLE);
                        mPrgsUploading.setVisibility(View.INVISIBLE);
                        mTVCountdown.setVisibility(View.INVISIBLE);
                        mIVPreview.setVisibility(View.INVISIBLE);
                        mPrgsChangeScreen.setVisibility(View.INVISIBLE);
                        mIVPreview.setImageResource(android.R.color.transparent);
                        tvInstructions.setVisibility(View.VISIBLE);

                        break;

                    case COUNTDOWN:
                        tvInstructions.setVisibility(View.INVISIBLE);
                        mTVCountdown.setVisibility(View.VISIBLE);
                        mBtnTakePicture.setVisibility(View.INVISIBLE);
                        break;

                    case TAKING_PICTURE:
                        mTVCountdown.setVisibility(View.INVISIBLE);
                        mIVPreview.setVisibility(View.VISIBLE);
                        mTVCurrentStatus.bringToFront();
                        break;

                    case UPLOADING:
                        mIVPreview.setVisibility(View.VISIBLE);
                        mPrgsUploading.setVisibility(View.VISIBLE);
                        mTVCurrentStatus.bringToFront();
                        mPrgsUploading.bringToFront();
                        break;

                    case SHOWING_RESULT:
                        mIVPreview.setVisibility(View.VISIBLE);
                        mPrgsUploading.setVisibility(View.INVISIBLE);
                        mPrgsChangeScreen.setVisibility(View.VISIBLE);
                        mPrgsChangeScreen.bringToFront();
                        mTVCurrentStatus.bringToFront();
                        break;

                    case ERROR:

                        break;
                }

                mTVCurrentStatus.setText(CURRENT_STATUS.toString());
            }

        });
    }







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = FirebaseDatabase.getInstance();

        ttsManager = new TTSManager();
        ttsManager.init(this);
        ttsManager.initQueue("Hello world!");


        DatabaseReference fbReleaseTimer = mDatabase.getReference("release_timer");
        fbReleaseTimer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RELEASE_TIMER = dataSnapshot.getValue(Integer.class);
                Log.i(TAG, "RELEASE TIMER CHANGED!!! **** " + String.valueOf(RELEASE_TIMER));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        DatabaseReference fbResultTimer = mDatabase.getReference("result_timer");
        fbResultTimer.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                RESULT_TIMER = dataSnapshot.getValue(Integer.class);
                Log.i(TAG, "RESULT TIMER CHANGED!!! **** " + String.valueOf(RESULT_TIMER));
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        setContentView(R.layout.activity_main);

        mBtnTakePicture = (Button)findViewById(R.id.btnTakePicture);
        mTVCurrentStatus = (TextView)findViewById(R.id.tvCurrentStatus);
        mPrgsUploading = (ProgressBar)findViewById(R.id.prgsUploading);
        mTVCountdown = (TextView)findViewById(R.id.tvCountdown);
        mIVPreview = (ImageView)findViewById(R.id.ivPreview);
        mPrgsChangeScreen = (ProgressBar)findViewById(R.id.prgsChangeScreen);
        tvInstructions = (TextView)findViewById(R.id.tvInstructions);


        gestureDetector = new GestureDetector(this, new GestureListener());


        changeStatus(DISPENSER_STATUS.IDLE);



        // We need permission to access the camera
        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return;
        }


        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        mCloudThread = new HandlerThread("CloudThread");
        mCloudThread.start();
        mCloudHandler = new Handler(mCloudThread.getLooper());


        // Camera code is complicated, so we've shoved it all in this closet class for you.
        mCamera = DispenserCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);





        // RELAY SETUP
        // Step 1. Create GPIO connection.
        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "Available GPIO: " + service.getGpioList());
        try {
            mRelayGpio = service.openGpio(RELAY_PIN_NAME);
            mRelayGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_HIGH);
            mRelayGpio.setValue(true);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }






        mBtnTakePicture.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                changeStatus(DISPENSER_STATUS.COUNTDOWN);
                ttsManager.initQueue("Give me that smile in...");


                new CountDownTimer(4000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        String time_left = String.valueOf((int)Math.floor(millisUntilFinished/1000));
                        ttsManager.addQueue(time_left);
                        mTVCountdown.setText(time_left);
                    }

                    @Override
                    public void onFinish() {
                        mTVCountdown.setText("0");
                        changeStatus(DISPENSER_STATUS.TAKING_PICTURE);
                        mCamera.takePicture();
                    }
                }.start();

//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }





            }
        });




    }


    private void releaseDispenser() {
        totalAttempts = 0;

        new Thread(new Runnable() {
            public void run() {
                Log.i(TAG, "chamando rele...");
                try {
                    mRelayGpio.setValue(false);
                    Thread.sleep(RELEASE_TIMER);
                    mRelayGpio.setValue(true);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCamera.shutDown();

        mCameraThread.quitSafely();
        mCloudThread.quitSafely();

        if (mRelayGpio  != null) {
            try {
                mRelayGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }


    }


    /**
     * Listener for new camera images.
     */
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener =
            new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    // get image bytes
                    ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                    final byte[] imageBytes = new byte[imageBuf.remaining()];
                    imageBuf.get(imageBytes);
                    image.close();

                    final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    Log.i(TAG, "*** IMAGE SIZE *** " + bitmap.getWidth() + " x " + bitmap.getHeight());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        mIVPreview.setImageBitmap(bitmap);
                        }
                    });


                    // convert and compress image to JPEG
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, out);
                    onPictureTaken(out.toByteArray());
                }
            };


    private int total = RESULT_TIMER;

    private String randomMessages(String[] messages) {
        return messages[(int)Math.floor(messages.length * Math.random())];
    }

    private boolean checkProbability(Map<String, Object> annotations, String key) {
        return annotations.containsKey(key) && (float)annotations.get(key) > 0.5;

    }



    static final String messages[] = {
            "Hold on... I am checking if you are not a robot.",
            "Just a sec. Let me see what I can tell about you...",
            "Just a second... I am checking your photo!"
    };



    static final String disabled_message_multiple_persons[] = {
            "Looks like you are not alone. Everyone should smile!",
            "You all need to smile, or no one will get candies!",
            "I see %d people here, but someone is not smiling to me."
    };

    static final String disabled_message_one_person[] = {
            "OK, let's try one more time",
            "I can't see you smiling. Let's try again!",
            "No smile... ... no candies! You can try again."
    };

    static final String enabled_ok[] = {
            "That's it. Good smile!",
            "I think you deserve candies!"
    };
    static final String enabled_multiple_times_errors[] = {
            "OK, because you are trying so hard I think you deserve this time. Don't tell anyone about that.",
            "Humm, I think that will make you smile. Enjoy these candies and try again later."
    };
    static final String enabled_cat[] = {
            "OK, I will give some candies, but it is for the cat?",
            "This cat is cute. It deserve some candies!"
    };


    static final String disabled_message_no_face[] = {
            "I can't see any face here... Are you trying to cheat?",
            "Where are you in this picture?",
            "I can't see your face! Try again."
    };

    static final String compliments_beard[] = {
            "and I like your beard!",
            "and this beard is awesome!"
    };
    static final String compliments_glasses[] = {
            "and I like your glasses!",
            "nice glasses!"
    };



    /**
     * Handle image processing in Firebase and Cloud Vision.
     */
    private void onPictureTaken(final byte[] imageBytes) {
//        if (imageBytes != null) {
//            try {
//                // Process the image using Cloud Vision
//                Map<String, Float> annotations = annotateImage(imageBytes);
//
//                Log.d(TAG, "annotations:" + annotations);
//            } catch (IOException e) {
//                Log.w(TAG, "Unable to annotate image", e);
//            }
//        }








        ttsManager.addQueue(messages[(int)Math.floor(messages.length * Math.random())]);
        changeStatus(DISPENSER_STATUS.UPLOADING);

        if (imageBytes != null) {

            Log.i(TAG, "IMAGE SIZE IS: " + imageBytes.length);

            final DatabaseReference log = mDatabase.getReference("logs").push();
            String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);
            // upload image to firebase
            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
            log.child("image").setValue(imageStr);
            log.child("released").setValue(false);

            // define last id (current photo) pushed into the database
            mDatabase.getReference("last_id").setValue(log.getKey());

            mCloudHandler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "sending image to cloud vision");
                    // annotate image by uploading to Cloud Vision API
                    try {
                        Map<String, Object> annotations = CloudVisionUtils.annotateImage(imageBytes);
                        //BatchAnnotateImagesResponse annotations = CloudVisionUtils.annotateImage(imageBytes);

                        changeStatus(DISPENSER_STATUS.SHOWING_RESULT);
                        Log.d(TAG, "cloud vision annotations:" + annotations);
                        if (annotations != null) {
                            log.child("annotations").setValue(annotations);


                            ttsManager.initQueue("");

                            total = RESULT_TIMER;
                            mPrgsChangeScreen.setMax(RESULT_TIMER);
                            mPrgsChangeScreen.setProgress(RESULT_TIMER);





                            // check CV data to confirm if dispenser will be activated

                            if (annotations.containsKey("faces") && (int)annotations.get("faces") > 0) {
                                // detected at least one face
                                int total_faces = (int)annotations.get("faces");

                                if (annotations.get("face_Joy") != null) {
                                    switch ((String)annotations.get("face_Joy")) {
                                        case "POSSIBLE":
                                        case "LIKELY":
                                        case "VERY_LIKELY":
                                            ttsManager.addQueue(
                                                    randomMessages(enabled_ok)
                                            );
                                            log.child("released").setValue(true);
                                            releaseDispenser();
                                            break;

                                        default:
                                            totalAttempts++;
                                            if (totalAttempts < 4) {
                                                if (total_faces > 1) {
                                                    ttsManager.addQueue(
                                                        String.format(randomMessages(disabled_message_multiple_persons), total_faces)
                                                    );

                                                } else {

                                                    ttsManager.addQueue(
                                                        randomMessages(disabled_message_one_person)
                                                    );
                                                }

                                            } else {
                                                // release after 3 attempts
                                                ttsManager.addQueue(
                                                        randomMessages(enabled_multiple_times_errors)
                                                );
                                                log.child("released").setValue(true);
                                                log.child("3_tentatives").setValue(true);
                                                releaseDispenser();
                                            }
                                    }
                                }

                                //TODO: add more itens detected, like beard, flags, etc.
                                if (checkProbability(annotations, "glasses")) {
                                    ttsManager.addQueue(randomMessages(compliments_glasses));
                                }
                                if (checkProbability(annotations, "beard") || checkProbability(annotations, "facialhair")) {
                                    ttsManager.addQueue(randomMessages(compliments_beard));
                                }



                            } else {
                                // no face detected
                                if (!checkProbability(annotations, "cat")) {
                                    ttsManager.addQueue(
                                            randomMessages(disabled_message_no_face)
                                    );
                                }

                            }

                            if (checkProbability(annotations, "hand")) {
                                ttsManager.addQueue("Why are you showing your hand?");
                            }

                            if (checkProbability(annotations, "cat")) {
                                ttsManager.addQueue(randomMessages(enabled_cat));
                                log.child("released").setValue(true);
                                log.child("cat_presence").setValue(true);
                                releaseDispenser();
                            }


                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Cloud Vison API error: ", e);
                        ttsManager.addQueue("Something went wrong. Can you try again?");
                        changeStatus(DISPENSER_STATUS.SHOWING_RESULT);

                    }

                    new CountDownTimer(RESULT_TIMER, 50) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            total -= 50;
                            mPrgsChangeScreen.setProgress(total);
                        }

                        @Override
                        public void onFinish() {
                            changeStatus(DISPENSER_STATUS.IDLE);
                        }
                    }.start();


                }
            });
        }
    }



    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {

            float x = e.getX();
            float y = e.getY();

            Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTVCurrentStatus.setVisibility(mTVCurrentStatus.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
                }
            });

        }
    }


}
