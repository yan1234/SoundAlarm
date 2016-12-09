package com.yanling.soundalarm;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;


/**
 * 声控闹钟的主界面
 * @author yanling
 * @date 2016-12-08
 */
public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    //闹钟响起时广播action
    private static final String ACTION_ALARM_ALERT = "com.android.deskclock.ALARM_ALERT";
    //闹钟暂停或关闭时广播action
    private static final String ACTION_ALARM_DONE = "com.android.deskclock.ALARM_DONE";
    //关闭闹钟广播action
    private static final String ACTION_STOP_ALARM = "com.android.deskclock.ALARM_DISMISS";
    //暂停闹钟广播action
    private static final String ACTION_SNOOZE_ALARM = "com.android.deskclock.ALARM_SNOOZE";


    private String grammarId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registAlarmReceiver();
        startSpeechReco();
    }

    private SpeechRecognizer mAsr = null;

    private void startSpeechReco(){
        // 在线命令词识别，不启用终端级语法
        // 1.创建SpeechRecognizer对象
        mAsr = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
        mAsr.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        //mAsr.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mAsr.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mAsr.setParameter(SpeechConstant.VAD_BOS, "4000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mAsr.setParameter(SpeechConstant.VAD_EOS, "1000");
        int ret = mAsr.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            Log.e(TAG, "听写失败,错误码：" + ret);
        } else {

        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.e(TAG, "初始化失败，错误码：" + code);
            }
        }
    };

    private com.iflytek.cloud.GrammarListener mGrammarListener = new com.iflytek.cloud.GrammarListener() {
        @Override
        public void onBuildFinish(String s, SpeechError speechError) {
            if (speechError == null){
                if (!TextUtils.isEmpty(s)){
                    //构建语法成功
                    Log.d(TAG, "构建语法成功");
                    grammarId = s;
                    // 2.设置参数
                    mAsr.setParameter(SpeechConstant.ENGINE_TYPE, "cloud");
                    mAsr.setParameter(SpeechConstant.CLOUD_GRAMMAR, grammarId);
                    // 3.开始识别
                    int ret = mAsr.startListening(mRecognizerListener);
                    Log.d(TAG, "" + ret);
                    if (ret != ErrorCode.SUCCESS) {
                        Log.d(TAG,"识别失败,错误码: " + ret);
                    }
                }else{
                    Log.e(TAG, "构建语法失败"+"----" + speechError.getErrorCode());
                }
            }
        }
    };

    /**
     * 识别监听
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int i, byte[] bytes) {
            Log.d(TAG, "音量变化");
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d(TAG, "开始说话");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "结束说话");
        }

        @Override
        public void onResult(RecognizerResult recognizerResult, boolean isLast) {
            Log.d(TAG, recognizerResult.getResultString());
            Log.d(TAG, ""+recognizerResult.describeContents());
        }

        @Override
        public void onError(SpeechError speechError) {
            Log.d(TAG, "错误回调");
            Log.d(TAG, speechError.getErrorCode() + "------"+ speechError.getMessage());
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            Log.d(TAG, "事件回调");
        }
    };

    /**
     * 定义消息回调
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    stopAlarm();
                    break;
                case 1:
                    snoozeAlarm();
                    break;
            }
        }
    };

    /**
     * 注册闹钟监听广播
     */
    private void registAlarmReceiver(){
        AlarmReceiver receiver = new AlarmReceiver();
        IntentFilter filter = new IntentFilter();
        //监听闹钟响起和关闭的广播
        filter.addAction(ACTION_ALARM_ALERT);
        filter.addAction(ACTION_ALARM_DONE);
        registerReceiver(receiver, filter);
    }

    class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_ALARM_ALERT)) {
                Log.i(TAG, "ALARM_ALERT");
                mHandler.sendEmptyMessageDelayed(0,15); //stop alarm
                //mHandler.sendEmptyMessageDelayed(1,15); //snooze alarm
            }else if (intent.getAction().equals(ACTION_ALARM_DONE)) {
                Log.i(TAG,"ALARM_DONE");
            }
        }
    }

    private void stopAlarm() {
        Intent intent = new Intent();
        intent.setAction(ACTION_STOP_ALARM);
        sendBroadcast(intent);
    }

    private void snoozeAlarm() {
        Intent intent = new Intent();
        intent.setAction(ACTION_SNOOZE_ALARM);
        sendBroadcast(intent);
    }
}
