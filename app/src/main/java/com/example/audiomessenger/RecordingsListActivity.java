package com.example.audiomessenger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class RecordingsListActivity extends ListActivity implements AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener,
                                                                    MediaPlayer.OnCompletionListener
{
    private File RECORDINGS_DIR;

    private final String ALERT_TITLE = "File Options";
    private final String ALERT_BUTTON_CANCEL = "Cancel";
    private final String ALERT_BUTTON_PLAY = "Play";
    private final String ALERT_BUTTON_SEND = "Send";
    private final String ALERT_MESSAGE = "What would you like to do with this message?";

    private final String ALERT_TITLE_LONGCLICK = "Rename / Delete";
    private final String ALERT_BUTTON_SAVE = "Save";
    private final String ALERT_BUTTON_DELETE = "Delete";
    private final String ALERT_MESSAGE_LONGCLICK = "Enter file name...";
    
    private ArrayAdapter<String> m_Adapter;
    private MediaPlayer m_Player;
    private int m_SelectedItemIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        RECORDINGS_DIR = new File(getFilesDir(), getString(R.string.RecordingsDirName));
        m_Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        getListView().setAdapter(m_Adapter);
        getListView().setOnItemClickListener(this);
        getListView().setOnItemLongClickListener(this);
        reloadFiles();
        getActionBar().setTitle(this.getClass().getSimpleName());
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        m_Player = new MediaPlayer();
        m_Player.setOnCompletionListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if(m_Player.isPlaying())
        {
            m_Player.stop();
        }
        m_Player.reset();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        m_Player.release();
        m_Player = null;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_recordings_list, menu);
        if(m_Player.isPlaying())
        {
            // only enable the stop button if audio is actually playing (default is disabled)
            menu.findItem(R.id.action_stop).setEnabled(true);
        }
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id)
        {
            case R.id.action_stop:
            {
                if(m_Player.isPlaying())
                {
                    m_Player.stop();
                }
                m_Player.reset();
                item.setEnabled(false);
                break;
            }
            default:
            {
                break;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // adapted from http://stackoverflow.com/questions/2679699/what-characters-allowed-in-file-names-on-android ... TODO - review this
    private final String PROHIBITED_CHARS = "|?*<>+/\\'\":";

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
    {
        // if we are playing, stop
        if(m_Player.isPlaying())
        {
            m_Player.stop();
        }
        m_Player.reset();
        // redraw Action bar
        invalidateOptionsMenu();

        // save the index of the clicked item ...
        m_SelectedItemIndex = position;
        // ... and the filename it represents
        final String originalFileName = (String) getListView().getItemAtPosition(m_SelectedItemIndex);

        // show the options popup
        AlertDialog popup = new AlertDialog.Builder(this).create();
        popup.setCanceledOnTouchOutside(false);
        popup.setCancelable(false);
        popup.setTitle(ALERT_TITLE_LONGCLICK);
        final EditText nameEntryField = new EditText(this);
        nameEntryField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); // turn off AutoCorrect
        nameEntryField.setHint(ALERT_MESSAGE_LONGCLICK);
        nameEntryField.setFilters(new InputFilter[] { new InputFilter()
        {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
            {
                if(source.length() == 0)
                {
                    // this is a delete - always allow
                    return null;
                }
                StringBuilder builder = new StringBuilder();
                for(int i = start; i < end; i++)
                {
                    if(!PROHIBITED_CHARS.contains(source.subSequence(i, i + 1))) // CharSequence equivalent of charAt(i)
                    {
                        builder.append(source.charAt(i));
                    }
                }
                // if the string the user has typed has any valid chars, allow it through
                // otherwise, keep what was there before
                return builder.length() > 0 ? builder.toString() : dest.subSequence(dstart, dend);
            }
        } });
        popup.setView(nameEntryField);
        popup.setButton(DialogInterface.BUTTON_POSITIVE, ALERT_BUTTON_SAVE, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                String newFileName = nameEntryField.getText().toString();
                // append suffix if necessary (i.e. the filename does not end with ".mp4"
                final int suffixPos = newFileName.indexOf(AppConstants.FILE_EXT);
                if(suffixPos == -1 || suffixPos != newFileName.length() - AppConstants.FILE_EXT.length())
                {
                    newFileName = newFileName.concat(AppConstants.FILE_EXT);
                }

                // save the file with the new name
                final File savedFile = new File(RECORDINGS_DIR, originalFileName);
                if(savedFile.exists())
                {
                    if(savedFile.renameTo(new File(RECORDINGS_DIR, newFileName)))
                    {
                        reloadFiles();
                    }
                    else
                    {
                        // something went wrong
                        Toast oops = Toast.makeText(RecordingsListActivity.this, "Uh oh.. file wasn't saved. Try again?", Toast.LENGTH_SHORT);
                        oops.setGravity(Gravity.CENTER, 0, 0);
                        oops.show();
                    }
                }
            }
        });
        popup.setButton(DialogInterface.BUTTON_NEUTRAL, ALERT_BUTTON_DELETE, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // delete the file
                final File savedFile = new File(RECORDINGS_DIR, originalFileName);
                if (savedFile.exists())
                {
                    if(savedFile.delete())
                    {
                        reloadFiles();
                    }
                    else
                    {
                        // something went wrong
                        Toast oops = Toast.makeText(RecordingsListActivity.this, "Uh oh.. file wasn't deleted. Try again?", Toast.LENGTH_SHORT);
                        oops.setGravity(Gravity.CENTER, 0, 0);
                        oops.show();
                    }
                }
            }
        });
        popup.setButton(DialogInterface.BUTTON_NEGATIVE, ALERT_BUTTON_CANCEL, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // here we want to cancel - nothing to do
            }
        });
        popup.show();

        return true;
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        mp.reset();
        // redraw action bar to disable "stop" button
        invalidateOptionsMenu();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id)
    {
        final ListView list = (ListView) parent;

        // if we are playing, stop
        if(m_Player.isPlaying())
        {
            m_Player.stop();
        }
        m_Player.reset();
        // redraw Action bar
        invalidateOptionsMenu();

        // save the index of the clicked item
        m_SelectedItemIndex = position;

        // show the options popup
        AlertDialog popup = new AlertDialog.Builder(this).create();
        popup.setCanceledOnTouchOutside(false);
        popup.setCancelable(false);
        popup.setTitle(ALERT_TITLE);
        popup.setButton(DialogInterface.BUTTON_POSITIVE, ALERT_BUTTON_SEND, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                Intent sharingIntent = new Intent();
                sharingIntent.setAction(Intent.ACTION_SEND);

                final String fileName = (String) list.getAdapter().getItem(position);
                final File fileObj = new File(RECORDINGS_DIR, fileName);
                Uri fileUri = FileProvider.getUriForFile(RecordingsListActivity.this, getString(R.string.FileProviderAuthority), fileObj);
                sharingIntent.setType(getContentResolver().getType(fileUri));
                sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                startActivity(sharingIntent);
            }
        });
        popup.setButton(DialogInterface.BUTTON_NEUTRAL, ALERT_BUTTON_PLAY, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // here we want to play the file
                final String fileNameToPlay = (String) getListView().getItemAtPosition(m_SelectedItemIndex);
                try
                {
                    m_Player.setDataSource(new File(RECORDINGS_DIR, fileNameToPlay).getAbsolutePath());
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
                try
                {
                    m_Player.prepare();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                m_Player.start();
                // redraw action bar to enable "stop" button
                invalidateOptionsMenu();
            }
        });
        popup.setButton(DialogInterface.BUTTON_NEGATIVE, ALERT_BUTTON_CANCEL, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                // here we want to cancel - nothing to do
            }
        });
        popup.setMessage(ALERT_MESSAGE);
        popup.show();
    }

    private void reloadFiles()
    {
        m_Adapter.clear();
        if(!RECORDINGS_DIR.exists())
        {
            RECORDINGS_DIR.mkdir();
        }
        File [] files = RECORDINGS_DIR.listFiles();
        if(null != files)
        {
            ArrayList<String> fileNames = new ArrayList<String>();
            int numFiles = Arrays.asList(files).size();
            for(int i = 0; i < numFiles; i++)
            {
                fileNames.add(files[i].getName());
            }
            m_Adapter.addAll(fileNames);
            m_Adapter.notifyDataSetChanged();
        }
    }

}
