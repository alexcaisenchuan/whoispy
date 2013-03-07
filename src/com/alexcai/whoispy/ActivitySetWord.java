package com.alexcai.whoispy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySetWord extends Activity{
	/*--------------------------
	 * ����
	 *------------------------*/
	static final String TAG = "whoispy.ActivitySetWord";
	/*Msg����*/
	private final int MSG_FLAG_SEND_WORD_OK 				= 1;	//�ɹ������ʻ�
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_COMMON_ERR			= 6;	//δ֪����
	
	/*--------------------------
	 * ����
	 *------------------------*/
	/*����Ԫ��*/
	private EditText text_civil_word;
	private EditText text_trick_word;
	/*����Ԫ��*/
	private ProgressDialog mProgDialog;
	
	//��Ϸ�����Ϣ
	private Game gameInfo;
	
	/*���߳�Handler*/
	private Handler mHandler;
	
	/*--------------------------
	 * ����
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*���ý���*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_word);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l);
		
		/*���ñ�������*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_word);
		
		/*��Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				handler_msg(msg);
			};
		};
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*������Ϸ�׶�*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_WORD);
		
		/*��Ԫ��*/
		//�鿴��Ϸ��ť�������·�����ݣ���������
		Button game_check_button = (Button)findViewById(R.id.game_check_button);
		game_check_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetWord.this, ActivityGameView.class);
				startActivity(i);
			}
		});
		//��ʼ��Ϸ��ť�����·�����ݣ�������
		Button game_start_button = (Button)findViewById(R.id.game_start_button);
		game_start_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				game_start();
			}
		});
		//��һ����ť
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetWord.this, ActivitySetRoleNum.class);
				startActivity(i);
				finish();		//Ҫ�ǵùرյ�ǰActivity
				Log.d(TAG, "finish!");
			}
		});
		
		//ƽ��ʻ�
		text_civil_word = (EditText)findViewById(R.id.civil_player_word);
		text_civil_word.setText(gameInfo.game_getCivilWord());
		
		//���ʹʻ�
		text_trick_word = (EditText)findViewById(R.id.trick_player_word);
		if(gameInfo.game_getTrickNum() <= 0)		//��û�д��͵ģ������ؽ���Ԫ��
		{
			TextView text_trick = (TextView)findViewById(R.id.text_trick_player_word);
			text_trick.setVisibility(View.GONE);
			text_trick_word.setVisibility(View.GONE);
		}
		else		//������ʾ���͵Ĵʻ�
		{
			text_trick_word.setText(gameInfo.game_getTrickWord());
		}
	}
	
	@Override
	protected void onPause() {
		//������Ϸ����
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	/**
	 * �������Ĵʻ��Ƿ����
	 * */
	private boolean check_word_valid()
	{
		boolean valid;		//�Ƿ�Ϸ��ı�־
		String civil_word = text_civil_word.getText().toString();
		String trick_word = text_trick_word.getText().toString();
		
		valid = true;		//Ĭ�ϺϷ�
		if(civil_word.length() == 0)		//����ƽ��ʻ㣬����Ϊ��
		{
			valid = false;
		}
		if((gameInfo.game_getTrickNum() > 0) && (trick_word.length() == 0))		//���д��͵ĵ���ÿ������͵Ĵʻ㣬����Ϊ��
		{
			valid = false;
		}
		
		if(valid)
		{
			gameInfo.game_setCivilWord(civil_word);
			gameInfo.game_setTrickWord(trick_word);
		}
		else
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_word_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
		}
		
		return valid;
	}
	
	/**
	 * ������Ϸ
	 * */
	void game_start(){
		if(check_word_valid())				//����Ƿ������˴ʻ�
		{
			/* ������Ϸ��Ϣ */
			gameInfo.game_setting();
			
			/* �����ʻ� */
			//�����ӿ�
			Runnable task_send_word = new Runnable() {
				@Override
				public void run() {
					/*��ʾ������*/
					Message msg_open = new Message();
					msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
					mHandler.sendMessage(msg_open);
					
					/*�����ʻ�*/
					int ret = gameInfo.game_send_word_to_all();
					
					//������Ϣ����������������̵߳Ļ���
					Message msg = new Message();
					switch(ret)
					{
						case Err.NO_ERR:
							msg.what = MSG_FLAG_SEND_WORD_OK;
							break;
							
						case Err.ERR_NETWORK:
							msg.what = MSG_FLAG_NETWORK_ERR;
							break;
							
						case Err.ERR_NETWORK_TIMEOUT:
							msg.what = MSG_FLAG_NETWORK_TIMEOUT;
							break;
							
						case Err.ERR_COMMON:
						default:
							msg.what = MSG_FLAG_COMMON_ERR;
							break;
					}
					mHandler.sendMessage(msg);
					
					/*�رս�����*/
					Message msg_close = new Message();
					msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
					mHandler.sendMessage(msg_close);
				}
			};
			//�����߳�
			new Thread(task_send_word).start();
		}
		
		return;
	}
	
	/**
	 * ���߳�Handler����msg�ĺ���
	 * */
	private void handler_msg(Message msg){
		switch(msg.what)
		{
	        case MSG_FLAG_OPEN_PROGRESS_BAR:
	        	/*������������ʾ*/
	    		mProgDialog = new ProgressDialog(this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_send_words));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*�رս�����*/
				mProgDialog.dismiss();
	        	break;
	        
	        case MSG_FLAG_SEND_WORD_OK:		//�ʻ�ɹ���������������Ϸ����
	        	Intent i = new Intent(this, ActivityGameView.class);
	        	startActivity(i);
	        	break;
	        	
	        case MSG_FLAG_NETWORK_ERR:
	        	Toast.makeText(this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_TIMEOUT:
	        	Toast.makeText(this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_COMMON_ERR:
	        	Toast.makeText(this, getString(R.string.hint_common_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
			default:
			{
				Log.d(TAG, "Unknown msg : " + msg.what);
				break;
			}
		}
	}
}
