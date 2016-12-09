package com.yanling.soundalarm;

import android.app.Application;
import android.speech.SpeechRecognizer;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;

/**
 * 基类application
 * @author yanling
 * @date 2016-12-08
 */
public class BaseApplication extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化即创建语音配置对象
        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=58490d1c");
    }
}
