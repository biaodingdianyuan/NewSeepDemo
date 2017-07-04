package come.example.liuhaifeng.newseepdemo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener {
    @InjectView(R.id.start_ly)
    Button startLy;
    @InjectView(R.id.music_yiny)
    Button musicYiny;
    @InjectView(R.id.btn_clean)
    Button btnClean;
    private String mEngineType = SpeechConstant.TYPE_CLOUD;
    @InjectView(R.id.music_to_word)
    TextView musicToWord;
    @InjectView(R.id.myseekbar)
    Myseekbar myseekbar;
    @InjectView(R.id.music_start)
    Button musicStart;
    String lag = "mandarin";
    @InjectView(R.id.music_yy)
    Button musicYy;
    private String voicer;
    ApkInstaller mInstaller;
    private ContentObserver mVoiceObserver;
    public AudioManager audiomanage;
    private SpeechRecognizer mIat;
    private int maxVolume, currentVolume;
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    int ret = 0;
    String s = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        SpeechUtility.createUtility(MainActivity.this, SpeechConstant.APPID + "=5949d643");
        mIat = SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);
        ButterKnife.inject(this);
        audiomanage = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        btnClean.setOnClickListener(this);
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        myseekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
        myRegisterReceiver();
        mVoiceObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                // TODO Auto-generated method stub
                super.onChange(selfChange);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                myseekbar.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM));
            }
        };
        myseekbar.setOnSeekBarChangeListener(this);
        maxVolume = audiomanage.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        myseekbar.setMax(maxVolume);
        currentVolume = audiomanage.getStreamVolume(AudioManager.STREAM_MUSIC);
        myseekbar.setProgress(currentVolume);
        mInstaller = new ApkInstaller(MainActivity.this);
        musicStart.setEnabled(false);
        musicYiny.setEnabled(true);
        musicYy.setEnabled(true);
    }


    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d("", "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
            } else {
            }
        }
    };


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        audiomanage.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
        currentVolume = audiomanage.getStreamVolume(AudioManager.STREAM_MUSIC);  //获取当前值
        myseekbar.setProgress(currentVolume);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    private void myRegisterReceiver() {
        MyVolumeReceiver mVolumeReceiver = new MyVolumeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        MainActivity.this.registerReceiver(mVolumeReceiver, filter);
    }

    @OnClick(R.id.start_ly)
    public void onViewClicked() {
        if (startLy.getText().equals("开始")) {
            FlowerCollector.onEvent(MainActivity.this, "iat_recognize");
            mIatResults.clear();

            ret = mIat.startListening(mRecognizerListener);

            startLy.setText("停止");
        } else {
            mIat.stopListening();
            startLy.setText("开始");
        }

    }

    @OnClick({R.id.music_start, R.id.music_yiny, R.id.music_yy})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.music_start:
                lag = "mandarin";
                musicStart.setEnabled(false);
                musicYiny.setEnabled(true);
                musicYy.setEnabled(true);
                break;
            case R.id.music_yiny:
                lag = "lmz";
                musicStart.setEnabled(true);
                musicYiny.setEnabled(false);
                musicYy.setEnabled(true);
                break;
            case R.id.music_yy:
                lag = "cantonese";
                musicStart.setEnabled(true);
                musicYiny.setEnabled(true);
                musicYy.setEnabled(false);
                break;
        }
    }//lmz 四川话

    @Override
    public void onClick(View v) {
        musicToWord.setText("");
        s = "";
    }

    /**
     * 处理音量变化时的界面显示
     *
     * @author long
     */
    private class MyVolumeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //如果音量发生变化则更改seekbar的位置
            if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                int currVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);// 当前的媒体音量
                myseekbar.setProgress(currVolume);
            }
        }
    }

    /**
     * 参数设置
     *
     * @param
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");


        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);
        }

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, "10000");

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, "5000");

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, "1");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d("**", "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
            }
        }
    };
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {

        }

        @Override
        public void onError(SpeechError error) {

            startLy.setText("开始");

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
            mIat.startListening(mRecognizerListener);

        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            myseekbar.setProgress(volume);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        s = s + "," + resultBuffer.toString();
        musicToWord.setText(s.substring(1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mIat) {
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(MainActivity.this);
        FlowerCollector.onPageStart(MainActivity.class.getSimpleName());
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析getSimpleName
        FlowerCollector.onPageEnd(MainActivity.class.getSimpleName());
        FlowerCollector.onPause(MainActivity.this);
        super.onPause();
    }
}
