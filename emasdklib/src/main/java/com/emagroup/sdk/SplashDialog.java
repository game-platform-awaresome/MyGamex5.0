package com.emagroup.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class SplashDialog extends Dialog {

    private static final String TAG = "SplashDialog";
    private static final int DISMISS_NOW = 11;
    private static final int DISMISS = 10;
    private static final int ALERT_SHOW = 13;
    private static final int ALERT_WEBVIEW_SHOW = 21;
    private Activity mActivity;
    private ResourceManager mResourceManager;

    private Timer mTimer;

    private long startTime;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DISMISS:
                    dismissDelay(msg.arg1);
                    break;
                case ALERT_SHOW:
                    new EmaAlertDialog(mActivity, SplashDialog.this, (Map) msg.obj, msg.arg1, msg.arg2).show();
                    break;
                case DISMISS_NOW:
                    SplashDialog.this.dismiss();
                    break;
                case ALERT_WEBVIEW_SHOW:
                    new EmaWebviewDialog(mActivity, SplashDialog.this, (Map) msg.obj, msg.arg1, msg.arg2, mHandler).show();
                    break;
            }
        }
    };


    private int necessary;
    private String updateUrl;
    private int version;
    private String maintainBg;
    private String maintainContent;
    private String showStatus;
    private LayoutParams params;


    public SplashDialog(Context context) {
        super(context, ResourceManager.getInstance(context).getIdentifier("ema_dialog", "style"));
        mActivity = (Activity) context;
        mResourceManager = ResourceManager.getInstance(mActivity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        initView();

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
        this.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }

    /**
     * 开始显示闪屏，并在3秒后关闭闪屏
     */
    public void start() {
        if (this.isShowing()) {
            return;
        }
        this.show();
        startTime = System.currentTimeMillis();//记录show开始时间

        //得到appkey相关信息  官方平台不需要
        //getChannelKeyInfo();

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(DISMISS_NOW);
            }
        }, 3000);

        //开始检查维护状态
        checkSDKStatus();
    }

    /**
     * 延长3000-delta ms后关闭
     *
     * @param delayTime
     */
    public void dismissDelay(int delayTime) {
        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(DISMISS_NOW);
            }
        }, 3000 - delayTime);
    }


    /**
     * 检查sdk是否维护状态，并能拿到appkey
     */
    private void checkSDKStatus() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", ConfigManager.getInstance(mActivity).getAppId());
        params.put("channelId", ConfigManager.getInstance(mActivity).getChannel());

        params.put("channelTag", ConfigManager.getInstance(mActivity).getChannelTag());
        params.put("deviceId", DeviceInfoManager.getInstance(mActivity).getDEVICE_ID());
        String sign = ConfigManager.getInstance(mActivity).getAppId() + ConfigManager.getInstance(mActivity).getChannel()
                + ConfigManager.getInstance(mActivity).getChannelTag() + DeviceInfoManager.getInstance(mActivity).getDEVICE_ID()
                + EmaUser.getInstance().getAppKey();

        //String sign =ConfigManager.getInstance(mActivity).getAppId()+ConfigManager.getInstance(mActivity).getChannel()+EmaUser.getInstance().getAppKey();
        //LOG.e("rawSign",sign);
        sign = UCommUtil.MD5(sign);
        params.put("sign", sign);

        new HttpInvoker().postAsync(Url.getSDKStatusUrl(), params, new HttpInvoker.OnResponsetListener() {
            @Override
            public void OnResponse(String result) {
                Message message = Message.obtain();
                try {
                    JSONObject json = new JSONObject(result);
                    int resultCode = json.getInt("status");

                    HashMap<String, String> contentMap = new HashMap<>();

                    switch (resultCode) {
                        case HttpInvokerConst.SDK_RESULT_SUCCESS:// 请求状态成功
                            LOG.d(TAG, "请求状态成功！！");

                            JSONObject dataObj = json.getJSONObject("data");

                            try {
                                JSONObject appVersionInfo = dataObj.getJSONObject("appVersionInfo");
                                necessary = appVersionInfo.getInt("necessary");
                                Log.e("necessary", necessary + "");
                                updateUrl = appVersionInfo.getString("updateUrl");
                                version = appVersionInfo.getInt("version");
                            } catch (Exception e) {
                                LOG.w(TAG, "jiexi appVersionInfo error", e);
                            }

                            try {
                                JSONObject maintainInfo = dataObj.getJSONObject("maintainInfo");
                                maintainBg = maintainInfo.getString("maintainBg");
                                maintainContent = maintainInfo.getString("maintainContent");
                                showStatus = maintainInfo.getString("status");// 0-维护/1-公告
                            } catch (Exception e) {
                                LOG.w(TAG, "jiexi maintainInfo error", e);
                            }

                            try {
                                //将得到的menubar信息存sp，在toolbar那边取
                                String menuBarInfo = dataObj.getString("menuBarInfo");
                                USharedPerUtil.setParam(mActivity, "menuBarInfo", menuBarInfo);

                                try {
                                    //三个三方登录是否显示
                                    Ema.getInstance().saveQQLoginVisibility(new JSONObject(menuBarInfo).getInt("support_qq_login"));
                                    Ema.getInstance().saveWeboLoginVisibility(new JSONObject(menuBarInfo).getInt("support_weibo_login"));
                                    Ema.getInstance().saveWachatLoginVisibility(new JSONObject(menuBarInfo).getInt("support_weixin_login"));
                                } catch (Exception e) {
                                    Log.e("third login", "json parse failed");
                                    e.printStackTrace();
                                }
                                try {
                                    //记录微信qq支付是否支持
                                    USharedPerUtil.setParam(mActivity, EmaConst.SUPPORT_QQ_PAY, new JSONObject(menuBarInfo).getInt("support_qq_pay"));
                                    USharedPerUtil.setParam(mActivity, EmaConst.SUPPORT_WX_PAY, new JSONObject(menuBarInfo).getInt("support_weixin_pay"));
                                } catch (Exception e) {
                                    Log.e("third pay", "json parse failed");
                                    e.printStackTrace();
                                }
                                try {
                                    //记录实名认证逻辑 0-不开启实名认证/1-开启实名认证但不强制/2-强制实名认证
                                    USharedPerUtil.setParam(mActivity, EmaConst.IDENTIFY_LV, new JSONObject(menuBarInfo).getInt("identify_lv"));
                                } catch (Exception e) {
                                    Log.e("identify_lv", "json parse failed");
                                    e.printStackTrace();
                                }

                            } catch (Exception e) {
                                USharedPerUtil.setParam(mActivity, "menuBarInfo", "");
                                LOG.w(TAG, "jiexi menuBarInfo error", e);
                            }

                            contentMap.put("updateUrl", updateUrl);
                            contentMap.put("maintainContent", maintainContent);
                            contentMap.put("whichUpdate", "none");

                            if (TextUtils.isEmpty(showStatus)) {

                                if (!TextUtils.isEmpty(updateUrl)) {
                                    HashMap<String, String> updateMap = new HashMap<>();
                                    updateMap.put("updateUrl", updateUrl);

                                    if (ConfigManager.getInstance(mActivity).getVersionCode(mActivity) < version) { // 需要更新
                                        Log.e("gengxin", ConfigManager.getInstance(mActivity).getVersionCode(mActivity) + "..." + version);
                                        if (1 == necessary) {  //necessary 1强更
                                            message.arg1 = 1;  //arg1是显示类型，1的话就是只显示确定按钮
                                            message.arg2 = 2;
                                        } else {
                                            message.arg1 = 2;
                                            message.arg2 = 1;
                                        }
                                        message.what = ALERT_SHOW;
                                        message.obj = updateMap;        //内容
                                        mHandler.sendMessage(message);
                                    }
                                }

                            } else if ("1".equals(showStatus)) { //显示公告

                                message.what = ALERT_WEBVIEW_SHOW;
                                message.arg1 = 1;               //显示形式 1只有确定按钮
                                message.arg2 = 2;                    //------2确定按钮按下顺利进  3有更新，有后续dialog
                                message.obj = contentMap; //内容

                                if (!TextUtils.isEmpty(updateUrl)) {
                                    if (ConfigManager.getInstance(mActivity).getVersionCode(mActivity) < version) { // 需要更新
                                        Log.e("gengxin", ConfigManager.getInstance(mActivity).getVersionCode(mActivity) + "..." + version);
                                        if (1 == necessary) {  //necessary 1强更
                                            contentMap.put("whichUpdate", "hard");
                                        } else {
                                            contentMap.put("whichUpdate", "soft");
                                        }
                                    }
                                } else {
                                    contentMap.put("whichUpdate", "none");
                                }
                                mHandler.sendMessage(message);

                            } else if ("0".equals(showStatus)) { //维护状态
                                message.what = ALERT_WEBVIEW_SHOW;
                                message.arg1 = 1;               //显示形式 1只有确定按钮
                                message.arg2 = 1;                    //-------1确定按钮按下退出
                                message.obj = contentMap; //内容
                                mHandler.sendMessage(message);
                            }

                            Ema.getInstance().makeCallBack(EmaCallBackConst.INITSUCCESS, "初始化完成"); //一连串走完了到这里
                            break;
                        default:
                            LOG.e(TAG, "请求状态失败！！" + json.getString("message"));
                            //ToastHelper.toast(mActivity,json.getString("message"));
                            Ema.getInstance().makeCallBack(EmaCallBackConst.INITFALIED, "初始化失败!!");
                            break;
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "sdk status error", e);
                    Ema.getInstance().makeCallBack(EmaCallBackConst.INITFALIED, "初始化失败!!"); //一连串走完了到这里
                }
            }
        });
    }

    /**
     * 根据chennelid和appid获取各渠道的key信息
     */
    private void getChannelKeyInfo() {
        Map<String, String> params = new HashMap<>();
        params.put("appId", ConfigManager.getInstance(mActivity).getAppId());
        params.put("channelId", ConfigManager.getInstance(mActivity).getChannel());
        new HttpInvoker().postAsync(Url.getChannelKayInfo(), params, new HttpInvoker.OnResponsetListener() {
            @Override
            public void OnResponse(String result) {
                /*//记录时间差
				long endTime = System.currentTimeMillis();
				long deltaTime = endTime - startTime;
				Message message = Message.obtain();*/
                try {
                    JSONObject json = new JSONObject(result);
                    int resultCode = json.getInt("status");

                    switch (resultCode) {
                        case HttpInvokerConst.SDK_RESULT_SUCCESS:// 请求成功
                            LOG.d(TAG, "请求成功！！");
                            JSONObject data = json.getJSONObject("data");
                            String appKey = data.getString("channelAppKey");
                            EmaUser.getInstance().setAppKey(appKey);
							/*message.what=DISMISS;
							message.arg1= deltaTime>3000? 3000: (int) deltaTime;
							mHandler.sendMessage(message);*/
                            break;
                        case HttpInvokerConst.SDK_RESULT_FAILED:
                            LOG.e(TAG, "请求失败！！");
                            break;
                        default:
                            LOG.d(TAG, json.getString("message"));
                            ToastHelper.toast(mActivity, json.getString("message"));
                            break;
                    }
                } catch (Exception e) {
                    LOG.w(TAG, "sdk status error", e);
                }
            }
        });
    }

    /**
     * 初始化界面
     */
    private void initView() {
        int type = mActivity.getResources().getConfiguration().orientation;
        View view = mResourceManager.getLayout("ema_splash");
        //view.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        int drawableId = 0;
        ImageView imageView = (ImageView) view.findViewById(mResourceManager.getIdentifier("ema_splash_imageview", "id"));

        if (type == Configuration.ORIENTATION_LANDSCAPE) {
            LOG.d(TAG, "landscape");
            //drawableId = mResourceManager.getIdentifier("ema_init_bg", "drawable");
            drawableId = mResourceManager.getIdentifier("ema_splash_logo", "drawable");
            view.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        } else if (type == Configuration.ORIENTATION_PORTRAIT) {
            LOG.d(TAG, "portrait");
            //drawableId = mResourceManager.getIdentifier("ema_init_bg_vertical", "drawable");
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
            drawableId = mResourceManager.getIdentifier("ema_splash_logo", "drawable");
            //	imageView.setPadding(0,0,0,500);
        }

        imageView.setImageResource(drawableId);
        this.setContentView(view);
        this.setCancelable(false);
    }

}
