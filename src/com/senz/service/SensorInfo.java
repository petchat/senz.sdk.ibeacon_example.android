package com.senz.service;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.senz.utils.L;
//import com.senz.utils.Writer;

/***********************************************************************************************************************
 * @ClassName:   SensorInfo
 * @Author:      Woodie
 * @CreateAt:    Sat, Nov 15, 2014
 * @Description: It's a Sensor manager.
 *               - First, you need instantiate it, and the para is context and a SensorHandler(It's a interface defined by user)
 *               - When you instantiated it, it would init the sensor module and start collecting data.
 *               - You can manipulate data in SensorHandler from the sensor every time when the sensors's data changed.
 *               - Also, you can collect the data whenever you want, all you need is accessing the SensorInfo's private member
 *               - GyroValues, AcceValues, and LightValues.
 * @Hint:        Don't block the callback method. Sensor data can change at a high rate, which means the
 *               system may call the onSensorChanged() and onAccuracyChanged() method quite often.
 ***********************************************************************************************************************/
public class SensorInfo{

    // Sensor Manager
    private SensorManager mSensorManager;
    // Sensor
    private Sensor mAccelerometer;
    private Sensor mGyroscope;
    private Sensor mLight;
    // Context
    private Context Ctx;
    // The value of Gyroscope.
    public float GyroValues[] = new float[]{0,0,0};
    // The value of Accelerometer.
    public float AcceValues[] = new float[]{0,0,0};
    // The value of Light.
    public float LightValues = 0;
    // Interface - defined by user.
    private SensorHandler sensorHandler;

    //Writer
    //private Writer gyroWriter = null;
    //private Writer acceWriter = null;
    //private Writer lightWriter = null;

    // It is the Sensor Event Listener.
    // Here, we instantiate it and override it's two method
    private SensorEventListener sensorEventListener = new SensorEventListener(){
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Get the sensors' data.
            // HIGH FREQUENCY
            if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
            {
                // Acceleration force along the x axis(Excluding gravity, m/s2)
                AcceValues[0] = event.values[0];
                // Acceleration force along the y axis(Excluding gravity, m/s2)
                AcceValues[1] = event.values[1];
                // Acceleration force along the z axis(Excluding gravity, m/s2)
                AcceValues[2] = event.values[2];
                // Save the accelerometers' data to local file.
                HandleAcceData();
            }
            // HIGH FREQUENCY
            else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            {
                // Gyroscope force along the x axis(with drift compensation, rad/s)
                GyroValues[0] = event.values[0];
                // Gyroscope force along the y axis(with drift compensation, rad/s)
                GyroValues[1] = event.values[1];
                // Gyroscope force along the z axis(with drift compensation, rad/s)
                GyroValues[2] = event.values[2];
                // Save the Gyroscopes' data to local file.
                HandleGyroData();
            }
            // LOW FREQUENCY
            else if(event.sensor.getType() == Sensor.TYPE_LIGHT)
            {
                // Illuminance(lx)
                LightValues = event.values[0];
                HandleLightData();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    // Constructor
    public SensorInfo(Context ctx, SensorHandler ltn) {
        // Check context is exist.
        if (ctx == null) {
            L.e("The context of SensorInfo is null.");
        }
        else {
            Ctx = ctx;
            sensorHandler = ltn;
            // Get Sensor's Services.
            mSensorManager = (SensorManager) Ctx.getSystemService(Context.SENSOR_SERVICE);
            // Get corresponding Sensors.
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            mGyroscope     = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mLight         = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            // Check Accelerometer is exist.
            if (mAccelerometer != null) {
                // Success! There's a accelerometer.
                L.i("There's a accelerometer!");
                // Sensor start listening.
                mSensorManager.registerListener(sensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
            else {
                // Failure! No accelerometer.
                L.e("Failure! No accelerometer!");
            }

            // Check Gyroscope is exist.
            if (mGyroscope != null) {
                // Success! There's a mGyroscope.
                L.i("There's a mGyroscope!");
                // Sensor start listening.
                mSensorManager.registerListener(sensorEventListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            }
            else {
                // Failure! No mGyroscope.
                L.e("Failure! No mGyroscope!");
            }

            // Check Light is exist.
            if (mLight != null) {
                // Success! There's a Light.
                L.i("There's a Light!");
                // Sensor start listening.
                mSensorManager.registerListener(sensorEventListener, mLight, SensorManager.SENSOR_DELAY_NORMAL);
            }
            else {
                // Failure! No Light.
                L.e("Failure! No Light!");
            }
        }
    }

    // Unregister Sensor
    public void unregisterSensor()
    {
        L.i("Unregister Sensor");
        mSensorManager.unregisterListener(sensorEventListener);
    }

    // This method will be invoked when Accelemeters' data changed
    private void HandleAcceData()
    {
        // It's not pretty, but it works(any calculations in onSensorChanged aren't pretty to be fair).
        //new Thread(new Runnable(){
            //@Override
            //public void run()
            //{
                //acceWriter.writeAcceToFile(AcceValues);
                sensorHandler.AcceHandler(AcceValues);
            //}
        //}).start();
    }

    // This method will be invoked when Gyroscopes' data changed
    private void HandleGyroData()
    {
        // It's not pretty, but it works(any calculations in onSensorChanged aren't pretty to be fair).
        //new Thread(new Runnable(){
            //@Override
            //public void run()
            //{
                //gyroWriter.writeGyroToFile(GyroValues);
                sensorHandler.GyroHandler(GyroValues);
            //}
        //}).start();
    }

    // This method will be invoked when Lights' data changed
    private void HandleLightData()
    {
        // It's not pretty, but it works(any calculations in onSensorChanged aren't pretty to be fair).
        //new Thread(new Runnable(){
            //@Override
            //public void run()
            //{
                //gyroWriter.writeGyroToFile(GyroValues);
                sensorHandler.LightHandler(LightValues);
            //}
        //}).start();
    }

    // It's a user - interface to define callback.
    // In this method, you can manipulate data from the sensor every time when the sensors's data changed
    // Also, you can collect the data whenever you want, what all you need is access the SensorInfo's private member -
    // GyroValues, AcceValues, and LightValues.
    public interface SensorHandler {
        // All operation about Accelemeter, Gyroscope and Light data is defined in following method.
        // These method all run in a new thread. So you can put complicated computation here.
        // The para is the current data which collect from android sensor.
        public void AcceHandler(float Acce[]);
        public void GyroHandler(float Gyro[]);
        public void LightHandler(float Light);
    }

}
