package cn.emagroup.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import cn.emagroup.sdk.Ema;
import cn.emagroup.sdk.comm.ConfigManager;
import cn.emagroup.sdk.comm.DeviceInfoManager;
import cn.emagroup.sdk.comm.EmaCallBackConst;
import cn.emagroup.sdk.comm.ResourceManager;
import cn.emagroup.sdk.comm.Url;
import cn.emagroup.sdk.pay.EmaDialogPayPromptCancel;
import cn.emagroup.sdk.pay.EmaDialogPayPromptResult;
import cn.emagroup.sdk.pay.EmaPay;
import cn.emagroup.sdk.pay.EmaPayProcessManager;
import cn.emagroup.sdk.user.EmaUser;
import cn.emagroup.sdk.utils.EmaConst;
import cn.emagroup.sdk.utils.LOG;
import cn.emagroup.sdk.utils.PropertyField;
import cn.emagroup.sdk.utils.ToastHelper;
import cn.emagroup.sdk.utils.UCommUtil;

public class WebViewActivity extends Activity implements OnClickListener {

	private static final String TAG = "WebViewActivity";
	
	public static final String INTENT_TITLE = "webview_title";//webView显示的标题
	public static final String INTENT_URL = "url";//webView显示的url
	public static final String INTENT_TYPE = "type";//调用webView的类别
	public static final String INTENT_INFORGAME = "inforgame";//webView的结果是否需要通知游戏
	
	public static final int CODE_LOGOUT = 10;//退出
	private static final int CODE_CHANGE_TITLE = 11;//修改标题
	public static final int CODE_CLEAR_HISTORY = 40;//清楚浏览记录
	public static final int CODE_TENPAY_SUCC = 30;//财付通充值成功
	
	public static final int TYPE_EMAACCOUNT = 1;//账号中心
	public static final int TYPE_GIFT = 2;
	public static final int TYPE_HELP = 3;
	public static final int TYPE_TENPAY = 4;//财付通支付  ***** 目前支付只有财付通一个 ******
	public static final int TYPE_FIND_LOGIN_PASSW = 5;//找回用户密码
	public static final int TYPE_FIND_WALLET_PASSW = 7;//找回钱包密码
	public static final int TYPE_PROMOTION = 6;//推广
	
	private int mType;//标记打开网页的类型（即从哪个入口进来的）
	//标记
	private boolean mFlagIsNeedInforGame = false;//是否需要回调游戏
	
	private EmaUser mEmaUser;
	private ConfigManager mConfigManager;
	private DeviceInfoManager mDeviceInfoManager;
	private ResourceManager mResourceManager;
	private EmaPay mEmaPay;
	
	//views
	private ImageView mBtnBack;
	private ImageView mBtnReturnGame;
	private TextView mTxtTitle;
	private WebView mWebView;
	private RadioButton mBtnAccount;
	private RadioButton mBtnGift;
	private RadioButton mBtnPromotion;
	private RadioButton mBtnHelp;
	private RadioGroup mRadioGroupView;
	private ProgressBar mProgressBar;
	
	
	private Map<String, Integer> mIDMap;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case CODE_TENPAY_SUCC://财付通支付成功
				doTenpaySucc();
				break;
			case CODE_LOGOUT://退出
				break;
			case CODE_CLEAR_HISTORY:
				mWebView.clearHistory();
				break;
			case CODE_CHANGE_TITLE://修改标题
				doUrlChange((String)msg.obj);
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
		
		mEmaUser = EmaUser.getInstance();
		mConfigManager = ConfigManager.getInstance(this);
		mDeviceInfoManager = DeviceInfoManager.getInstance(this);
		mResourceManager = ResourceManager.getInstance(this);
		mEmaPay = EmaPay.getInstance(this);

		initView();

