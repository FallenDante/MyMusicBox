package com.yzbkaka.mymusic.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yzbkaka.mymusic.R;
import com.yzbkaka.mymusic.adapter.RecyclerViewAdapter;
import com.yzbkaka.mymusic.database.DBManager;
import com.yzbkaka.mymusic.entity.MusicInfo;
import com.yzbkaka.mymusic.receiver.PlayerManagerReceiver;
import com.yzbkaka.mymusic.service.MusicPlayerService;
import com.yzbkaka.mymusic.util.Constant;
import com.yzbkaka.mymusic.util.MyMusicUtil;
import com.yzbkaka.mymusic.view.MusicPopMenuWindow;
import com.yzbkaka.mymusic.view.SideBar;
import com.mcxtzhang.swipemenulib.SwipeMenuLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class LastMyloveActivity extends PlayBarBaseActivity {

    private Toolbar toolbar;

    private RelativeLayout playModeLayout;

    private ImageView playModeImage;

    private TextView playModeText;

    private RecyclerView recyclerView;

    private SideBar sideBar;

    public  RecyclerViewAdapter recyclerViewAdapter;

    private List<MusicInfo> musicInfoList = new ArrayList<>();

    private DBManager dbManager;

    private String label;

    private UpdateReceiver mReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_mylove);
        dbManager = DBManager.getInstance(LastMyloveActivity.this);
        label = getIntent().getStringExtra(Constant.LABEL);
        toolbar = (Toolbar)findViewById(R.id.last_mylove_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (label != null){
                actionBar.setTitle(label);
            }
        }
        init();
        register();
    }


    private void init(){
        recyclerView = (RecyclerView)findViewById(R.id.last_mylove_recycler_view);
        recyclerViewAdapter = new RecyclerViewAdapter(LastMyloveActivity.this, musicInfoList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(LastMyloveActivity.this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
       // recyclerView.addItemDecoration(new DividerItemDecoration(LastMyloveActivity.this, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setAdapter(recyclerViewAdapter);

        //????????????
        recyclerViewAdapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onOpenMenuClick(int position) {
                MusicInfo musicInfo = musicInfoList.get(position);
                showPopFormBottom(musicInfo);
            }

            @Override
            public void onDeleteMenuClick(View swipeView, int position) {
                deleteOperate(swipeView,position,LastMyloveActivity.this);
            }

            @Override
            public void onContentClick(int position) {
                if (label != null){
                    if (label.equals(Constant.LABEL_LAST)){
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_LIST,Constant.LIST_LASTPLAY);
                    }else if (label.equals(Constant.LABEL_MYLOVE)){
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_LIST,Constant.LIST_MYLOVE);
                    }
                }
            }
        });

        // ???????????????????????????????????????????????????????????????
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    SwipeMenuLayout viewCache = SwipeMenuLayout.getViewCache();
                    if (null != viewCache) {
                        viewCache.smoothClose();
                    }
                }
                return false;
            }
        });

        sideBar = (SideBar) findViewById(R.id.last_mylove_music_siderbar);
        sideBar.setOnListener(new SideBar.OnTouchingLetterChangedListener() {
            @Override
            public void onTouchingLetterChanged(String letter) {
                //??????????????????????????????
                int position = recyclerViewAdapter.getPositionForSection(letter.charAt(0));
                if (position != -1) {
                    recyclerView.smoothScrollToPosition(position);
                }
            }
        });

        playModeLayout = (RelativeLayout)findViewById(R.id.last_mylove_playmode_rl);
        playModeImage = (ImageView)findViewById(R.id.last_mylove_playmode_iv);
        playModeText = (TextView)findViewById(R.id.last_mylove_playmode_tv);

        initDefaultPlayModeView();

        //  ?????? --> ??????-- > ??????
        playModeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int playMode = MyMusicUtil.getIntSharedPreference(Constant.KEY_MODE);
                switch (playMode){
                    case Constant.PLAYMODE_SEQUENCE:
                        playModeText.setText(Constant.PLAYMODE_RANDOM_TEXT);
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE,Constant.PLAYMODE_RANDOM);
                        break;
                    case Constant.PLAYMODE_RANDOM:
                        playModeText.setText(Constant.PLAYMODE_SINGLE_REPEAT_TEXT);
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE,Constant.PLAYMODE_SINGLE_REPEAT);
                        break;
                    case Constant.PLAYMODE_SINGLE_REPEAT:
                        playModeText.setText(Constant.PLAYMODE_SEQUENCE_TEXT);
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_MODE,Constant.PLAYMODE_SEQUENCE);
                        break;
                }
                initPlayMode();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        updateView();
        initDefaultPlayModeView();
    }


    private void initDefaultPlayModeView(){
        int playMode = MyMusicUtil.getIntSharedPreference(Constant.KEY_MODE);
        switch (playMode){
            case Constant.PLAYMODE_SEQUENCE:
                playModeText.setText(Constant.PLAYMODE_SEQUENCE_TEXT);
                break;
            case Constant.PLAYMODE_RANDOM:
                playModeText.setText(Constant.PLAYMODE_RANDOM_TEXT);
                break;
            case Constant.PLAYMODE_SINGLE_REPEAT:
                playModeText.setText(Constant.PLAYMODE_SINGLE_REPEAT_TEXT);
                break;
        }
        initPlayMode();
    }


    private void initPlayMode() {
        int playMode = MyMusicUtil.getIntSharedPreference(Constant.KEY_MODE);
        if (playMode == -1) {
            playMode = 0;
        }
        playModeImage.setImageLevel(playMode);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    /**
     * ??????
     */
    public void updateView(){
        try {
            if (label != null) {
                if (label.equals(Constant.LABEL_LAST)) {  //????????????
                    musicInfoList = dbManager.getAllMusicFromTable(Constant.LIST_LASTPLAY);
                } else if (label.equals(Constant.LABEL_MYLOVE)) {  //????????????
                    musicInfoList = dbManager.getAllMusicFromTable(Constant.LIST_MYLOVE);
                    Collections.sort(musicInfoList);
                }
            }
            recyclerViewAdapter.updateMusicInfoList(musicInfoList);

            if (musicInfoList.size() == 0) {
                sideBar.setVisibility(View.GONE);
                playModeLayout.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            } else {
                sideBar.setVisibility(View.VISIBLE);
                playModeLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    public void showPopFormBottom(MusicInfo musicInfo) {
        MusicPopMenuWindow menuPopupWindow;
        if (label.equals(Constant.LABEL_LAST)){
            menuPopupWindow = new MusicPopMenuWindow(LastMyloveActivity.this,musicInfo,findViewById(R.id.activity_last_mylove),Constant.ACTIVITY_RECENTPLAY);
        }else {
            menuPopupWindow = new MusicPopMenuWindow(LastMyloveActivity.this,musicInfo,findViewById(R.id.activity_last_mylove),Constant.ACTIVITY_MYLOVE);
        }

//      ??????Popupwindow?????????????????????????????????
        menuPopupWindow.showAtLocation(findViewById(R.id.activity_last_mylove), Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        //?????????Popupwindow????????????????????????
        params.alpha=0.7f;
        getWindow().setAttributes(params);

        //??????Popupwindow??????????????????Popupwindow?????????????????????1f
        menuPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                WindowManager.LayoutParams params = getWindow().getAttributes();
                params.alpha=1f;
                getWindow().setAttributes(params);
            }
        });

        menuPopupWindow.setOnDeleteUpdateListener(new MusicPopMenuWindow.OnDeleteUpdateListener() {
            @Override
            public void onDeleteUpdate() {
                updateView();
            }
        });

    }


    public void deleteOperate(final View swipeView,final int position,final Context context){
        final MusicInfo musicInfo = musicInfoList.get(position);
        final int deleteMusicId = musicInfo.getId();
        final int musicId = MyMusicUtil.getIntSharedPreference(Constant.KEY_ID);
        final DBManager dbManager = DBManager.getInstance(context);
        final String path = dbManager.getMusicPath(deleteMusicId);
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_delete_file,null);
        final CheckBox deleteFile = (CheckBox)view.findViewById(R.id.dialog_delete_cb);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(view);

        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                update(swipeView,position,musicInfo,true);
                if (deleteFile.isChecked()){
                    //??????????????????
                    File file = new File(path);
                    if (file.exists()) {
                        MusicPopMenuWindow.deleteMediaDB(file,context);
                        dbManager.deleteMusic(deleteMusicId);
                    }else {
                        Toast.makeText(context,"???????????????",Toast.LENGTH_SHORT).show();
                    }
                    if (deleteMusicId == musicId){  //?????????????????????????????????
                        Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                        intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
                        context.sendBroadcast(intent);
                        MyMusicUtil.setIntSharedPreference(Constant.KEY_ID,dbManager.getFirstId(Constant.LIST_ALLMUSIC));
                    }
                }
                dialog.dismiss();

            }
        });
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }


    private void update(View swipeView, int position,MusicInfo musicInfo,boolean isDelete){
        if (isDelete){
            final int deleteMusicId = musicInfo.getId();
            final int musicId = MyMusicUtil.getIntSharedPreference(Constant.KEY_ID);
            //???????????????
            if (label.equals(Constant.LABEL_LAST)){
                dbManager.removeMusic(musicInfo.getId(),Constant.ACTIVITY_RECENTPLAY);
            }else if (label.equals(Constant.ACTIVITY_MYLOVE)){
                dbManager.removeMusic(musicInfo.getId(),Constant.LIST_LASTPLAY);
            }
            if (deleteMusicId == musicId) {  //?????????????????????????????????
                Intent intent = new Intent(MusicPlayerService.PLAYER_MANAGER_ACTION);
                intent.putExtra(Constant.COMMAND, Constant.COMMAND_STOP);
                sendBroadcast(intent);
            }
            recyclerViewAdapter.notifyItemRemoved(position);//???????????????
            updateView();
        }
        //???????????????????????????mAdapter.notifyItemRemoved(pos)?????????????????????????????????
        //???????????????????????????????????????????????????????????? ((CstSwipeDelMenu) holder.itemView).quickClose();
        ((SwipeMenuLayout) swipeView).quickClose();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unRegister();
    }


    private void register() {
        try {
            if (mReceiver != null) {
                this.unRegister();
            }
            mReceiver = new UpdateReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PlayerManagerReceiver.ACTION_UPDATE_UI_ADAPTER);
            this.registerReceiver(mReceiver, intentFilter);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private void unRegister() {
        try {
            if (mReceiver != null) {
                this.unregisterReceiver(mReceiver);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }


    private class UpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            recyclerViewAdapter.notifyDataSetChanged();
        }
    }
}
