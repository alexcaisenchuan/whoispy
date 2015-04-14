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
	private static final int MSG_FLAG_SEND_WORD_OK 				= 1;	//�ɹ������ʻ�
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR_SEND 	= 2;	//�����ʻ���ʾ
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR_SEND 	= 3;	//�����ʻ���ʾ
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR_READ 	= 4;	//��ȡ�Ƽ��ʻ���ʾ
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR_READ	= 5;	//��ȡ�Ƽ��ʻ���ʾ
	private static final int MSG_FLAG_NETWORK_ERR 				= 6;	//�������
	private static final int MSG_FLAG_NETWORK_TIMEOUT 			= 7;	//���糬ʱ
	private static final int MSG_FLAG_COMMON_ERR				= 8;	//δ֪����
	private static final int MSG_FLAG_SET_RECOMMEND_WORD		= 9;	//���ôʻ�
	private static final int MSG_FLAG_NONE						= 10;	//ʲô������
	private static final int MSG_FLAG_NOT_LOGIN					= 11;	//���δ��¼
	/*Bundle Key*/
	private static final String KEY_CIVIL_WORD = "civil_word";
	private static final String KEY_TRICK_WORD = "trick_word";
	
	/*--------------------------
	 * ����
	 *------------------------*/
	/*����Ԫ��*/
	private EditText text_civil_word;
	private EditText text_trick_word;
	/*����Ԫ��*/
	private ProgressDialog mProgDialog_send;		//���ʹʻ���ʾ
	private ProgressDialog mProgDialog_read;		//��ȡ�Ƽ��ʻ���ʾ
	
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
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_star);
		
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
			}
		});
		//�Ƽ��ʻ㰴ť
		ImageButton star_button = (ImageButton)findViewById(R.id.title_button_star);
		star_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*������ȡ�̲߳�����*/
				Runnable r = new Runnable() {
					@Override
					public void run() {
						recommend_word();
					}
				};
				
				new Thread(r).start();
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
		//�����Ƽ��ʻ��б�
		gameInfo.recommend_word.save_words_to_file();
		
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
	 * �Ƽ��ʻ�
	 * */
	private void recommend_word() {
		/*�ж��Ƿ���Ҫ��������*/
		if(gameInfo.recommend_word.need_to_read_remote_words())
		{
			/*�򿪽�����*/
			Message msg_open_progress_bar = new Message();
			msg_open_progress_bar.what = MSG_FLAG_OPEN_PROGRESS_BAR_READ;
			mHandler.sendMessage(msg_open_progress_bar);
			
			/*�����������ӣ���ȡ�ʻ�*/
			int ret = gameInfo.recommend_word.get_remote_word();
			
			/*�������н��������ʾ*/
			Message msg = new Message();
			switch(ret)
			{
				case Err.NO_ERR:
					msg.what = MSG_FLAG_NONE;
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
			Message msg_close_progreass_bar = new Message();
			msg_close_progreass_bar.what = MSG_FLAG_CLOSE_PROGRESS_BAR_READ;
			mHandler.sendMessage(msg_close_progreass_bar);
		}
		
		/*�����ȡ����*/
		RecommendWord.WordPair word = gameInfo.recommend_word.new WordPair("", "");
		gameInfo.recommend_word.read_word(word);
		
		/*���ý���*/
		Message msg_word = new Message();
		Bundle b = new Bundle();
		b.putString(KEY_CIVIL_WORD, word.civil_word);
		b.putString(KEY_TRICK_WORD, word.trick_word);
		msg_word.what = MSG_FLAG_SET_RECOMMEND_WORD;
		msg_word.setData(b);
		mHandler.sendMessage(msg_word);
		
		return;
	}
	
	/**
	 * ������Ϸ
	 * */
	private void game_start(){
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
					msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR_SEND;
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
							
						case Err.ERR_NOT_LOGIN:
							msg.what = MSG_FLAG_NOT_LOGIN;
							break;
							
						case Err.ERR_COMMON:
						default:
							msg.what = MSG_FLAG_COMMON_ERR;
							break;
					}
					mHandler.sendMessage(msg);
					
					/*�رս�����*/
					Message msg_close = new Message();
					msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR_SEND;
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
			case MSG_FLAG_NONE:		//ʲô������
				break;
				
	        case MSG_FLAG_OPEN_PROGRESS_BAR_SEND:
	        	/*������������ʾ*/
	    		mProgDialog_send = new ProgressDialog(this);
	    		mProgDialog_send.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog_send.setMessage(getString(R.string.hint_send_words));
	    		mProgDialog_send.setCancelable(true);
	    		mProgDialog_send.show();
	        	break;
	        
	        case MSG_FLAG_CLOSE_PROGRESS_BAR_SEND:
	        	/*�رս�����*/
	        	mProgDialog_send.dismiss();
	        	break;
	        	
	        case MSG_FLAG_OPEN_PROGRESS_BAR_READ:
	        	/*������������ʾ*/
	    		mProgDialog_read = new ProgressDialog(this);
	    		mProgDialog_read.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog_read.setMessage(getString(R.string.hint_read_recommend_word));
	    		mProgDialog_read.setCancelable(true);
	    		mProgDialog_read.show();
	        	break;
	        
	        case MSG_FLAG_CLOSE_PROGRESS_BAR_READ:
	        	/*�رս�����*/
	        	mProgDialog_read.dismiss();
	        	break;
	        
	        case MSG_FLAG_SEND_WORD_OK:		//�ʻ�ɹ���������������Ϸ����
	        	Intent i = new Intent(this, ActivityGameView.class);
	        	startActivity(i);
	        	break;
	        
	        case MSG_FLAG_SET_RECOMMEND_WORD:		//�����Ƽ��ʻ�
	        	//��ȡ����
	        	Bundle b = msg.getData();
	        	String civil_word = b.getString(KEY_CIVIL_WORD);
	        	String trick_wodd = b.getString(KEY_TRICK_WORD);
	        	//���ý���Ԫ��
	        	text_civil_word.setText(civil_word);
	        	text_trick_word.setText(trick_wodd);
	        	break;
	        	
	        case MSG_FLAG_NETWORK_ERR:
	        	Toast.makeText(this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_TIMEOUT:
	        	Toast.makeText(this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NOT_LOGIN:
	        	Toast.makeText(this, getString(R.string.hint_not_login), Toast.LENGTH_SHORT).show();
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
