package com.alexcai.whoispy;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityMenu extends Activity {
	private String TAG = "whoispy.ActivityMenu";

	//游戏相关信息
	private Game gameInfo;
	
	//其他
	private long time_exit = 0;		//记录上次按下后退键的时间
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*设置版本号*/
		TextView version_text = (TextView)findViewById(R.id.version_text);
		version_text.setText(getVersion());
		
		/*绑定按钮点击*/
		//读取词汇
		Button word_display_button = (Button)findViewById(R.id.word_display_button);
		word_display_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityWordDisplay.class);
				startActivity(i);
			}
		});
		//发出词汇
		Button word_send_button = (Button)findViewById(R.id.word_send_button);
		word_send_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//读取保存的游戏信息
				gameInfo.game_continue();
				
				//读取游戏上次保存的阶段
				int step = gameInfo.game_get_step();
				
				//挑选Activity
				Class<?> activityClass;
				switch(step)
				{
					case Game.GAME_STEP_SET_PLAYER:
						activityClass = ActivitySetPlayer.class;
						break;
					
					case Game.GAME_STEP_SET_ROLE_NUM:
						activityClass = ActivitySetRoleNum.class;
						break;
						
					case Game.GAME_STEP_SET_WORD:
						activityClass = ActivitySetWord.class;
						break;
						
					case Game.GAME_STEP_GAME_VIEW:
						activityClass = ActivityGameView.class;
						break;
						
					default:		//默认进入玩家设置界面
						activityClass = ActivitySetPlayer.class;
						break;
				}
				
				//启动Activity
				Intent i = new Intent(ActivityMenu.this, activityClass);
    			startActivity(i);
			}
		});
		//游戏规则
		Button rule_button = (Button)findViewById(R.id.rule_button);
		rule_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityRule.class);
				startActivity(i);
			}
		});
		//关于
		Button about_button = (Button)findViewById(R.id.about_button);
		about_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityAbout.class);
				startActivity(i);
			}
		});
		
		//判断是否要升级
		if(gameInfo.want_update && gameInfo.sys_need_update())
		{
			update_ad_show();
			gameInfo.want_update = false;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
		{
			long time_current = System.currentTimeMillis();
			if((time_current - time_exit) > 2000)		//判断两次按键间隔
			{
				//显示提示信息
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.hint_exit), Toast.LENGTH_SHORT).show();
				//记录本次按下的时间
				time_exit = time_current;
			}
			else
			{
				//退出程序
				finish();		//finish仅仅只是退出Activity
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
    /**点击菜单键后显示我们的设置菜单*/
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_logout, menu);
        
        return true;
    }
    
    @Override
    /**选择某个菜单项时调用*/
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId())
    	{
	    	case R.id.logout:
	    		ad_show();		//弹窗询问是否退出
	    		return true;
	    		
	    	default:
	    		break;
    	}
    	
    	return false;
    }
    
	/*-----------------------------
	 * 自定义方法
	 * ---------------------------*/
	/**
	 * 显示询问界面
	 * */
	private void ad_show() {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.hint_logout);
		b.setIcon(android.R.drawable.ic_dialog_info);
		b.setPositiveButton(
				getString(R.string.button_ok), 
				new DialogInterface.OnClickListener() {			
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//退出登录
						logout();
					}
		});
		b.setNegativeButton(
				getString(R.string.button_cancel), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//什么都不做
					}
		});
		
		AlertDialog ad = b.create();
		ad.show();
	}
	
	/**
	 * 显示询问界面
	 * */
	private void update_ad_show() {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setTitle(R.string.hint_update);
		b.setIcon(android.R.drawable.ic_dialog_info);
		b.setPositiveButton(
				getString(R.string.button_ok), 
				new DialogInterface.OnClickListener() {			
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse("http://zhuoguiyouxi.com/whoispy_front/files/whoispy.apk");
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						startActivity(intent);
					}
		});
		b.setNegativeButton(
				getString(R.string.button_cancel), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//什么都不做
					}
		});
		
		AlertDialog ad = b.create();
		ad.show();
	}
	
	/**
	 * 登出
	 * */
	private void logout() {
		//调用登出函数
		gameInfo.user.logout();
		//删除词汇数据
		gameInfo.data_delWordList();
		//关闭当前界面
		ActivityMenu.this.finish();
		//启动登陆界面
		Intent i = new Intent(ActivityMenu.this, ActivityLogin.class);
		startActivity(i);
	}
	
	/**
	 * 读取版本号
	 * */
	private String getVersion() {
		PackageManager pm = getPackageManager();
		PackageInfo pi;
		String version;
		try {
			pi = pm.getPackageInfo(getPackageName(), 0);
			version = pi.versionName;
		} catch (NameNotFoundException e) {
			Log.d(TAG, "Exception : " + e.toString());
			e.printStackTrace();
			version = "";
		}
		
		return version;
	}
}
