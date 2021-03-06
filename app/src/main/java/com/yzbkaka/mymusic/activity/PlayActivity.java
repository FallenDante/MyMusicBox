package com.yzbkaka.mymusic.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yzbkaka.mymusic.R;
import com.yzbkaka.mymusic.database.DBManager;
import com.yzbkaka.mymusic.fragment.PlayBarFragment;
import com.yzbkaka.mymusic.receiver.PlayerManagerReceiver;
import com.yzbkaka.mymusic.service.MusicPlayerService;
import com.yzbkaka.mymusic.util.Constant;
import com.yzbkaka.mymusic.util.CustomAttrValueUtil;
import com.yzbkaka.mymusic.util.MyMusicUtil;
import com.yzbkaka.mymusic.view.PlayingPopWindow;

import java.util.Locale;

public class PlayActivity extends BaseActivity implements View.OnClickListener {

    private DBManager dbManager;

    private ImageView backImage;

    private ImageView playImage;

    private ImageView menuImage;

    private ImageView preImage;

    private ImageView nextImage;

    private ImageView modeImage;

    private TextView curTimeText;

    private TextView totalTimeText;

    private TextView musicNameText;

    private TextView singerNameText;

    private SeekBar seekBar;

    private PlayReceiver mReceiver;

    private int mProgress;

    private int duration;

