package com.alexcai.whoispy;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
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
		
		Log.d(TAG, "onCreate");
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
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
		//�˳���¼
		Button logout_button = (Button)findViewById(R.id.logout_button);
		logout_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ad_show();		//����ѯ���Ƿ��˳�
			}
		});
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
}
