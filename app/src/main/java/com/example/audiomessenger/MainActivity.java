package com.example.audiomessenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.ToggleButton;

import java.io.File;
import java.util.GregorianCalendar;

public class MainActivity extends Activity
{
    private final int SAMPLE_RATE = 44100; // Hz

    private File RECORDINGS_DIR;

    private MediaRecorder m_Recorder;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        RECORDINGS_DIR = new File(getFilesDir(), getString(R.string.RecordingsDirName));
        setContentView(R.layout.activity_main);
        getActionBar().setTitle(this.getClass().getSimpleName());
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        // create recorder
        m_Recorder = new MediaRecorder();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);

        // make sure the ToggleButton is in the off state
        ((ToggleButton)findViewById(R.id.toggleButton)).setChecked(false);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        // reset and release recorder - it should not be used after this point
        m_Recorder.reset();
        m_Recorder.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id)
        {
            case R.id.action_recordings:
            {
                // go to list of recordings
                Intent i = new Intent(this, RecordingsListActivity.class);
                startActivity(i);
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void ToggleButtonClicked(View v)
    {
        ToggleButton button = (ToggleButton) v;
        if(button.isChecked())
        {
            startRecording();
        }
        else
        {
            m_Recorder.stop();
        }
    }

    private void startRecording()
    {
        final String curUNIXTimeStr = String.valueOf(GregorianCalendar.getInstance().getTimeInMillis()); // current time (for file name)
        // the absolute path to where user-generated recordings go (internal storage)
        if(!RECORDINGS_DIR.exists())
        {
            RECORDINGS_DIR.mkdir();
        }
        // set audio source to mic
        m_Recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // set sample rate
        m_Recorder.setAudioSamplingRate(SAMPLE_RATE);
        // set output format
        m_Recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // set audio encoder
        m_Recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        // set output file name
        m_Recorder.setOutputFile(new File(RECORDINGS_DIR, curUNIXTimeStr.concat(AppConstants.FILE_EXT)).getAbsolutePath());
        // prepare
        try
        {
            m_Recorder.prepare();
        }
        catch(Exception e)
        {
            Log.e("", e.toString());
        }
        // actually begin recording
        try
        {
            m_Recorder.start();
        }
        catch(Exception e)
        {
            Log.e("", e.toString());
        }
    }

}