    private int current;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        setStyle();
        dbManager = DBManager.getInstance(PlayActivity.this);
        initView();
        register();
    }


    /**
     * ???????????????
     */
    private void initView() {
        backImage = (ImageView) findViewById(R.id.iv_back);
        playImage = (ImageView) findViewById(R.id.iv_play);
        menuImage = (ImageView) findViewById(R.id.iv_menu);
        preImage = (ImageView) findViewById(R.id.iv_prev);
        nextImage = (ImageView) findViewById(R.id.iv_next);
        modeImage = (ImageView) findViewById(R.id.iv_mode);
        curTimeText = (TextView) findViewById(R.id.tv_current_time);
        totalTimeText = (TextView) findViewById(R.id.tv_total_time);
        musicNameText = (TextView) findViewById(R.id.tv_title);
        singerNameText = (TextView) findViewById(R.id.tv_artist);
        seekBar = (SeekBar) findViewById(R.id.activity_play_seekbar);
        backImage.setOnClickListener(this);
        playImage.setOnClickListener(this);
        menuImage.setOnClickListener(this);
        preImage.setOnClickListener(this);
        nextImage.setOnClickListener(this);
        modeImage.setOnClickListener(this);

        setSeekBarBackground();
        initPlayMode();
        initTitle();
        initPlayImage();

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {  //?????????????????????
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int musicId = MyMusicUtil.getIntSharedPreference(Constant.KEY_ID);
                if (musicId == -1) {
                    Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                    intent.putExtra("cmd", Constant.COMMAND_STOP);
                    sendBroadcast(intent);
                    Toast.makeText(PlayActivity.this, "???????????????", Toast.LENGTH_LONG).show();
                    return;
                }

                //??????????????????
                Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                intent.putExtra("cmd", Constant.COMMAND_PROGRESS);
                intent.putExtra("current", mProgress);
                sendBroadcast(intent);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                initTime();
            }
        });
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back:
                onBackPressed();
                break;
            case R.id.iv_mode:
                switchPlayMode();
                break;
            case R.id.iv_play:  //?????????????????????/??????
                play();
                break;
            case R.id.iv_next:
                MyMusicUtil.playNextMusic(this);
                break;
            case R.id.iv_prev:
                MyMusicUtil.playPreMusic(this);
                break;
            case R.id.iv_menu:
                showPopFormBottom();
                break;
        }
    }


    /**
     * ??????????????????
     */
    private void initPlayImage(){
        int status = PlayerManagerReceiver.status;
        switch (status) {
            case Constant.STATUS_STOP:
                playImage.setSelected(false);
                break;
            case Constant.STATUS_PLAY:
                playImage.setSelected(true);
                break;
            case Constant.STATUS_PAUSE:
                playImage.setSelected(false);
                break;
            case Constant.STATUS_RUN:
                playImage.setSelected(true);
                break;
        }
    }


    /**
     * ??????????????????
     */
    private void initPlayMode() {
        int playMode = MyMusicUtil.getIntSharedPreference(Constant.KEY_MODE);
        if (playMode == -1) {
            playMode = 0;
        }
        modeImage.setImageLevel(playMode);
    }


    /**
     * ????????????
     */
    private void initTitle() {
        int musicId = MyMusicUtil.getIntSharedPreference(Constant.KEY_ID);
        if (musicId == -1) {
            musicNameText.setText("????????????");
            singerNameText.setText("?????????");
        } else {
            musicNameText.setText(dbManager.getMusicInfo(musicId).get(1));
            singerNameText.setText(dbManager.getMusicInfo(musicId).get(2));
        }
    }


    /**
     * ???????????????????????????
     */
    private void initTime() {
        curTimeText.setText(formatTime(current));
        totalTimeText.setText(formatTime(duration));
    }


    private String formatTime(long time) {
        return formatTime("mm:ss", time);
    }


    public static String formatTime(String pattern, long milli) {
        int m = (int) (milli / DateUtils.MINUTE_IN_MILLIS);
        int s = (int) ((milli / DateUtils.SECOND_IN_MILLIS) % 60);
        String mm = String.format(Locale.getDefault(), "%02d", m);
        String ss = String.format(Locale.getDefault(), "%02d", s);
        return pattern.replace("mm", mm).replace("ss", ss);
    }


    private void switchPlayMode() {
        int playMode = MyMusicUtil.getIntSharedPreference(Constant.KEY_MODE);
        switch (playMode) {
            case Constant.PLAYMODE_SEQUENCE:
                MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE, Constant.PLAYMODE_RANDOM);
                break;
            case Constant.PLAYMODE_RANDOM:
                MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE, Constant.PLAYMODE_SINGLE_REPEAT);
                break;
            case Constant.PLAYMODE_SINGLE_REPEAT:
                MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE, Constant.PLAYMODE_SEQUENCE);
                break;
        }
        initPlayMode();
    }


    /**
     * ?????????????????????
     */
    private void setSeekBarBackground(){
        try {
            int progressColor = CustomAttrValueUtil.getAttrColorValue(R.attr.colorPrimary,R.color.colorAccent,this);
            LayerDrawable layerDrawable = (LayerDrawable) seekBar.getProgressDrawable();
            ScaleDrawable scaleDrawable = (ScaleDrawable)layerDrawable.findDrawableByLayerId(android.R.id.progress);
            GradientDrawable drawable = (GradientDrawable) scaleDrawable.getDrawable();
            drawable.setColor(progressColor);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void play() {
        int musicId;
        musicId = MyMusicUtil.getIntSharedPreference(Constant.KEY_ID);
        if (musicId == -1 || musicId == 0) {
            musicId = dbManager.getFirstId(Constant.LIST_ALLMUSIC);
            Intent intent = new Intent(Constant.MP_FILTER);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
            sendBroadcast(intent);
            Toast.makeText(PlayActivity.this, "???????????????", Toast.LENGTH_SHORT).show();
            return;
        }
        if (PlayerManagerReceiver.status == Constant.STATUS_PAUSE) {  //??????-??????
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            sendBroadcast(intent);
        } else if (PlayerManagerReceiver.status == Constant.STATUS_PLAY) {  //??????-??????
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PAUSE);
            sendBroadcast(intent);
        } else {  //??????-??????
            String path = dbManager.getMusicPath(musicId);
            Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
            intent.putExtra(Constant.COMMAND, Constant.COMMAND_PLAY);
            intent.putExtra(Constant.KEY_PATH, path);
            sendBroadcast(intent);
        }
    }


    public void showPopFormBottom() {
        PlayingPopWindow playingPopWindow = new PlayingPopWindow(PlayActivity.this);
        playingPopWindow.showAtLocation(findViewById(R.id.activity_play), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha=0.7f;
        getWindow().setAttributes(params);

        playingPopWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.alpha=1f;
                getWindow().setAttributes(params);
            }
        });
    }


    private void register() {
        mReceiver = new PlayReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlayBarFragment.ACTION_UPDATE_UI_PlAYBAR);
        registerReceiver(mReceiver, intentFilter);
    }


    private void unRegister() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegister();

    }


    class PlayReceiver extends BroadcastReceiver {
        int status;

        /**
         * ???????????????ACTION_UPDATE_UI_PlAYBAR
         */
        @Override
        public void onReceive(Context context, Intent intent) {  //??????PlayBar????????????????????????
            initTitle();
            status = intent.getIntExtra(Constant.STATUS, 0);
            current = intent.getIntExtra(Constant.KEY_CURRENT, 0);
            duration = intent.getIntExtra(Constant.KEY_DURATION, 100);
            switch (status) {  //?????????????????????????????????seekBar
                case Constant.STATUS_STOP:
                    playImage.setSelected(false);
                    break;
                case Constant.STATUS_PLAY:
                    playImage.setSelected(true);
                    break;
                case Constant.STATUS_PAUSE:
                    playImage.setSelected(false);
                    break;
                case Constant.STATUS_RUN:
                    playImage.setSelected(true);
                    seekBar.setMax(duration);
                    seekBar.setProgress(current);
                    break;
                default:
                    break;
            }
        }
    }


    /**
     * ?????????????????????
     */
    private void setStyle() {
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }
}
