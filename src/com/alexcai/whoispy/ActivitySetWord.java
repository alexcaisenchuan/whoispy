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
	 * 常量
	 *------------------------*/
	static final String TAG = "whoispy.ActivitySetWord";
	/*Msg类型*/
	private final int MSG_FLAG_SEND_WORD_OK 				= 1;	//成功发出词汇
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_COMMON_ERR			= 6;	//未知错误
	
	/*--------------------------
	 * 属性
	 *------------------------*/
	/*界面元素*/
	private EditText text_civil_word;
	private EditText text_trick_word;
	/*界面元素*/
	private ProgressDialog mProgDialog;
	
	//游戏相关信息
	private Game gameInfo;
	
	/*主线程Handler*/
	private Handler mHandler;
	
	/*--------------------------
	 * 方法
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_word);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_word);
		
		/*绑定Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				handler_msg(msg);
			};
		};
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*设置游戏阶段*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_WORD);
		
		/*绑定元素*/
		//查看游戏按钮，不重新分配身份，不发短信
		Button game_check_button = (Button)findViewById(R.id.game_check_button);
		game_check_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetWord.this, ActivityGameView.class);
				startActivity(i);
			}
		});
		//开始游戏按钮，重新分配身份，发短信
		Button game_start_button = (Button)findViewById(R.id.game_start_button);
		game_start_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				game_start();
			}
		});
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetWord.this, ActivitySetRoleNum.class);
				startActivity(i);
				finish();		//要记得关闭当前Activity
				Log.d(TAG, "finish!");
			}
		});
		
		//平民词汇
		text_civil_word = (EditText)findViewById(R.id.civil_player_word);
		text_civil_word.setText(gameInfo.game_getCivilWord());
		
		//打酱油词汇
		text_trick_word = (EditText)findViewById(R.id.trick_player_word);
		if(gameInfo.game_getTrickNum() <= 0)		//若没有打酱油的，则隐藏界面元素
		{
			TextView text_trick = (TextView)findViewById(R.id.text_trick_player_word);
			text_trick.setVisibility(View.GONE);
			text_trick_word.setVisibility(View.GONE);
		}
		else		//否则显示打酱油的词汇
		{
			text_trick_word.setText(gameInfo.game_getTrickWord());
		}
	}
	
	@Override
	protected void onPause() {
		//保存游戏参数
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	/**
	 * 检查输入的词汇是否合理
	 * */
	private boolean check_word_valid()
	{
		boolean valid;		//是否合法的标志
		String civil_word = text_civil_word.getText().toString();
		String trick_word = text_trick_word.getText().toString();
		
		valid = true;		//默认合法
		if(civil_word.length() == 0)		//若无平民词汇，则认为错
		{
			valid = false;
		}
		if((gameInfo.game_getTrickNum() > 0) && (trick_word.length() == 0))		//若有打酱油的但是每输入打酱油的词汇，则认为错
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
	 * 启动游戏
	 * */
	void game_start(){
		if(check_word_valid())				//检查是否输入了词汇
		{
			/* 设置游戏信息 */
			gameInfo.game_setting();
			
			/* 发出词汇 */
			//构建接口
			Runnable task_send_word = new Runnable() {
				@Override
				public void run() {
					/*显示进度条*/
					Message msg_open = new Message();
					msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
					mHandler.sendMessage(msg_open);
					
					/*发出词汇*/
					int ret = gameInfo.game_send_word_to_all();
					
					//创建消息并发出，完成与主线程的互动
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
					
					/*关闭进度条*/
					Message msg_close = new Message();
					msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
					mHandler.sendMessage(msg_close);
				}
			};
			//启动线程
			new Thread(task_send_word).start();
		}
		
		return;
	}
	
	/**
	 * 主线程Handler处理msg的函数
	 * */
	private void handler_msg(Message msg){
		switch(msg.what)
		{
	        case MSG_FLAG_OPEN_PROGRESS_BAR:
	        	/*弹出进度条提示*/
	    		mProgDialog = new ProgressDialog(this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_send_words));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*关闭进度条*/
				mProgDialog.dismiss();
	        	break;
	        
	        case MSG_FLAG_SEND_WORD_OK:		//词汇成功发出，则启动游戏界面
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
