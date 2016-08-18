package cn.emagroup.sdk.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import cn.emagroup.sdk.Ema;
import cn.emagroup.sdk.comm.ConfigManager;
import cn.emagroup.sdk.comm.DeviceInfoManager;
import cn.emagroup.sdk.comm.EmaCallBackConst;
import cn.emagroup.sdk.comm.EmaProgressDialog;
import cn.emagroup.sdk.comm.HttpInvoker;
import cn.emagroup.sdk.comm.HttpInvokerConst;
import cn.emagroup.sdk.comm.ResourceManager;
import cn.emagroup.sdk.comm.Url;
import cn.emagroup.sdk.ui.ToolBar;
import cn.emagroup.sdk.utils.LOG;
import cn.emagroup.sdk.utils.ToastHelper;
import cn.emagroup.sdk.utils.UCommUtil;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

public class RegisterByPhoneDialog extends Dialog implements android.view.View.OnClickListener{

	private static final String TAG = "RegisterByPhoneDialog";
	
	private static final int CODE_GET_AUTH_CODE_SUCCESS = 100;//成功
	private static final int CODE_GET_AUTH_CODE_FAILED = 101;//失败，但原因不明
	private static final int CODE_FAILED_TOO_OFFTEN = 102;//失败，由于操作频繁
	private static final int CODE_FAILED_OVER_MAX = 103;//失败，由于发送量达到上限
	private static final int CODE_SIGN_ERROR = 104;//失败，签名验证失败
	
	private static final int CODE_LOGIN_SUCC = 300;
	private static final int CODE_LOGIN_FAILED = 301;
	private static final int CODE_LOGIN_FAILED_ERROR_AUTH_CODE = 302;
	
	private static final int CODE_TIMER = 400;//发送定时器计数消息
	
	private Activity mActivity;
	private ResourceManager mResourceManager;// 资源管理
	private DeviceInfoManager mDeviceInfoManager;// 设备信息管理
	private ConfigManager mConfigManager;// 配置项管理
	private EmaUser mEmaUser;// 当前登录用户信息
	
	private int mCountNum;//秒数
	private Timer mTimer;
	private TimerTask mTask;
	
	private LoginSuccDialog mLoginSuccDialog;// 登录成功后显示的对话框
	
	//Views
	private Button mBtnStartWork;//用于获取验证码 和  登录
	private Button mBtnReturnLogin;//返回普通登录界面
	private Button mBtnReturnRegister;//返回普通注册界面
	private Button mBtnGetAuthCode;//获取验证码
	private EditText mEdtContentView;//输入手机号码 或者 验证码
	
	private Map<String, Integer> mIDmap;
	
