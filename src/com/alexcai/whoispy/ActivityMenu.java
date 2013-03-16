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

	//��Ϸ�����Ϣ
	private Game gameInfo;
	
	//����
	private long time_exit = 0;		//��¼�ϴΰ��º��˼���ʱ��
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_menu);
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*���ð汾��*/
		TextView version_text = (TextView)findViewById(R.id.version_text);
		version_text.setText(getVersion());
		
		/*�󶨰�ť���*/
		//��ȡ�ʻ�
		Button word_display_button = (Button)findViewById(R.id.word_display_button);
		word_display_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityWordDisplay.class);
				startActivity(i);
			}
		});
		//�����ʻ�
		Button word_send_button = (Button)findViewById(R.id.word_send_button);
		word_send_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//��ȡ�������Ϸ��Ϣ
				gameInfo.game_continue();
				
				//��ȡ��Ϸ�ϴα���Ľ׶�
				int step = gameInfo.game_get_step();
				
				//��ѡActivity
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
						
					default:		//Ĭ�Ͻ���������ý���
						activityClass = ActivitySetPlayer.class;
						break;
				}
				
				//����Activity
				Intent i = new Intent(ActivityMenu.this, activityClass);
    			startActivity(i);
			}
		});
		//��Ϸ����
		Button rule_button = (Button)findViewById(R.id.rule_button);
		rule_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityRule.class);
				startActivity(i);
			}
		});
		//����
		Button about_button = (Button)findViewById(R.id.about_button);
		about_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityMenu.this, ActivityAbout.class);
				startActivity(i);
			}
		});
		
		//�ж��Ƿ�Ҫ����
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
			if((time_current - time_exit) > 2000)		//�ж����ΰ������
			{
				//��ʾ��ʾ��Ϣ
				Toast.makeText(getApplicationContext(), getResources().getString(R.string.hint_exit), Toast.LENGTH_SHORT).show();
				//��¼���ΰ��µ�ʱ��
				time_exit = time_current;
			}
			else
			{
				//�˳�����
				finish();		//finish����ֻ���˳�Activity
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
    /**����˵�������ʾ���ǵ����ò˵�*/
    public boolean onCreateOptionsMenu(Menu menu) {
    	super.onCreateOptionsMenu(menu);
    	
    	MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_logout, menu);
        
        return true;
    }
    
    @Override
    /**ѡ��ĳ���˵���ʱ����*/
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId())
    	{
	    	case R.id.logout:
	    		ad_show();		//����ѯ���Ƿ��˳�
	    		return true;
	    		
	    	default:
	    		break;
    	}
    	
    	return false;
    }
    
	/*-----------------------------
	 * �Զ��巽��
	 * ---------------------------*/
	/**
	 * ��ʾѯ�ʽ���
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
						//�˳���¼
						logout();
					}
		});
		b.setNegativeButton(
				getString(R.string.button_cancel), 
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						//ʲô������
					}
		});
		
		AlertDialog ad = b.create();
		ad.show();
	}
	
	/**
	 * ��ʾѯ�ʽ���
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
						//ʲô������
					}
		});
		
		AlertDialog ad = b.create();
		ad.show();
	}
	
	/**
	 * �ǳ�
	 * */
	private void logout() {
		//���õǳ�����
		gameInfo.user.logout();
		//ɾ���ʻ�����
		gameInfo.data_delWordList();
		//�رյ�ǰ����
		ActivityMenu.this.finish();
		//������½����
		Intent i = new Intent(ActivityMenu.this, ActivityLogin.class);
		startActivity(i);
	}
	
	/**
	 * ��ȡ�汾��
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
