package fr.julienderenty.crepes;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Date;


public class MainActivity extends Activity implements SensorEventListener {

    private static String TAG = "CrepesApp";
    private static Context context;

    //Default cook time
    static int loopTime = 40000;

    //Existing sleep time setting
    int sleepTime;


    /***********    UI     ***********/
    public static TextView instructions;
    static TextView timerDisplay;
    static ImageView pictureCrepe;
    Button reset;
    NumberPicker numberPicker;

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_main);
        MainActivity.context = getApplicationContext();

        // Sensor listener initialization
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);

        //UI Initialization
        instructions = (TextView) findViewById(R.id.instructions);
        timerDisplay = (TextView) findViewById(R.id.textView);
        pictureCrepe = (ImageView) findViewById(R.id.pictureCrepe);
        reset = (Button) findViewById(R.id.reset);
        numberPicker = (NumberPicker) findViewById(R.id.loopTime);

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetTimer();
            }
        });


        numberPicker.setMaxValue(100);
        numberPicker.setMinValue(10);
        numberPicker.setValue(loopTime/1000);
        numberPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i2) {
                loopTime = numberPicker.getValue()*1000;
            }
        });

        // Stop screen sleep
        try {
            sleepTime = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 1800000);
    }


    @Override
    protected void onPause() {
        super.onPause();

        //Restore screen sleepTime
        android.provider.Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, sleepTime);

        this.finish();
    }


    public static State state = State.firstExplanation;


    /***********  SENSOR  ***********/
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    static CountDownTimer waitTimer;

    Float trigger = 0.3f;
    private SensorBuffer lastValues=new SensorBuffer(50);

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            lastValues.put(sensorEvent.values[2]);

            // If we have movement : check if too much
            Float mean = lastValues.mean();
            Float variation = lastValues.variation();

            //Any case, if the phone is moved : return to beginning
            if(mean > 0.02){
                state.firstExplanation();
            }

            switch (state){

                case firstExplanation:

                    // If phone is stable, laying, we can wait for the start
                    if(mean!=0 && Math.abs(mean) < 0.004) {
                        state.waitingForCrepe();
                    }
                    break;

                case waitingForCrepe:

                    //If  when the phone laying we detect the trigger : launch the counter
                    if (variation > trigger) {
                        state.countingDown();
                    }
                    break;

                case countingDown:
                    //do nothing
                    break;

                case waitingForUser:

                    // If user acts : restart timer
                    if (variation > trigger) {
                        state.countingDown();
                    }

                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Do nothing
    }



    /*********** APP STATE ***********/

    /**
     * Describe and change app's state :
     *   - Explanations before phone lied
     *   - Waiting for crepe
     *   - Counting down until the crepe is cooked
     *   - Waiting for user action
     */
    enum State {
        firstExplanation, waitingForCrepe, countingDown, waitingForUser;

        //Ensure you can't change state too fast
        long lastChange = new Date().getTime();
        long toggle = 3000;

        public void firstExplanation(){
            long time = new Date().getTime();
            if(time - lastChange > toggle) {
                lastChange = time;
                instructions.setText("Posez le tél près de la poele");
                instructions.setBackgroundColor(Color.RED);
                state = firstExplanation;
                resetTimer();
            }
        }

        public void waitingForCrepe() {
            long time = new Date().getTime();
            if(time - lastChange > toggle) {
                lastChange = time;
                instructions.setText("Posez la poêle avec une crêpe :)");
                instructions.setBackgroundColor(Color.YELLOW);

                state = waitingForCrepe;
                if(waitTimer!=null) waitTimer.cancel();

            }
        }

        public void countingDown() {
            long time = new Date().getTime();
            if(time - lastChange > toggle) {
                lastChange = time;
                instructions.setText("Laissez cuire !");
                instructions.setBackgroundColor(Color.GREEN);
                state = countingDown;

                pictureCrepe.setVisibility(View.INVISIBLE);
                startTimer();
            }
        }

        public void waitingForUser() {
            long time = new Date().getTime();
            if(time - lastChange > toggle) {
                lastChange = time;
                pictureCrepe.setVisibility(View.VISIBLE);
                instructions.setText("Retournez la");
                instructions.setBackgroundColor(Color.YELLOW);
                state = waitingForUser;
            }

        }
    }

    /***********  TIMER  ***********/
    private static void startTimer() {
        if(waitTimer!=null)
        {
            waitTimer.cancel();
        }

        waitTimer = new CountDownTimer(loopTime, 100) {

            public void onTick(long millisUntilFinished) {
                timerDisplay.setText(millisUntilFinished/1000 + "s");
            }

            public void onFinish() {
                playSound();
                state.waitingForUser();
            }
        }.start();
    }

    private static void resetTimer() {
        if(waitTimer != null) {
            waitTimer.cancel();
            waitTimer = null;
        }
        timerDisplay.setText(loopTime/1000 + "s");
    }

    private static void playSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(MainActivity.context, notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