	//标记
	private boolean mFlagHasGetAuthCode;//标记当前状态是获取验证码之前(false)，还是获取了验证码之后(true)
	// 进度条
	private EmaProgressDialog mProgress;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case CODE_GET_AUTH_CODE_SUCCESS://成功
				ToastHelper.toast(mActivity, "请在手机上查收验证码！");
				setViewChange();
				startTimeTask();
				break;
			case CODE_SIGN_ERROR:
				ToastHelper.toast(mActivity, "签名验证失败");
				break;
			case CODE_GET_AUTH_CODE_FAILED://失败，但原因不明
				ToastHelper.toast(mActivity, "请求失败，请检查手机号！");
				break;
			case CODE_FAILED_TOO_OFFTEN://失败，由于操作频繁
				ToastHelper.toast(mActivity, "请求过于频繁，请稍后再试");
				break;
			case CODE_FAILED_OVER_MAX://失败，由于发送量达到上限
				ToastHelper.toast(mActivity, "今日请求数量已达上限");
				break;
			case EmaProgressDialog.CODE_LOADING_START://开始显示进度条
				mProgress.showProgress((String)msg.obj);
				break;
			case EmaProgressDialog.CODE_LOADING_END://关闭进度条
				mProgress.closeProgress();
				break;
			case CODE_TIMER://发送定时器消息，刷新定时器
				updateTimeTask();
				break;
			case CODE_LOGIN_SUCC://登录成功
				ToastHelper.toast(mActivity, "登录成功");
				RegisterByPhoneDialog.this.stopTimeTask();
				RegisterByPhoneDialog.this.dismiss();
				UCommUtil.makeUserCallBack(EmaCallBackConst.REGISTERSUCCESS, "注册成功");
				// 保存登录成功用户的信息
				mEmaUser.saveLoginUserInfo(mActivity);
				mLoginSuccDialog = new LoginSuccDialog(mActivity, false);
				mLoginSuccDialog.start();
				break;
			case CODE_LOGIN_FAILED://登录失败，原因未知
				ToastHelper.toast(mActivity, "登录失败");
				break;
			case CODE_LOGIN_FAILED_ERROR_AUTH_CODE://登录失败，原因 验证码错误
				ToastHelper.toast(mActivity, "验证码错误，请重新输入");
				break;
			}
		};
	};
	
	public RegisterByPhoneDialog(Context context) {
		super(context);
		mActivity = (Activity) context;
		mResourceManager = ResourceManager.getInstance(mActivity);
		mDeviceInfoManager = DeviceInfoManager.getInstance(mActivity);
		mConfigManager = ConfigManager.getInstance(mActivity);
		mEmaUser = EmaUser.getInstance();
		mProgress = new EmaProgressDialog(mActivity);
		mFlagHasGetAuthCode = false;
		mTimer = null;
		mTask = null;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		this.setCanceledOnTouchOutside(false);
		
		
		initView();
	}

	/**
	 * 初始化界面
	 */
	private void initView() {
		setContentView(mResourceManager.getIdentifier("ema_register_by_phone", "layout"));
		mBtnStartWork = (Button) findViewById(getId("ema_btn_start_work"));
		mBtnReturnLogin = (Button) findViewById(getId("ema_btn_return_login"));
		mBtnReturnRegister = (Button) findViewById(getId("ema_btn_return_register"));
		mBtnGetAuthCode = (Button) findViewById(getId("ema_btn_get_auth_code"));
		mEdtContentView = (EditText) findViewById(getId("ema_phone_info_inputText"));
		mBtnGetAuthCode.setVisibility(View.GONE);
		
		mBtnStartWork.setOnClickListener(this);
		mBtnReturnLogin.setOnClickListener(this);
		mBtnReturnRegister.setOnClickListener(this);
		mBtnGetAuthCode.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == getId("ema_btn_start_work")){//获取验证码，或者进入游戏
			doStartWork();
		}else if(id == getId("ema_btn_return_login")){//账号登录
			this.dismiss();
			new LoginDialog(Ema.getInstance().getContext()).show();
		}else if(id == getId("ema_btn_return_register")){//快速注册
			this.dismiss();
			new RegisterDialog(Ema.getInstance().getContext()).show();
		}else if(id == getId("ema_btn_get_auth_code")){//重新获取验证码
			startTimeTask();
			doGetAuthCode(mEmaUser.getPhoneNum());
		}
	}
	
	/**
	 * 获取验证码 / 进入游戏
	 */
	private void doStartWork(){
		if(mFlagHasGetAuthCode){//获取验证码之后，进行的是登录操作
			doLogin();
		}else{//还没有获取验证码，进行获取验证码操作
			String phone = mEdtContentView.getText().toString();	
			if(UserUtil.checkPhoneInputIsOk(mActivity, phone)){
				doGetAuthCode(phone);
			}
		}
	}
	
	/**
	 * 获取验证码
	 */
	private void doGetAuthCode(String phoneNum) {
		mProgress.showProgress("获取验证码");
		//设置用户的电话号码信息
		mEmaUser.setPhoneNum(phoneNum);
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("uuid", phoneNum);
		params.put("phone", phoneNum);
		params.put("app_id", mConfigManager.getAppId());
		params.put("action", "mobile_login");
		long stamp = (int) (System.currentTimeMillis() / 1000);
		stamp = stamp - stamp % 600;
		String sign = mConfigManager.getAppId() + stamp
				+ mConfigManager.getAppKEY();
		sign = UCommUtil.MD5(sign);
		params.put("sign", sign);
		
		UCommUtil.testMapInfo(params);

		new HttpInvoker().postAsync(Url.getSendPhoneCodeUrl(), params,
				new HttpInvoker.OnResponsetListener() {
					@Override
					public void OnResponse(String result) {
						mHandler.sendEmptyMessage(EmaProgressDialog.CODE_LOADING_END);
						try {
							JSONObject json = new JSONObject(result);
							int resultCode = json
									.getInt(HttpInvokerConst.RESULT_CODE);
							switch (resultCode) {
							case HttpInvokerConst.SDK_RESULT_SUCCESS:// 成功
								mHandler.sendEmptyMessage(CODE_GET_AUTH_CODE_SUCCESS);
								break;
							case HttpInvokerConst.SEND_PHONE_CODE_FAILED://获取验证码失败
								LOG.d(TAG, "获取验证码失败");
								mHandler.sendEmptyMessage(CODE_GET_AUTH_CODE_FAILED);
								break;
							case HttpInvokerConst.SEND_PHONE_CODE_FAILED_TOO_OFFTEN:// 操作过于频繁
								LOG.d(TAG, "请求验证过于频繁，获取验证码失败");
								mHandler.sendEmptyMessage(CODE_FAILED_TOO_OFFTEN);
								break;
							case HttpInvokerConst.SEND_PHONE_CODE_FAILED_OVER_MAX:// 发送量达到今日最大限量
							case HttpInvokerConst.SEND_PHONE_CODE_FAILED_OVER_MAX_1:
								LOG.d(TAG, "请求验证次数达到上限，获取验证码失败");
								mHandler.sendEmptyMessage(CODE_FAILED_OVER_MAX);
								break;
							case HttpInvokerConst.SDK_RESULT_FAILED_SIGIN_ERROR:
								LOG.d(TAG, "签名失败");
								mHandler.sendEmptyMessage(CODE_SIGN_ERROR);
								break;
							default:
								LOG.d(TAG, "请求验证码失败");
								mHandler.sendEmptyMessage(CODE_GET_AUTH_CODE_FAILED);
								break;
							}
						} catch (Exception e) {
							LOG.w(TAG, "doGetAuthCode error", e);
							mHandler.sendEmptyMessage(CODE_GET_AUTH_CODE_FAILED);
						}
					}
				});
	}
	
	/**
	 * 登录
	 */
	private void doLogin(){
		String authCode = mEdtContentView.getText().toString();
		if(UCommUtil.isStrEmpty(authCode)){
			LOG.d(TAG, "验证码为空");
			ToastHelper.toast(mActivity, "验证码不能为空");
			return;
		}
		mProgress.showProgress("登录中...");
		Map<String, String> params = new HashMap<String, String>();
		params.put("loginname", mEmaUser.getPhoneNum());
		params.put("code", authCode);
		params.put("channel", mConfigManager.getChannel());
		params.put("device_id", mDeviceInfoManager.getDEVICE_ID());
		params.put("app_id", mConfigManager.getAppId());
		
		new HttpInvoker().postAsync(Url.getLoginUrlByPhone(), params, new HttpInvoker.OnResponsetListener() {
			@Override
			public void OnResponse(String result) {
				mHandler.sendEmptyMessage(EmaProgressDialog.CODE_LOADING_END);
				try {
					JSONObject json = new JSONObject(result);
					int resultCode = json.getInt(HttpInvokerConst.RESULT_CODE);
					switch(resultCode){
					case HttpInvokerConst.SDK_RESULT_SUCCESS://登录成功
						mEmaUser.setCode(json.getString("code"));
						mEmaUser.setUserName(mEmaUser.getPhoneNum());
						mEmaUser.setUUID(json.getString("uuid"));
						mEmaUser.setSid(json.getString("sid"));
						mEmaUser.setIsAnlaiye(false);
						mHandler.sendEmptyMessage(CODE_LOGIN_SUCC);
						LOG.d(TAG, "手机注册登录成功");
						break;
					case HttpInvokerConst.LOGIN_PHONE_LOGIN_AUTH_CODE_ERROR://验证码错误
						LOG.w(TAG, "验证码错误，登录失败");
						mHandler.sendEmptyMessage(CODE_LOGIN_FAILED_ERROR_AUTH_CODE);
						break;
					default:
						LOG.w(TAG, "手机注册登录失败");
						mHandler.sendEmptyMessage(CODE_LOGIN_FAILED);
						break;
					}
				} catch (Exception e) {
					mHandler.sendEmptyMessage(CODE_LOGIN_FAILED);
					LOG.w(TAG, "doLogin error", e);
				}
			}
		});
		
	}
	
	/**
	 * 刷新定时器
	 */
	private void updateTimeTask(){
		mCountNum--;
		if(0 < mCountNum){
			mBtnGetAuthCode.setText("重新获取(" + mCountNum + ")");
		}else{
			mBtnGetAuthCode.setText("重新获取");
			stopTimeTask();
		}
	}
	
	/**
	 * 开启定时器
	 */
	private void startTimeTask(){
		mCountNum = 60;
		mBtnGetAuthCode.setEnabled(false);
		if(mTimer == null){
			mTimer = new Timer();
		}
		if(mTask != null){
			mTask.cancel();
		}
		mTask = new TimerTask() {
			@Override
			public void run() {
				mHandler.sendEmptyMessage(CODE_TIMER);
			}
		};
		mTimer.schedule(mTask, 1000, 1000);
	}
	
	/**
	 * 暂停定时器
	 */
	public void stopTimeTask(){
		mBtnGetAuthCode.setEnabled(true);
		if(mTimer != null){
			mTimer.cancel();
			mTimer = null;
		}
		if(mTask != null){
			mTask.cancel();
			mTask = null;
		}
	}
	
	/**
	 * 控制界面的转化（在获取验证码之后  转化为 输入验证码进行登录的界面）
	 */
	private void setViewChange(){
		mEdtContentView.setText("");
		mEdtContentView.setHint("请输入验证码");
		mBtnGetAuthCode.setVisibility(View.VISIBLE);
		mFlagHasGetAuthCode = true;
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		EmaUser.getInstance().clearUserInfo();
		UCommUtil.makeUserCallBack(EmaCallBackConst.LOGINCANELL, "取消登录");
		ToolBar.getInstance(mActivity).showToolBar();
	}
	
	/**
	 * 方法目的（为了防止重复去获取资源ID） 
	 * @param key
	 * @return
	 */
	private int getId(String key){
		if(mIDmap == null){
			mIDmap = new HashMap<String, Integer>();
		}
		if(!mIDmap.containsKey(key)){
			mIDmap.put(key, mResourceManager.getIdentifier(key, "id"));
		}
		return mIDmap.get(key);
	}
	
}