		initData();
	}
	
	/**
	 * 初始化界面
	 */
	@SuppressLint("NewApi")
	private void initView() {
		setContentView(mResourceManager.getIdentifier("ema_webview", "layout"));
		mBtnBack = (ImageView) findViewById(getID("ema_webview_back"));
		mBtnReturnGame = (ImageView) findViewById(getID("ema_webview_imageView_return"));
		mTxtTitle = (TextView) findViewById(getID("ema_webView_title"));
		mBtnAccount = (RadioButton) findViewById(getID("ema_webview_account"));
		mBtnGift = (RadioButton) findViewById(getID("ema_webview_gift"));
		mBtnPromotion = (RadioButton) findViewById(getID("ema_webview_promotion"));
		mBtnHelp = (RadioButton) findViewById(getID("ema_webview_help"));
		mRadioGroupView = (RadioGroup) findViewById(getID("ema_webview_RadioGroup"));
		mProgressBar = (ProgressBar) findViewById(getID("ema_webview_ProgressBar"));
		
		mWebView = (WebView) findViewById(getID("ema_webview_url"));
		
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setUseWideViewPort(true);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setDefaultZoom(ZoomDensity.MEDIUM);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setTextZoom(75);
		mWebView.getSettings().setLoadsImagesAutomatically(true);
		mWebView.getSettings()
				.setCacheMode(WebSettings.LOAD_NO_CACHE);
		mWebView.getSettings().setLayoutAlgorithm(
				LayoutAlgorithm.NARROW_COLUMNS);
		mWebView.setWebViewClient(new WebViewClientEma(mHandler));
		mWebView.setWebChromeClient(new WebChromeClient());
		mWebView.setWebChromeClient(new WebChromeClient(){
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				if(newProgress == 100){
					mProgressBar.setVisibility(View.INVISIBLE);
					Message msg = new Message();
					msg.what = CODE_CHANGE_TITLE;
					msg.obj = view.getUrl();
					mHandler.sendMessage(msg);
				}else {
					if(View.INVISIBLE == mProgressBar.getVisibility()){
						mProgressBar.setVisibility(View.VISIBLE);
					}
					mProgressBar.setProgress(newProgress);
				}
				super.onProgressChanged(view, newProgress);
			}
		});
		
		
		mBtnReturnGame.setOnClickListener(this);
		mBtnBack.setOnClickListener(this);
		mBtnBack.setVisibility(View.GONE);
		
		mRadioGroupView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int arg1) {
				if(!EmaUser.getInstance().getIsLogin()){
					ToastHelper.toast(WebViewActivity.this, "请先登录");
					return;
				}
				int checkedId = group.getCheckedRadioButtonId();
				if(checkedId == getID("ema_webview_account")){
					mWebView.loadUrl(Url.getWebUrlUserInfo());
					doSetTitle("个人中心");
				}else if(checkedId == getID("ema_webview_gift")){
					mWebView.loadUrl(Url.getWebUrlGift());
					doSetTitle("礼包列表");
//					ToastHelper.toast(WebViewActivity.this, "礼包暂未开放");
				}else if(checkedId == getID("ema_webview_help")){
					mWebView.loadUrl(Url.getWebUrlHelp());
					doSetTitle("帮助中心");
				}else if(checkedId == getID("ema_webview_promotion")){
//					mWebView.loadUrl(Url.getWebUrlPromotion());
//					doSetTitle("推广");
					ToastHelper.toast(WebViewActivity.this, "推广暂未开放");
				}
			}
		});
	}

	/**
	 * 初始化数据
	 */
	private void initData() {
		Intent intent = getIntent();
		mTxtTitle.setText(intent.getStringExtra(INTENT_TITLE));
		mType = intent.getIntExtra(INTENT_TYPE, TYPE_EMAACCOUNT);
		mFlagIsNeedInforGame = intent.getBooleanExtra(INTENT_INFORGAME, false);
		
		if(mType == TYPE_TENPAY || mType == TYPE_FIND_WALLET_PASSW){
			if(mFlagIsNeedInforGame){
				EmaPayProcessManager.getInstance().addPayActivity(this);
			}else{
				EmaPayProcessManager.getInstance().addRechargeActivity(this);
			}
		}
		
		String url = intent.getStringExtra(INTENT_URL);
		
		switch(mType){
		case TYPE_EMAACCOUNT:
			doSetCookies(url);
			mBtnAccount.setChecked(true);
			break;
		case TYPE_GIFT:
			doSetCookies(url);
			mBtnGift.setChecked(true);
//			ToastHelper.toast(WebViewActivity.this, "礼包暂未开放");
			break;
		case TYPE_PROMOTION:
//			doSetCookies(url);
//			mBtnPromotion.setChecked(true);
			ToastHelper.toast(WebViewActivity.this, "推广暂未开放");
			return;
		case TYPE_HELP:
			doSetCookies(url);
			mBtnHelp.setChecked(true);
			break;
		case TYPE_TENPAY:
			mRadioGroupView.setVisibility(View.GONE);
			break;
		case TYPE_FIND_LOGIN_PASSW:
			doSetCookies(url);
			break;
		}
		LOG.d(TAG, "url__:" + url);
		mWebView.loadUrl(url);
	}
	
	/**
	 * Click监听事件
	 */
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == getID("ema_webview_imageView_return")){//返回游戏
			if(mType == TYPE_TENPAY || mType == TYPE_FIND_WALLET_PASSW){//支付或者充值
				new EmaDialogPayPromptCancel(this).show();
				return;
			}else if(mType != TYPE_FIND_LOGIN_PASSW){
				ToolBar.getInstance(this).showToolBar();
			}
			this.finish();
		}else if(id == getID("ema_webview_back")){//返回上一层界面 NOTE:目前这个按钮应该是出不来了，隐掉了
		}
	}
	
	/**
	 * 财付通支付成功后的操作，需要关闭页面
	 */
	private void doTenpaySucc(){
		if(mFlagIsNeedInforGame){
			UCommUtil.makePayCallBack(EmaCallBackConst.PAYSUCCESS, "财付通支付成功！");
		}else{
			sendBroadcast(new Intent(PropertyField.BROADCAST_RECHARGE_SUCC));
		}
		new EmaDialogPayPromptResult(this, 
				mFlagIsNeedInforGame ? EmaConst.PAY_ACTION_TYPE_PAY : EmaConst.PAY_ACTION_TYPE_RECHARGE,
				EmaConst.PAY_RESULT_SUCC, 
				mFlagIsNeedInforGame ? "支付成功" : "充值成功").show();
	}
	
	/**
	 * 账号中心需要设置cookie
	 * @param url
	 */
	private void doSetCookies(String url){
		CookieSyncManager.createInstance(this);
		CookieManager cookieManager = CookieManager.getInstance();
		cookieManager.setAcceptCookie(true);
		cookieManager.removeAllCookie();
		
		EmaUser emaUser = EmaUser.getInstance();
		
		cookieManager.setCookie(url, getCookie("token", emaUser.getmToken()));
		cookieManager.setCookie(url, getCookie("uid", emaUser.getmUid()));
		cookieManager.setCookie(url, getCookie("nickname", emaUser.getNickName()));
		//cookieManager.setCookie(url, getCookie("appid", mConfigManager.getAppId()));
		if(emaUser.getRoleInfo() != null){
			cookieManager.setCookie(url, getCookie(EmaConst.EMA_SUBMIT_ROLE_ID, emaUser.getRoleInfo().getRoleId()));
			cookieManager.setCookie(url, getCookie(EmaConst.EMA_SUBMIT_ROLE_NAME, emaUser.getRoleInfo().getRoleName()));
			cookieManager.setCookie(url, getCookie(EmaConst.EMA_SUBMIT_ROLE_LEVEL, emaUser.getRoleInfo().getRoleLevel()));
			cookieManager.setCookie(url, getCookie(EmaConst.EMA_SUBMIT_SERVER_ID, emaUser.getRoleInfo().getServerId()));
			cookieManager.setCookie(url, getCookie(EmaConst.EMA_SUBMIT_SERVER_NAME, emaUser.getRoleInfo().getServerName()));
		}
		CookieSyncManager.getInstance().sync();
		
		String str = cookieManager.getCookie(url);
		LOG.d(TAG, "cookStr__:" + str);
	}
	
	private String getCookie(String key, String value){
		LOG.d(TAG, "key__:" + key + "    vlaue:" + value);
		return key + "=" + value + ";domain=.lemonade-game.com;path=/";
	}
	
	/**
	 * 为不同的界面需要作出不同的改变
	 * @param url
	 */
	private void doUrlChange(String url) {
		// 个人中心
		if (-1 != url.indexOf("/userinfo")) {
			doSetTitle("个人中心");
		}
		// 修改密码
		if (-1 != url.indexOf("/modifypwd")) {
			doSetTitle("修改密码");
		}
		// 忘记密码
		if (-1 != url.indexOf("/forgetpwd")) {
			doSetTitle("忘记密码");
		}
		// bindphone.html 手机绑定
		if (-1 != url.indexOf("/bindphone")) {
			doSetTitle("手机绑定");
		}
		// isbind.html 更换绑定
		if (-1 != url.indexOf("/isbind")) {
			doSetTitle("更换绑定");
		}
		// libao/libaolist.html 礼包列表
		if (-1 != url.indexOf("/libao/libaolist")) {
			doSetTitle("礼包列表");
		}
		// libao/lingqu.html 我的礼包（我领取的号）
		if (-1 != url.indexOf("/libao/lingqu")) {
			doSetTitle("我的礼包");
		}
		// libao/libaodetail.html 礼包详情
		if (-1 != url.indexOf("/libao/libaodetail")) {
			doSetTitle("礼包详情");
		}
		// chargerecord.html 充值记录
		if (-1 != url.indexOf("/chargerecord")) {
			doSetTitle("充值记录");
		}
		// forgetwtpwd.html 钱包密码
		if (-1 != url.indexOf("/forgetwtpwd")) {
			doSetTitle("找回钱包密码");
		}
		// chargesucc.html 支付成功页面
		if (-1 != url.indexOf("/chargesucc")) {
			doSetTitle("支付成功");
		}
	}
	
	/**
	 * 设置标题
	 * @param title
	 */
	private void doSetTitle(String title){
		mTxtTitle.setText(title);
	}
	
	/**
	 * 监听返回按钮
	 */
	@Override
	public void onBackPressed() {
		//如果有上层界面，则返回到上层界面，没有的话结束页面（是否需要提示？？？？）
		if(mWebView.canGoBack()){
			mWebView.goBack();
			doUrlChange(mWebView.getUrl());
		}else{
			if(mType == TYPE_TENPAY && mFlagIsNeedInforGame){
				new EmaDialogPayPromptCancel(this).show();
			}else{
				closeWebView();
			}
		}
	}
	
	/**
	 * 关闭WebViewActivity，之后要显示toolbar(支付页面，不需要显示)
	 * so stupid ！！！！！ can you fix me ....
	 * 
	 */
	private void closeWebView(){
		this.finish();
		if(mType != TYPE_TENPAY && mType != TYPE_FIND_LOGIN_PASSW && mType != TYPE_FIND_WALLET_PASSW){
			Ema.getInstance().showToolBar();
		}
	}
	
	/**
	 * 获取资源ID
	 * @param key
	 * @return
	 */
	private int getID(String key){
		if(mIDMap == null){
			mIDMap = new HashMap<String, Integer>();
		}
		if(!mIDMap.containsKey(key)){
			int id = mResourceManager.getIdentifier(key, "id");
			mIDMap.put(key, id);
		}
		return mIDMap.get(key);
	}
} 
