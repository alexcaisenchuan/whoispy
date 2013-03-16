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
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityGameView extends ListActivity{
	/*--------------------------
	 * 常量
	 *------------------------*/
	static final String TAG = "whoispy.ActivityGameView";
	/*常量定义*/
	//玩家设置定义
	private final int SETTING_SMS_RESEND 			= 0;	//重新发短信
	private final int SETTING_PLAYER_MARK			= 1;	//标记玩家出局或恢复
	/*Msg类型*/
	private final int MSG_FLAG_SEND_WORD_OK 				= 1;	//成功发出词汇
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_SEND_SMS				= 6;	//未知错误
	
	/*--------------------------
	 * 属性
	 *------------------------*/
	/*界面元素*/
	private TextView civil_player_word;
	private TextView trick_player_word;
	private TextView text_round;
	private ProgressDialog mProgDialog;
	
	/*List相关*/
    private SimpleAdapter mAdapter;
    private ArrayList<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    //Map Key
    private final String KEY_INDEX = "index";
    private final String KEY_INFO = "info";
    private final String KEY_IMG = "img";
    
	/*主线程Handler*/
	private Handler mHandler;
	
	/*游戏相关信息*/
	private Game gameInfo;
	
	/*--------------------------
	 * 重写方法
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_game_view);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_game_view);
		
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
		gameInfo.game_set_step(Game.GAME_STEP_GAME_VIEW);
		
		/*绑定List*/
        mAdapter = new SimpleAdapter(this,
        							 mData,
        							 R.layout.list_set_player,
        							 new String[]{KEY_INDEX, KEY_IMG, KEY_INFO},
        							 new int[]{R.id.player_index, R.id.player_status_img, R.id.player_info});
        setListAdapter(mAdapter);
        
		/*绑定元素*/
		//再开一局按钮
		Button next_button = (Button)findViewById(R.id.replay_button);
		next_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gameInfo.game_replay();
				Intent i = new Intent(ActivityGameView.this, ActivitySetPlayer.class);
    			startActivity(i);
			}
		});
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityGameView.this, ActivitySetWord.class);
				startActivity(i);
				finish();		//要记得关闭当前Activity
				Log.d(TAG, "finish!");
			}
		});
		
		//回合
		text_round = (TextView)findViewById(R.id.round_game_view);
		text_round.setText(String.valueOf(gameInfo.game_getRound()));
		
		//平民词汇
		civil_player_word = (TextView)findViewById(R.id.civil_player_word_game_view);
		civil_player_word.setText(gameInfo.game_getCivilWord());
		
		//打酱油词汇
		trick_player_word = (TextView)findViewById(R.id.trick_player_word_game_view);
		if(gameInfo.game_getTrickNum() <= 0)		//若没有打酱油的，则隐藏界面元素
		{
			TextView text_trick = (TextView)findViewById(R.id.text_trick_player_word_game_view);
			text_trick.setVisibility(View.GONE);
			trick_player_word.setVisibility(View.GONE);
		}
		else
		{
			trick_player_word.setText(gameInfo.game_getTrickWord());
		}
		
		//检查是否有玩家
		Player player = gameInfo.player_get_first();
		while(player != null)
		{
			Log.d(TAG, "Set player! id : " + player.id 
					+ ", name : " + player.name 
					+ ", phone : " + player.phone_num
					+ ", role : " + player.role);
			
			//添加界面元素
			if(player.role != Player.ROLE_SHIELD)
			{
				layout_add_one_player(player);
			}
			
			//下一个玩家
			player = gameInfo.player_get_next();
		}
	}
	
	@Override
	protected void onPause() {
		//保存玩家列表到XML文件中
		gameInfo.data_savePlayerList();
		//保存游戏参数
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		try {
			/*读取玩家ID*/
			String index = mData.get(position).get(KEY_INDEX).toString();
			final int player_id = Integer.parseInt(index);
			final int list_index = position;
			
			/*根据玩家当前状态显示设置选项*/
			CharSequence[] items;
			if(Player.STATUS_OUT == gameInfo.player_get(player_id).status)		//玩家已出局
			{
				items = new CharSequence[]{
					getResources().getString(R.string.seting_resend_sms),
					getResources().getString(R.string.seting_player_mark_resume)
				};			
			}
			else																//玩家未出局
			{
				items = new CharSequence[]{
						getResources().getString(R.string.seting_resend_sms),
						getResources().getString(R.string.seting_player_mark_out)
					};
			}
			
			AlertDialog.Builder dlg = new AlertDialog.Builder(this);
			dlg.setTitle(R.string.player_setting);
			dlg.setItems(items,
					     new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which)
								{
									player_setting(which, player_id, list_index);
								}
						 });
			dlg.show();
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
		}
	}
	
	/*--------------------------
	 * 私有方法
	 *------------------------*/
	/**
	 * 添加一个玩家到界面上
	 * */
	private boolean layout_add_one_player(final Player player)
	{
		/*判断有效性*/
		if(player == null)
		{
			Log.d(TAG, "[layout_add_one_player()]: player == null , error!");
			return false;
		}
		
		/*设置新元素*/
		String role;		//角色名称
		switch(player.role)
		{
		case Player.ROLE_CIVIL:
			role = getResources().getString(R.string.role_civil);
			break;
			
		case Player.ROLE_SPY:
			role = getResources().getString(R.string.role_spy);
			break;
			
		case Player.ROLE_TRICK:
			role = getResources().getString(R.string.role_trick);
			break;
			
		default:
			role = getResources().getString(R.string.role_unknown);
			break;
		}
		
		/*设置新元素*/
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_INDEX, player.id);
		map.put(KEY_INFO, player.name + ":" + player.phone_num + "(" + role + ")");
		//根据玩家状态设置标记
		if(Player.STATUS_OUT == player.status)
		{
			map.put(KEY_IMG, R.drawable.out);		//显示标记
		}
		else
		{
			map.put(KEY_IMG, R.drawable.none);		//不显示图片
		}
		mData.add(map);
		
		/*通知列表适配器变化*/
		mAdapter.notifyDataSetChanged();
		
		return true;
	}
	
	/**
	 * 对玩家进行设置
	 * */
	private boolean player_setting(int which, int player_id, int list_index)
	{
		switch(which)
		{
			case SETTING_SMS_RESEND:
			{
				Player player = gameInfo.player_get(player_id);
				send_word(player);
				break;
			}
			
			case SETTING_PLAYER_MARK:
			{
				if(Player.STATUS_OUT == gameInfo.player_get(player_id).status)		//若出局，则设置为恢复
				{
					gameInfo.player_setStatus(player_id, Player.STATUS_NORMAL);
					Map<String, Object> map;
					map = mData.get(list_index);
					map.put(KEY_IMG, R.drawable.none);
					mAdapter.notifyDataSetChanged();
				}
				else																//若正常，则设置为出局
				{
					gameInfo.player_setStatus(player_id, Player.STATUS_OUT);
					Map<String, Object> map;
					map = mData.get(list_index);
					map.put(KEY_IMG, R.drawable.out);
					mAdapter.notifyDataSetChanged();
				}
				break;
			}
			
			default:
			{
				Log.d(TAG, "Unknown which : " + which);
				return false;
			}
		}
		
		return true;
	}
	
	private void send_word(final Player player) {
		
		//创建线程
		Runnable task_send_word = new Runnable() {
			
			@Override
			public void run() {
				/*显示进度条*/
				Message msg_open = new Message();
				msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
				mHandler.sendMessage(msg_open);
				
				/*访问网络，发出词汇*/
				int ret = gameInfo.player_send_word(player);
				
				Message msg = new Message();
				switch(ret)
				{
					case Err.ERR_NETWORK:
						msg.what = MSG_FLAG_NETWORK_ERR;
						break;
						
					case Err.ERR_NETWORK_TIMEOUT:
						msg.what = MSG_FLAG_NETWORK_TIMEOUT;
						break;
						
					case Err.ERR_COMMON:
						msg.what = MSG_FLAG_SEND_SMS;
						break;
						
					case Err.NO_ERR:
						msg.what = MSG_FLAG_SEND_WORD_OK;
						break;
					
					default:
						Log.d(TAG, "Unknown ret : " + ret);
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
	        
	        case MSG_FLAG_SEND_WORD_OK:
	        	Toast.makeText(this, getString(R.string.hint_send_word_success), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_ERR:
	        	Toast.makeText(this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_TIMEOUT:
	        	Toast.makeText(this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_SEND_SMS:
	        	Toast.makeText(this, getString(R.string.hint_send_sms), Toast.LENGTH_SHORT).show();
	        	break;
	        	
			default:
			{
				Log.d(TAG, "Unknown msg : " + msg.what);
				break;
			}
		}
	}
}
