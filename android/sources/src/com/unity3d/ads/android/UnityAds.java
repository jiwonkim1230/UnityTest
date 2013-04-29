package com.unity3d.ads.android;

import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import com.unity3d.ads.android.cache.UnityAdsCacheManager;
import com.unity3d.ads.android.cache.UnityAdsDownloader;
import com.unity3d.ads.android.cache.IUnityAdsCacheListener;
import com.unity3d.ads.android.campaign.UnityAdsCampaign;
import com.unity3d.ads.android.campaign.UnityAdsCampaignHandler;
import com.unity3d.ads.android.campaign.UnityAdsRewardItem;
import com.unity3d.ads.android.campaign.UnityAdsCampaign.UnityAdsCampaignStatus;
import com.unity3d.ads.android.properties.UnityAdsConstants;
import com.unity3d.ads.android.properties.UnityAdsProperties;
import com.unity3d.ads.android.view.UnityAdsMainView;
import com.unity3d.ads.android.view.IUnityAdsMainViewListener;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewAction;
import com.unity3d.ads.android.view.UnityAdsMainView.UnityAdsMainViewState;
import com.unity3d.ads.android.webapp.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;


public class UnityAds implements IUnityAdsCacheListener, 
										IUnityAdsWebDataListener, 
										IUnityAdsWebBrigeListener,
										IUnityAdsMainViewListener {
	
	// Reward item HashMap keys
	public static final String UNITY_ADS_REWARDITEM_PICTURE_KEY = "picture";
	public static final String UNITY_ADS_REWARDITEM_NAME_KEY = "name";
	
	// Unity Ads developer options keys
	public static final String UNITY_ADS_OPTION_NOOFFERSCREEN_KEY = "noOfferScreen";
	public static final String UNITY_ADS_OPTION_OPENANIMATED_KEY = "openAnimated";
	public static final String UNITY_ADS_OPTION_GAMERSID_KEY = "sid";
	public static final String UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS = "muteVideoSounds";

	// Unity Ads components
	public static UnityAds instance = null;
	public static UnityAdsCacheManager cachemanager = null;
	public static UnityAdsWebData webdata = null;
	public static UnityAdsMainView mainview = null;
	
	// Temporary data
	private boolean _initialized = false;
	private boolean _showingAds = false;
	private boolean _adsReadySent = false;
	private boolean _webAppLoaded = false;
	private boolean _openRequestFromDeveloper = false;
	private AlertDialog _alertDialog = null;
		
	
	// Listeners
	private IUnityAdsListener _adsListener = null;
	
	
	public UnityAds (Activity activity, String gameId) {
		init(activity, gameId, null);
	}
	
	public UnityAds (Activity activity, String gameId, IUnityAdsListener listener) {
		init(activity, gameId, listener);
	}

	
	/* PUBLIC STATIC METHODS */
	
	public static boolean isSupported () {
		if (Build.VERSION.SDK_INT < 9) {
			return false;
		}
		
		return true;
	}
	
	public static void setDebugMode (boolean debugModeEnabled) {
		UnityAdsProperties.UNITY_ADS_DEBUG_MODE = debugModeEnabled;
	}
	
	public static void setTestMode (boolean testModeEnabled) {
		UnityAdsProperties.TESTMODE_ENABLED = testModeEnabled;
	}
	
	public static String getSDKVersion () {
		return UnityAdsConstants.UNITY_ADS_VERSION;
	}
	
	
	/* PUBLIC METHODS */
	
	public void setListener (IUnityAdsListener listener) {
		_adsListener = listener;
	}
	
	public void changeActivity (Activity activity) {
		if (activity == null) return;
		
		if (activity != null && !activity.equals(UnityAdsProperties.CURRENT_ACTIVITY)) {
			UnityAdsProperties.CURRENT_ACTIVITY = activity;
			
			// Not the most pretty way to detect when the fullscreen activity is ready
			if (activity != null &&
				activity.getClass() != null &&
				activity.getClass().getName() != null &&
				activity.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				
				String view = null;
				
				if (mainview != null && mainview.webview != null) {
					view = mainview.webview.getWebViewCurrentView();
					
					if (_openRequestFromDeveloper) {
						view = UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START;
						UnityAdsUtils.Log("changeActivity: This open request is from the developer, setting start view", this);
					}
					
					if (view != null)
						open(view);
				}
				
				_openRequestFromDeveloper = false;
			}
			else {
				UnityAdsProperties.BASE_ACTIVITY = activity;
			}
		}
	}
	
	public boolean hide () {
		if (_showingAds) {
			close();
			return true;
		}
		
		return false;
	}
	
	public boolean show (Map<String, Object> options) {
		if (canShow()) {
			UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS = options;
			
			if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS != null) {
				if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY) && UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY).equals(true)) {
					if (webdata.getViewableVideoPlanCampaigns().size() > 0) {
						UnityAdsCampaign selectedCampaign = webdata.getViewableVideoPlanCampaigns().get(0);
						UnityAdsProperties.SELECTED_CAMPAIGN = selectedCampaign;
					}
				}
				if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UNITY_ADS_OPTION_GAMERSID_KEY) && UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_GAMERSID_KEY) != null) {
					UnityAdsProperties.GAMER_SID = "" + UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_GAMERSID_KEY);
				}
			}
			
			return show();
		}
		
		return false;
	}
	
	public boolean show () {
		if (canShow()) {
			_openRequestFromDeveloper = true;
			_showingAds = true;
			startAdsFullscreenActivity();
			return _showingAds;
		}

		return false;
	}
	
	public boolean canShowAds () {
		return mainview != null && 
			mainview.webview != null && 
			mainview.webview.isWebAppLoaded() && 
			_webAppLoaded && 
			webdata != null && 
			webdata.getViewableVideoPlanCampaigns() != null && 
			webdata.getViewableVideoPlanCampaigns().size() > 0;
	}
	
	public boolean canShow () {
		return !_showingAds && 
			mainview != null && 
			mainview.webview != null && 
			mainview.webview.isWebAppLoaded() && 
			_webAppLoaded && 
			webdata != null && 
			webdata.getVideoPlanCampaigns() != null && 
			webdata.getVideoPlanCampaigns().size() > 0;
	}

	public void stopAll () {
		UnityAdsUtils.Log("stopAll()", this);
		if (mainview != null && mainview.videoplayerview != null)
			mainview.videoplayerview.clearVideoPlayer();
		if (mainview != null && mainview.webview != null)
			mainview.webview.clearWebView();
		
		UnityAdsDownloader.stopAllDownloads();
		UnityAdsDownloader.clearData();
		cachemanager.setDownloadListener(null);
		cachemanager.clearData();
		webdata.stopAllRequests();
		webdata.setWebDataListener(null);
		webdata.clearData();
		UnityAdsProperties.BASE_ACTIVITY = null;
		UnityAdsProperties.CURRENT_ACTIVITY = null;
		UnityAdsProperties.SELECTED_CAMPAIGN = null;
	}
	
	
	/* PUBLIC MULTIPLE REWARD ITEM SUPPORT */
	
	public boolean hasMultipleRewardItems () {
		if (webdata.getRewardItems() != null && webdata.getRewardItems().size() > 0)
			return true;
		
		return false;
	}
	
	public ArrayList<String> getRewardItemKeys () {
		if (webdata.getRewardItems() != null && webdata.getRewardItems().size() > 0) {
			ArrayList<UnityAdsRewardItem> rewardItems = webdata.getRewardItems();
			ArrayList<String> rewardItemKeys = new ArrayList<String>();
			for (UnityAdsRewardItem rewardItem : rewardItems) {
				rewardItemKeys.add(rewardItem.getKey());
			}
			
			return rewardItemKeys;
		}
		
		return null;
	}
	
	public String getDefaultRewardItemKey () {
		if (webdata != null && webdata.getDefaultRewardItem() != null)
			return webdata.getDefaultRewardItem().getKey();
		
		return null;
	}
	
	public String getCurrentRewardItemKey () {
		if (webdata != null && webdata.getCurrentRewardItemKey() != null)
			return webdata.getCurrentRewardItemKey();
			
		return null;
	}
	
	public boolean setRewardItemKey (String rewardItemKey) {
		if (canShow()) {
			UnityAdsRewardItem rewardItem = webdata.getRewardItemByKey(rewardItemKey);
			
			if (rewardItem != null) {
				webdata.setCurrentRewardItem(rewardItem);
				return true;
			}
		}
		
		return false;
	}
	
	public void setDefaultRewardItemAsRewardItem () {
		if (canShow()) {
			if (webdata != null && webdata.getDefaultRewardItem() != null) {
				webdata.setCurrentRewardItem(webdata.getDefaultRewardItem());
			}
		}
	}
	
	public Map<String, String> getRewardItemDetailsWithKey (String rewardItemKey) {
		UnityAdsRewardItem rewardItem = webdata.getRewardItemByKey(rewardItemKey);
		if (rewardItem != null) {
			return rewardItem.getDetails();
		}
		else {
			UnityAdsUtils.Log("Could not fetch reward item: " + rewardItemKey, this);
		}
		
		return null;
	}
	
	
	/* LISTENER METHODS */
	
	// IUnityAdsMainViewListener
	public void onMainViewAction (UnityAdsMainViewAction action) {
		switch (action) {
			case BackButtonPressed:
				if (_showingAds)
					close();
				break;
			case VideoStart:
				if (_adsListener != null)
					_adsListener.onVideoStarted();
				break;
			case VideoEnd:
				if (_adsListener != null && UnityAdsProperties.SELECTED_CAMPAIGN != null && !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed()) {
					UnityAdsProperties.SELECTED_CAMPAIGN.setCampaignStatus(UnityAdsCampaignStatus.VIEWED);
					_adsListener.onVideoCompleted(getCurrentRewardItemKey());
				}
				break;
			case RequestRetryVideoPlay:
				UnityAdsUtils.Log("Retrying video play, because something went wrong.", this);
				playVideo(300);
				break;
		}
	}
	
	
	// IUnityAdsCacheListener
	@Override
	public void onCampaignUpdateStarted () {	
		UnityAdsUtils.Log("Campaign updates started.", this);
	}
	
	@Override
	public void onCampaignReady (UnityAdsCampaignHandler campaignHandler) {
		if (campaignHandler == null || campaignHandler.getCampaign() == null) return;
				
		UnityAdsUtils.Log("Got onCampaignReady: " + campaignHandler.getCampaign().toString(), this);
		
		if (canShowAds())
			sendAdsReadyEvent();
	}
	
	@Override
	public void onAllCampaignsReady () {
		UnityAdsUtils.Log("Listener got \"All campaigns ready.\"", this);
	}
	
	// IUnityAdsWebDataListener
	@Override
	public void onWebDataCompleted () {
		JSONObject jsonData = null;
		boolean dataFetchFailed = false;
		String nativeSdkVersion = null;
		
		if (webdata.getData() != null && webdata.getData().has(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY)) {
			try {
				jsonData = webdata.getData().getJSONObject(UnityAdsConstants.UNITY_ADS_JSON_DATA_ROOTKEY);
			}
			catch (Exception e) {
				dataFetchFailed = true;
			}
			
			if (!dataFetchFailed) {
				if (jsonData.has(UnityAdsConstants.UNITY_ADS_NATIVESDKVERSION_KEY)) {
					try {
						nativeSdkVersion = jsonData.getString(UnityAdsConstants.UNITY_ADS_NATIVESDKVERSION_KEY);
					}
					catch (Exception e) {
						dataFetchFailed = true;
					}
				}
			}
		}
		
		if (nativeSdkVersion != null && !dataFetchFailed && UnityAdsUtils.isDebuggable(UnityAdsProperties.CURRENT_ACTIVITY)) {
			if (!nativeSdkVersion.equals(UnityAdsConstants.UNITY_ADS_VERSION)) {
				_alertDialog = new AlertDialog.Builder(UnityAdsProperties.CURRENT_ACTIVITY).create();
				_alertDialog.setTitle("Unity Ads");
				_alertDialog.setMessage("You are not running the latest version of Unity Ads android. Please update your version (this dialog won't appear in release builds).");
				_alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						_alertDialog.dismiss();
					}
				});
				
				_alertDialog.show();
			}
		}
		
		setup();
	}
	
	@Override
	public void onWebDataFailed () {
		if (_adsListener != null)
			_adsListener.onFetchFailed();
	}
	
	
	// IUnityAdsWebBrigeListener
	@Override
	public void onPlayVideo(JSONObject data) {
		UnityAdsUtils.Log("onPlayVideo", this);
		if (data.has(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY)) {
			String campaignId = null;
			
			try {
				campaignId = data.getString(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_CAMPAIGNID_KEY);
			}
			catch (Exception e) {
				UnityAdsUtils.Log("Could not get campaignId", this);
			}
			
			if (campaignId != null) {
				if (webdata != null && webdata.getCampaignById(campaignId) != null) {
					UnityAdsProperties.SELECTED_CAMPAIGN = webdata.getCampaignById(campaignId);
				}
				
				if (UnityAdsProperties.SELECTED_CAMPAIGN != null && 
					UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() != null && 
					UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId().equals(campaignId)) {
					
					Boolean rewatch = false;
					
					try {
						rewatch = data.getBoolean(UnityAdsConstants.UNITY_ADS_WEBVIEW_EVENTDATA_REWATCH_KEY);
					}
					catch (Exception e) {
					}
					
					UnityAdsUtils.Log("onPlayVideo: Selected campaign=" + UnityAdsProperties.SELECTED_CAMPAIGN.getCampaignId() + " isViewed: " + UnityAdsProperties.SELECTED_CAMPAIGN.isViewed(), this);
					if (UnityAdsProperties.SELECTED_CAMPAIGN != null && (rewatch || !UnityAdsProperties.SELECTED_CAMPAIGN.isViewed())) {
						playVideo();
					}
				}
			}
		}
	}

	@Override
	public void onPauseVideo(JSONObject data) {
	}

	@Override
	public void onCloseAdsView(JSONObject data) {
		hide();
	}
	
	@Override
	public void onWebAppInitComplete (JSONObject data) {
		UnityAdsUtils.Log("WebApp init complete", this);
		_webAppLoaded = true;
		Boolean dataOk = true;
		
		if (canShowAds()) {
			JSONObject setViewData = new JSONObject();
			
			try {
				setViewData.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_INITCOMPLETE);
				setViewData.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
			}
			catch (Exception e) {
				dataOk = false;
			}
			
			if (dataOk) {
				mainview.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_START, setViewData);
				sendAdsReadyEvent();			
			}
		}
	}
	
	public void onOpenPlayStore (JSONObject data) {
	    UnityAdsUtils.Log("onOpenPlayStore", this);
		if (UnityAdsProperties.SELECTED_CAMPAIGN != null && UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId() != null) {
			try {
				if (!UnityAdsProperties.SELECTED_CAMPAIGN.shouldBypassAppSheet()) {
					UnityAdsUtils.Log("Opening playstore activity with storeId: " + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId(), this);
					UnityAdsProperties.CURRENT_ACTIVITY.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId())));
				}
				else {
					openPlayStoreInBrowser();
				}
			} 
			catch (android.content.ActivityNotFoundException anfe) {
				openPlayStoreInBrowser();
			}
			
			webdata.sendAnalyticsRequest(UnityAdsConstants.UNITY_ADS_ANALYTICS_EVENTTYPE_OPENAPPSTORE, UnityAdsProperties.SELECTED_CAMPAIGN);
		}
		else {
		    UnityAdsUtils.Log("Selected campaign (" + UnityAdsProperties.SELECTED_CAMPAIGN + ") or couldn't get storeId", this);
		}
	}
	

	/* PRIVATE METHODS */
	
	private void openPlayStoreInBrowser () {
	    UnityAdsUtils.Log("Could not open PlayStore activity, opening in browser with storeId: " + UnityAdsProperties.SELECTED_CAMPAIGN.getStoreId(), this);
		UnityAdsProperties.CURRENT_ACTIVITY.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(UnityAdsProperties.SELECTED_CAMPAIGN.getClickUrl())));
	}
	
	private void init (Activity activity, String gameId, IUnityAdsListener listener) {
		if (_initialized) return; 
		
		instance = this;
		setListener(listener);
		
		UnityAdsProperties.UNITY_ADS_GAME_ID = gameId;
		UnityAdsProperties.BASE_ACTIVITY = activity;
		UnityAdsProperties.CURRENT_ACTIVITY = activity;
		
		UnityAdsUtils.Log("Is debuggable=" + UnityAdsUtils.isDebuggable(activity), this);
		
		
		cachemanager = new UnityAdsCacheManager();
		cachemanager.setDownloadListener(this);
		webdata = new UnityAdsWebData();
		webdata.setWebDataListener(this);

		if (webdata.initCampaigns()) {
			_initialized = true;
		}
	}
	
	private void close () {
		UnityAdsCloseRunner closeRunner = new UnityAdsCloseRunner();
		UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(closeRunner);
	}
	
	private void open (String view) {
		Boolean dataOk = true;			
		JSONObject data = new JSONObject();
		
		try  {
			data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_OPEN);
			data.put(UnityAdsConstants.UNITY_ADS_REWARD_ITEMKEY_KEY, webdata.getCurrentRewardItemKey());
		}
		catch (Exception e) {
			dataOk = false;
		}

		UnityAdsUtils.Log("open() dataOk: " + dataOk, this);
		
		if (dataOk && view != null) {
			UnityAdsUtils.Log("open() opening with view:" + view + " and data:" + data.toString(), this);
			
			if (mainview != null) {
				mainview.openAds(view, data);
				
				if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS != null && 
					UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY)  && 
					UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_NOOFFERSCREEN_KEY).equals(true))
						playVideo();
				
				if (_adsListener != null)
					_adsListener.onShow();
			}
		}
	}

	private void setup () {
		initCache();
		setupViews();
	}
	
	private void initCache () {
		if (_initialized) {
			UnityAdsUtils.Log("Init cache", this);
			// Update cache WILL START DOWNLOADS if needed, after this method you can check getDownloadingCampaigns which ones started downloading.
			cachemanager.updateCache(webdata.getVideoPlanCampaigns());				
		}
	}
	
	private void sendAdsReadyEvent () {
		if (!_adsReadySent && _adsListener != null) {
			UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {				
				@Override
				public void run() {
					UnityAdsUtils.Log("Unity Ads ready!", this);
					_adsReadySent = true;
					_adsListener.onFetchCompleted();
				}
			});
		}
	}

	private void setupViews () {
		mainview = new UnityAdsMainView(UnityAdsProperties.CURRENT_ACTIVITY, this);
	}

	private void playVideo () {
		playVideo(0);
	}
	
	private void playVideo (long delay) {
		UnityAdsUtils.Log("Running threaded", this);
		
		if (delay > 0) {
			Timer delayTimer = new Timer();
			delayTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					UnityAdsUtils.Log("Delayed video start", this);
					UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
					if (UnityAdsProperties.CURRENT_ACTIVITY != null)
						UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(playVideoRunner);
				}
			}, delay);
		}
		else {
			UnityAdsPlayVideoRunner playVideoRunner = new UnityAdsPlayVideoRunner();
			if (UnityAdsProperties.CURRENT_ACTIVITY != null)
				UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(playVideoRunner);
		}
	}
	
	private void startAdsFullscreenActivity () {
		Intent newIntent = new Intent(UnityAdsProperties.CURRENT_ACTIVITY, com.unity3d.ads.android.view.UnityAdsFullscreenActivity.class);
		int flags = Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_NEW_TASK;
		
		if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS != null && 
			UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UNITY_ADS_OPTION_OPENANIMATED_KEY) && 
			UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_OPENANIMATED_KEY).equals(true))
				flags = Intent.FLAG_ACTIVITY_NEW_TASK;
		
		newIntent.addFlags(flags);
		
		try {
			UnityAdsProperties.BASE_ACTIVITY.startActivity(newIntent);
		}
		catch (ActivityNotFoundException e) {
			UnityAdsUtils.Log("Could not find activity: " + e.getStackTrace(), this);
		}
		catch (Exception e) {
			UnityAdsUtils.Log("Weird error: " + e.getStackTrace(), this);
		}
	}
	
	
	/* INTERNAL CLASSES */

	// FIX: Could these 2 classes be moved to MainView
	
	private class UnityAdsCloseRunner implements Runnable {
		JSONObject _data = null;
		@Override
		public void run() {
			
			if (UnityAdsProperties.CURRENT_ACTIVITY.getClass().getName().equals(UnityAdsConstants.UNITY_ADS_FULLSCREEN_ACTIVITY_CLASSNAME)) {
				Boolean dataOk = true;			
				JSONObject data = new JSONObject();
				
				try  {
					data.put(UnityAdsConstants.UNITY_ADS_WEBVIEW_API_ACTION_KEY, UnityAdsConstants.UNITY_ADS_WEBVIEW_API_CLOSE);
				}
				catch (Exception e) {
					dataOk = false;
				}

				UnityAdsUtils.Log("dataOk: " + dataOk, this);
				
				if (dataOk) {
					_data = data;
					mainview.webview.setWebViewCurrentView(UnityAdsConstants.UNITY_ADS_WEBVIEW_VIEWTYPE_NONE, data);
					Timer testTimer = new Timer();
					testTimer.schedule(new TimerTask() {
						@Override
						public void run() {
							UnityAdsProperties.CURRENT_ACTIVITY.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mainview.closeAds(_data);
									UnityAdsProperties.CURRENT_ACTIVITY.finish();
									
									if (UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS == null || 
										!UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.containsKey(UNITY_ADS_OPTION_OPENANIMATED_KEY) || 
										UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS.get(UNITY_ADS_OPTION_OPENANIMATED_KEY).equals(false))
											UnityAdsProperties.CURRENT_ACTIVITY.overridePendingTransition(0, 0);
									
									UnityAdsProperties.UNITY_ADS_DEVELOPER_OPTIONS = null;
									_showingAds = false;
									
									if (_adsListener != null)
										_adsListener.onHide();
								}
							});
						}
					}, 250);
				}
			}
			
			// Reset developer options when Unity Ads closes
		}
	}
	
	private class UnityAdsPlayVideoRunner implements Runnable {
		@Override
		public void run() {			
			UnityAdsUtils.Log("Running videoplayrunner", this);
			if (UnityAdsProperties.SELECTED_CAMPAIGN != null) {
				UnityAdsUtils.Log("Selected campaign found", this);
				JSONObject data = new JSONObject();
				
				try {
					data.put(UnityAdsConstants.UNITY_ADS_TEXTKEY_KEY, UnityAdsConstants.UNITY_ADS_TEXTKEY_BUFFERING);
				}
				catch (Exception e) {
					UnityAdsUtils.Log("Couldn't create data JSON", this);
					return;
				}
				
				mainview.webview.sendNativeEventToWebApp(UnityAdsConstants.UNITY_ADS_NATIVEEVENT_SHOWSPINNER, data);
				
				String playUrl = UnityAdsUtils.getCacheDirectory() + "/" + UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename();
				if (!UnityAdsUtils.isFileInCache(UnityAdsProperties.SELECTED_CAMPAIGN.getVideoFilename()))
					playUrl = UnityAdsProperties.SELECTED_CAMPAIGN.getVideoStreamUrl(); 

				mainview.setViewState(UnityAdsMainViewState.VideoPlayer);
				UnityAdsUtils.Log("Start videoplayback with: " + playUrl, this);
				mainview.videoplayerview.playVideo(playUrl);
			}			
			else
				UnityAdsUtils.Log("Campaign is null", this);
		}		
	}
}
