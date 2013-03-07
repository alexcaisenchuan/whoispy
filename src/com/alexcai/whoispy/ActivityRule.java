package com.alexcai.whoispy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;

public class ActivityRule extends Activity{
	private String TAG = "whoispy.ActivityHelp";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_rule);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_rule);

		Log.d(TAG, "onCreate");
		
		/*绑定界面元素*/
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityRule.this, ActivityMenu.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//清空Activity栈中ActivityMain之上的Activity
				startActivity(i);
				finish();		//要记得关闭当前Activity
				Log.d(TAG, "finish!");
			}
		});
		
		/*启动WEB界面，显示网页文件*/
		WebView web = (WebView)findViewById(R.id.content_rule);
		web.loadUrl("file:///android_asset/rule.html");
		
	}
}
