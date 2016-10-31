package com.quintet.audiorecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import java.io.IOException;
import java.util.Calendar;

public class AudioRecorderActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AudioRecordTest";
    private static String mFileName     = null;

    private MediaRecorder mRecorder     = null;
    private MediaPlayer mPlayer         = null;

    private TextView mRecordButton;
    private TextView mPlayButton;

    private boolean mStartRecording     = true;
    private boolean mStartPlaying       = true;

    private Animation mPulseAnimation;

    final int MSG_START_TIMER           = 0;
    final int MSG_STOP_TIMER            = 1;
    final int MSG_UPDATE_TIMER          = 2;

    StopWatch timer         = new StopWatch();
    final int REFRESH_RATE  = 100;

    public AudioRecorderActivity() {
        mFileName   = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName   += "/" + Calendar.getInstance().getTimeInMillis() +  "_recordedaudiofile.3gp";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_recorder);

        mPulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation);

        mRecordButton   = (TextView) findViewById(R.id.id_record_text);
        mPlayButton     = (TextView) findViewById(R.id.id_play_text);

        mRecordButton.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.INVISIBLE);

        mRecordButton.setText("RECORD");
        mPlayButton.setText("PLAY");

        mRecordButton.setBackgroundResource(R.drawable.stop_circle);

        mRecordButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_none_black, 0, 0, 0);
        mPlayButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_black, 0, 0, 0);

        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(AudioRecorderActivity.this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(AudioRecorderActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO}, 100);
                    if (ActivityCompat.checkSelfPermission(AudioRecorderActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(AudioRecorderActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);

                    }
                } else {
                    onRecord(mStartRecording);
                    if (mStartRecording) {
                        mRecordButton.setText("STOP");
                        mRecordButton.setBackgroundResource(R.drawable.record_circle);
                        mRecordButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_black, 0, 0, 0);
                    } else {
                        mRecordButton.setText("RECORD");
                        mRecordButton.setBackgroundResource(R.drawable.stop_circle);
                        mRecordButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_mic_none_black, 0, 0, 0);
                    }
                    mStartRecording = !mStartRecording;
                }
            }
        });

        mPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlay(mStartPlaying);
                if (mStartPlaying) {
                    mPlayButton.setText("STOP");
                    mPlayButton.setBackgroundResource(R.drawable.record_circle);
                    mPlayButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_black, 0, 0, 0);
                } else {
                    mPlayButton.setText("PLAY");
                    mPlayButton.setBackgroundResource(R.drawable.stop_circle);
                    mPlayButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_arrow_black, 0, 0, 0);
                }
                mStartPlaying = !mStartPlaying;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRecorder != null) {
            mRecorder.release();
            mRecorder = null;
        }

        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(mFileName);
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
        mRecordButton.setVisibility(View.INVISIBLE);
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        mRecordButton.setVisibility(View.VISIBLE);
        mPlayButton.setVisibility(View.INVISIBLE);
    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(mFileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
        mStopWatchHandler.sendEmptyMessage(MSG_START_TIMER);
        mPlayButton.setVisibility(View.INVISIBLE);
        mRecordButton.startAnimation(mPulseAnimation);
    }

    private void stopRecording() {
        mStopWatchHandler.sendEmptyMessage(MSG_STOP_TIMER);
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mPlayButton.setVisibility(View.VISIBLE);
        mRecordButton.clearAnimation();
        mRecordButton.setText("RECORD");
        mRecordButton.setVisibility(View.INVISIBLE);
    }

    Handler mStopWatchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_START_TIMER:
                    timer.start();
                    mStopWatchHandler.sendEmptyMessage(MSG_UPDATE_TIMER);
                    break;

                case MSG_UPDATE_TIMER:
                    mRecordButton.setText(""+ timer.getTime());
                    mStopWatchHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIMER,REFRESH_RATE);
                    break;

                case MSG_STOP_TIMER:
                    mStopWatchHandler.removeMessages(MSG_UPDATE_TIMER);
                    timer.stop();
                    break;

                default:
                    break;
            }
        }
    };
}
