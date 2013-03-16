package com.alexcai.whoispy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityWordDisplay extends ListActivity{
	/*------------------------------
	 * 常量
	 * */
	private static final String TAG = "whoispy.ActivityWordDisplay";
	/*列表的KEY*/
	private final String KEY_WORD = "word";		//词汇
	private final String KEY_SENDER = "sender";	//发词人
	private final String KEY_TIME = "time";		//发词时间
	/*Msg类型*/
	private final int MSG_FLAG_LISTVIEW_REFRESH 			= 1;	//更新ListView
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_COMMON_ERR			= 6;	//未知错误
	
	/*--------------------------
	 * 属性
	 * */
	/*游戏相关信息*/
	private Game gameInfo;
	/*词汇列表*/
	private ArrayList<OneWord> word_list = new ArrayList<OneWord>();
	/*ListView相关*/
	private SimpleAdapter mAdapt;
	private ArrayList<Map<String, Object>> mArray = new ArrayList<Map<String,Object>>(); 
	/*主线程Handler*/
	private Handler mHandler;
	/*界面元素*/
	private ProgressDialog mProgDialog;
	
	/*--------------------------
	 * 自定义类
	 * */
	/*读取线程*/
	class ThreadReadRemoteWord implements Runnable{
		@Override
		public void run() {
			read_remote_word();
		}
	}
	
	/*--------------------------
	 * 方法
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_word_display);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_recycle);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_word_display);
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*绑定List*/
		mAdapt = new SimpleAdapter(this, 
								   mArray,
								   R.layout.list_word_display,
								   new String[]{KEY_WORD, KEY_SENDER, KEY_TIME},
								   new int[]{R.id.word, R.id.sender, R.id.time});
		setListAdapter(mAdapt);
		
		/*绑定Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				handler_msg(msg);
			};
		};
		
		/*绑定界面元素*/
		//刷新按钮
		Button button_refresh = (Button)findViewById(R.id.refresh_button);
		button_refresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new Thread(new ThreadReadRemoteWord()).start();
			}
		});
		
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityWordDisplay.this, ActivityMenu.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//清空Activity栈中ActivityMain之上的Activity
				startActivity(i);
				finish();		//要记得关闭当前Activity
			}
		});
		
		//删除词汇按钮
		ImageButton recycle_button =  (ImageButton)findViewById(R.id.title_button_recycle);
		recycle_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//弹出提示窗口询问是否确认删除词汇
				ad_show();
			}
		});
		
		/*读取词汇*/
		read_local_word();									//读取本地词汇
		new Thread(new ThreadReadRemoteWord()).start();		//启动线程，读取远程词汇
	}
	
	@Override
	protected void onPause() {
		//保存词汇列表
		save_word_to_file();
		
		super.onPause();
	}
	
	/*-----------------------------
	 * 自定义方法
	 * ---------------------------*/
	/**
	 * 读取本地存储的词汇
	 * */
	private void read_local_word() {
		try{
			/*读取词汇列表*/
			ArrayList<OneWord> word_list = gameInfo.data_readWordList();
			
			/*将词汇列表添加到原来的列表中，并且显示到界面上*/
			add_word_list(word_list);
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
		}
	}
	
	/**
	 * 读取服务器上的词汇
	 * */
	private void read_remote_word() {
		/*显示进度条*/
		Message msg_open = new Message();
		msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
		mHandler.sendMessage(msg_open);
		
		try {
			/*读取词汇列表*/
			ArrayList<OneWord> word_list = gameInfo.user.get_all_word();
			
			/*将词汇添加到原来的列表中，并且显示在界面上*/
			add_word_list(word_list);
			
			/*删除远程数据库中的各个词汇*/
			//gameInfo.user.del_word(word_list);		//在服务端已经把读到的词汇删除了
		}
		catch(Err.ExceptionNetwork e)
		{
			Message msg = new Message();
			msg.what = MSG_FLAG_NETWORK_ERR;
			mHandler.sendMessage(msg);
			
			Log.d(TAG, "Exception : " + e.toString());
		}
		catch(Err.ExceptionNetworkTimeOut e)
		{
			Message msg = new Message();
			msg.what = MSG_FLAG_NETWORK_TIMEOUT;
			mHandler.sendMessage(msg);
			
			Log.d(TAG, "Exception : " + e.toString());
		}
		catch(Err.ExceptionCommon e)
		{
			Message msg = new Message();
			msg.what = MSG_FLAG_COMMON_ERR;
			mHandler.sendMessage(msg);
			
			Log.d(TAG, "Exception : " + e.toString());
		}
		
		/*关闭进度条*/
		Message msg_close = new Message();
		msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
		mHandler.sendMessage(msg_close);
	}
	
	/**
	 * 将词汇保存到本地文件中
	 * */
	private void save_word_to_file() {
		gameInfo.data_saveWordList(this.word_list);
	}
	
	/**
	 * 删除所有词汇
	 * */
	private void del_all_word() {
		//删除Adapter对应数组中的词汇
		mArray.removeAll(mArray);
		mAdapt.notifyDataSetChanged();		//更新界面
		
		//删除word_list中的词汇
		this.word_list.removeAll(word_list);
	}
	
	/**
	 * 将词汇添加到原来的列表中，并且显示在界面上
	 * */
	private void add_word_list(ArrayList<OneWord> word_list){
		
		for(OneWord element : word_list)
		{
			/*添加到原来的列表中*/
			this.word_list.add(element);
			
			/*发出Msg，更新界面*/
			Message msg = new Message();
			msg.what = MSG_FLAG_LISTVIEW_REFRESH;		//信息类型
			Bundle b = new Bundle();					//信息附带参数
			b.putString(KEY_WORD, element.word);
			b.putString(KEY_SENDER, element.sender);
			b.putString(KEY_TIME, element.time);
			msg.setData(b);
			mHandler.sendMessage(msg);					//发出信息
		}
		
		return;
	}
	
	/**
	 * 显示询问界面
	 * */
	private void ad_show(){
		/*创建提示窗口*/
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_del_words));
		builder.setIcon(android.R.drawable.ic_dialog_alert);		//使用Android内置图标
		builder.setPositiveButton(			//设置确认按钮
			getString(R.string.button_ok), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//删除所有词汇
					del_all_word();
				}
		});
		builder.setNegativeButton(			//设置取消按钮
			getString(R.string.button_cancel), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//什么都不做
				}
		});
		
		/*显示提示窗口*/
		AlertDialog ad = builder.create();;
		ad.show();
	}
	
	/**
	 * 主线程Handler处理msg的函数
	 * */
	private void handler_msg(Message msg){
		switch(msg.what)
		{
			case MSG_FLAG_LISTVIEW_REFRESH:
			{
				//读取Bundle数据
				Bundle b = msg.getData();
				String word = b.getString(KEY_WORD);
				String sender = b.getString(KEY_SENDER);
				String time = b.getString(KEY_TIME);
				
				//1. 构造数据
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_WORD, word);
				map.put(KEY_SENDER, sender);
				map.put(KEY_TIME, time);
				
				//2. 添加数据
				mArray.add(map);
				
				//3. 通知适配器更新界面
				mAdapt.notifyDataSetChanged();
				
				break;
			}
			
	        case MSG_FLAG_OPEN_PROGRESS_BAR:
	        	/*弹出进度条提示，显示“正在登录”*/
	    		mProgDialog = new ProgressDialog(ActivityWordDisplay.this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_read_words));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*关闭进度条*/
				mProgDialog.dismiss();
	        	break;
	        
	        case MSG_FLAG_NETWORK_ERR:
	        	Toast.makeText(ActivityWordDisplay.this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_TIMEOUT:
	        	Toast.makeText(ActivityWordDisplay.this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_COMMON_ERR:
	        	Toast.makeText(ActivityWordDisplay.this, getString(R.string.hint_common_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
			default:
			{
				Log.d(TAG, "Unknown msg : " + msg.what);
				break;
			}
		}
	}
}
