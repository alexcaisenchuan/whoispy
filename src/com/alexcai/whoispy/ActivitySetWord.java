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
	private static final int MSG_FLAG_SEND_WORD_OK 				= 1;	//成功发出词汇
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR_SEND 	= 2;	//发出词汇提示
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR_SEND 	= 3;	//发出词汇提示
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR_READ 	= 4;	//读取推荐词汇提示
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR_READ	= 5;	//读取推荐词汇提示
	private static final int MSG_FLAG_NETWORK_ERR 				= 6;	//网络错误
	private static final int MSG_FLAG_NETWORK_TIMEOUT 			= 7;	//网络超时
	private static final int MSG_FLAG_COMMON_ERR				= 8;	//未知错误
	private static final int MSG_FLAG_SET_RECOMMEND_WORD		= 9;	//设置词汇
	private static final int MSG_FLAG_NONE						= 10;	//什么都不做
	private static final int MSG_FLAG_NOT_LOGIN					= 11;	//玩家未登录
	/*Bundle Key*/
	private static final String KEY_CIVIL_WORD = "civil_word";
	private static final String KEY_TRICK_WORD = "trick_word";
	
	/*--------------------------
	 * 属性
	 *------------------------*/
	/*界面元素*/
	private EditText text_civil_word;
	private EditText text_trick_word;
	/*界面元素*/
	private ProgressDialog mProgDialog_send;		//发送词汇提示
	private ProgressDialog mProgDialog_read;		//读取推荐词汇提示
	
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
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_star);
		
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
			}
		});
		//推荐词汇按钮
		ImageButton star_button = (ImageButton)findViewById(R.id.title_button_star);
		star_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				/*创建读取线程并启动*/
				Runnable r = new Runnable() {
					@Override
					public void run() {
						recommend_word();
					}
				};
				
				new Thread(r).start();
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
		//保存推荐词汇列表
		gameInfo.recommend_word.save_words_to_file();
		
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
	 * 推荐词汇
	 * */
	private void recommend_word() {
		/*判断是否需要连接网络*/
		if(gameInfo.recommend_word.need_to_read_remote_words())
		{
			/*打开进度条*/
			Message msg_open_progress_bar = new Message();
			msg_open_progress_bar.what = MSG_FLAG_OPEN_PROGRESS_BAR_READ;
			mHandler.sendMessage(msg_open_progress_bar);
			
			/*启动网络连接，读取词汇*/
			int ret = gameInfo.recommend_word.get_remote_word();
			
			/*根据运行结果进行提示*/
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
			
			/*关闭进度条*/
			Message msg_close_progreass_bar = new Message();
			msg_close_progreass_bar.what = MSG_FLAG_CLOSE_PROGRESS_BAR_READ;
			mHandler.sendMessage(msg_close_progreass_bar);
		}
		
		/*随机读取单词*/
		RecommendWord.WordPair word = gameInfo.recommend_word.new WordPair("", "");
		gameInfo.recommend_word.read_word(word);
		
		/*设置界面*/
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
	 * 启动游戏
	 * */
	private void game_start(){
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
					msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR_SEND;
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
							
						case Err.ERR_NOT_LOGIN:
							msg.what = MSG_FLAG_NOT_LOGIN;
							break;
							
						case Err.ERR_COMMON:
						default:
							msg.what = MSG_FLAG_COMMON_ERR;
							break;
					}
					mHandler.sendMessage(msg);
					
					/*关闭进度条*/
					Message msg_close = new Message();
					msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR_SEND;
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
			case MSG_FLAG_NONE:		//什么都不做
				break;
				
	        case MSG_FLAG_OPEN_PROGRESS_BAR_SEND:
	        	/*弹出进度条提示*/
	    		mProgDialog_send = new ProgressDialog(this);
	    		mProgDialog_send.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog_send.setMessage(getString(R.string.hint_send_words));
	    		mProgDialog_send.setCancelable(true);
	    		mProgDialog_send.show();
	        	break;
	        
	        case MSG_FLAG_CLOSE_PROGRESS_BAR_SEND:
	        	/*关闭进度条*/
	        	mProgDialog_send.dismiss();
	        	break;
	        	
	        case MSG_FLAG_OPEN_PROGRESS_BAR_READ:
	        	/*弹出进度条提示*/
	    		mProgDialog_read = new ProgressDialog(this);
	    		mProgDialog_read.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog_read.setMessage(getString(R.string.hint_read_recommend_word));
	    		mProgDialog_read.setCancelable(true);
	    		mProgDialog_read.show();
	        	break;
	        
	        case MSG_FLAG_CLOSE_PROGRESS_BAR_READ:
	        	/*关闭进度条*/
	        	mProgDialog_read.dismiss();
	        	break;
	        
	        case MSG_FLAG_SEND_WORD_OK:		//词汇成功发出，则启动游戏界面
	        	Intent i = new Intent(this, ActivityGameView.class);
	        	startActivity(i);
	        	break;
	        
	        case MSG_FLAG_SET_RECOMMEND_WORD:		//设置推荐词汇
	        	//读取参数
	        	Bundle b = msg.getData();
	        	String civil_word = b.getString(KEY_CIVIL_WORD);
	        	String trick_wodd = b.getString(KEY_TRICK_WORD);
	        	//设置界面元素
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
