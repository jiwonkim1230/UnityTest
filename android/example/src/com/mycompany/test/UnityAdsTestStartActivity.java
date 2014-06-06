
package com.mycompany.test;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.unity3d.ads.android.UnityAds;
import com.unity3d.ads.android.IUnityAdsListener;

public class UnityAdsTestStartActivity extends Activity implements IUnityAdsListener {
	private UnityAdsTestStartActivity _self = null;
	private Button _settingsButton = null;
	private Button _startButton = null;
	private Button _openButton = null;
	private RelativeLayout _optionsView = null;
	private TextView _instructions = null;
	private ImageView _statusImage = null;
	private String _exampleAppLogTag = "UnityAdsExample";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onCreate()");
        super.onCreate(savedInstanceState);
        
        _self = this;
        
        setContentView(R.layout.main);
		Log.d(_exampleAppLogTag, "Init Unity Ads");
		
		UnityAds.setDebugMode(true);
		//UnityAds.setTestMode(true);

		_optionsView = ((RelativeLayout)findViewById(R.id.optionsView));
		
		_settingsButton = ((Button)findViewById(R.id.sandrabullock));
		_settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (_optionsView != null) {
					if (_optionsView.getVisibility() == View.INVISIBLE) {
						_optionsView.setVisibility(View.VISIBLE);
					}
					else {
						_optionsView.setVisibility(View.INVISIBLE);
					}
				}
			}
		});
		
		_startButton = ((Button)findViewById(R.id.startAdsButton));
		_startButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
		    	_statusImage = ((ImageView)findViewById(R.id.unityads_status));
		    	_statusImage.setVisibility(View.VISIBLE);
		    	UnityAds.setTestDeveloperId(((EditText)findViewById(R.id.developer_id_data)).getText().toString());
		    	UnityAds.setTestOptionsId(((EditText)findViewById(R.id.options_id_data)).getText().toString());
				UnityAds.init(_self, "16", _self);
				UnityAds.setListener(_self);
			}
		});
    }
    
    @Override
    public void onResume () {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onResume()");
    	super.onResume();
    	
   		UnityAds.changeActivity(this);
   		UnityAds.setListener(this);
    }
    
	@Override
	public boolean onCreateOptionsMenu (Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.layout.menu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected (MenuItem item) {
		switch (item.getItemId()) {
			case R.id.kill:
		    	System.runFinalizersOnExit(true);		
				finish();
		    	Log.d(_exampleAppLogTag, "Quitting");

		    	break;
		}
		
		return true;
	}
	
    @Override
	protected void onDestroy() {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onDestroy()");
    	super.onDestroy();		
	}
	
    @Override
	public void onHide () {
    }
    
    @Override
	public void onShow () {
    }
	
	// Unity Ads video events
    @Override
	public void onVideoStarted () {   	
    }
    
    @Override
	public void onVideoCompleted (String rewardItemKey, boolean skipped) {
    	if(skipped) {
    		Log.d(_exampleAppLogTag, "Video was skipped!");
    	}
    }
	
	// Unity Ads campaign events
    @Override
	public void onFetchCompleted () {
    	Log.d(_exampleAppLogTag, "UnityAdsTestStartActivity->onFetchCompleted()");
    	
    	_statusImage.setImageResource(R.drawable.unityads_loaded);
    	
    	_instructions = ((TextView)findViewById(R.id.instructionsText));
    	_instructions.setText(R.string.helpTextLoaded);
    	
    	_settingsButton.setEnabled(false);
    	_settingsButton.setVisibility(View.INVISIBLE);
    	_startButton.setEnabled(false);
    	_startButton.setVisibility(View.INVISIBLE);
    	_optionsView.setVisibility(View.INVISIBLE);
    	
    	_openButton = ((Button)findViewById(R.id.openAdsButton));
    	_openButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Open with options test
				Map<String, Object> optionsMap = new HashMap<String, Object>();
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_NOOFFERSCREEN_KEY, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_OPENANIMATED_KEY, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_GAMERSID_KEY, "gom");
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_MUTE_VIDEO_SOUNDS, false);
				optionsMap.put(UnityAds.UNITY_ADS_OPTION_VIDEO_USES_DEVICE_ORIENTATION, false);
				
				UnityAds.show(optionsMap);
				
				// Open without options (defaults)
				//UnityAds.show();
			}
		});
    	_openButton.setVisibility(View.VISIBLE);
	}
    
    @Override
    public void onFetchFailed () {
    }
}