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
	 * ����
	 *------------------------*/
	static final String TAG = "whoispy.ActivityGameView";
	/*��������*/
	//������ö���
	private final int SETTING_SMS_RESEND 			= 0;	//���·�����
	private final int SETTING_PLAYER_MARK			= 1;	//�����ҳ��ֻ�ָ�
	/*Msg����*/
	private final int MSG_FLAG_SEND_WORD_OK 				= 1;	//�ɹ������ʻ�
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_SEND_SMS				= 6;	//δ֪����
	
	/*--------------------------
	 * ����
	 *------------------------*/
	/*����Ԫ��*/
	private TextView civil_player_word;
	private TextView trick_player_word;
	private TextView text_round;
	private ProgressDialog mProgDialog;
	
	/*List���*/
    private SimpleAdapter mAdapter;
    private ArrayList<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    //Map Key
    private final String KEY_INDEX = "index";
    private final String KEY_INFO = "info";
    private final String KEY_IMG = "img";
    
	/*���߳�Handler*/
	private Handler mHandler;
	
	/*��Ϸ�����Ϣ*/
	private Game gameInfo;
	
	/*--------------------------
	 * ��д����
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*���ý���*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_game_view);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l);
		
		/*���ñ�������*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_game_view);
		
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
		gameInfo.game_set_step(Game.GAME_STEP_GAME_VIEW);
		
		/*��List*/
        mAdapter = new SimpleAdapter(this,
        							 mData,
        							 R.layout.list_set_player,
        							 new String[]{KEY_INDEX, KEY_IMG, KEY_INFO},
        							 new int[]{R.id.player_index, R.id.player_status_img, R.id.player_info});
        setListAdapter(mAdapter);
        
		/*��Ԫ��*/
		//�ٿ�һ�ְ�ť
		Button next_button = (Button)findViewById(R.id.replay_button);
		next_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				gameInfo.game_replay();
				Intent i = new Intent(ActivityGameView.this, ActivitySetPlayer.class);
    			startActivity(i);
			}
		});
		//��һ����ť
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityGameView.this, ActivitySetWord.class);
				startActivity(i);
				finish();		//Ҫ�ǵùرյ�ǰActivity
				Log.d(TAG, "finish!");
			}
		});
		
		//�غ�
		text_round = (TextView)findViewById(R.id.round_game_view);
		text_round.setText(String.valueOf(gameInfo.game_getRound()));
		
		//ƽ��ʻ�
		civil_player_word = (TextView)findViewById(R.id.civil_player_word_game_view);
		civil_player_word.setText(gameInfo.game_getCivilWord());
		
		//���ʹʻ�
		trick_player_word = (TextView)findViewById(R.id.trick_player_word_game_view);
		if(gameInfo.game_getTrickNum() <= 0)		//��û�д��͵ģ������ؽ���Ԫ��
		{
			TextView text_trick = (TextView)findViewById(R.id.text_trick_player_word_game_view);
			text_trick.setVisibility(View.GONE);
			trick_player_word.setVisibility(View.GONE);
		}
		else
		{
			trick_player_word.setText(gameInfo.game_getTrickWord());
		}
		
		//����Ƿ������
		Player player = gameInfo.player_get_first();
		while(player != null)
		{
			Log.d(TAG, "Set player! id : " + player.id 
					+ ", name : " + player.name 
					+ ", phone : " + player.phone_num
					+ ", role : " + player.role);
			
			//��ӽ���Ԫ��
			if(player.role != Player.ROLE_SHIELD)
			{
				layout_add_one_player(player);
			}
			
			//��һ�����
			player = gameInfo.player_get_next();
		}
	}
	
	@Override
	protected void onPause() {
		//��������б�XML�ļ���
		gameInfo.data_savePlayerList();
		//������Ϸ����
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		try {
			/*��ȡ���ID*/
			String index = mData.get(position).get(KEY_INDEX).toString();
			final int player_id = Integer.parseInt(index);
			final int list_index = position;
			
			/*������ҵ�ǰ״̬��ʾ����ѡ��*/
			CharSequence[] items;
			if(Player.STATUS_OUT == gameInfo.player_get(player_id).status)		//����ѳ���
			{
				items = new CharSequence[]{
					getResources().getString(R.string.seting_resend_sms),
					getResources().getString(R.string.seting_player_mark_resume)
				};			
			}
			else																//���δ����
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
	 * ˽�з���
	 *------------------------*/
	/**
	 * ���һ����ҵ�������
	 * */
	private boolean layout_add_one_player(final Player player)
	{
		/*�ж���Ч��*/
		if(player == null)
		{
			Log.d(TAG, "[layout_add_one_player()]: player == null , error!");
			return false;
		}
		
		/*������Ԫ��*/
		String role;		//��ɫ����
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
		
		/*������Ԫ��*/
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_INDEX, player.id);
		map.put(KEY_INFO, player.name + ":" + player.phone_num + "(" + role + ")");
		//�������״̬���ñ��
		if(Player.STATUS_OUT == player.status)
		{
			map.put(KEY_IMG, R.drawable.out);		//��ʾ���
		}
		else
		{
			map.put(KEY_IMG, R.drawable.none);		//����ʾͼƬ
		}
		mData.add(map);
		
		/*֪ͨ�б��������仯*/
		mAdapter.notifyDataSetChanged();
		
		return true;
	}
	
	/**
	 * ����ҽ�������
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
				if(Player.STATUS_OUT == gameInfo.player_get(player_id).status)		//�����֣�������Ϊ�ָ�
				{
					gameInfo.player_setStatus(player_id, Player.STATUS_NORMAL);
					Map<String, Object> map;
					map = mData.get(list_index);
					map.put(KEY_IMG, R.drawable.none);
					mAdapter.notifyDataSetChanged();
				}
				else																//��������������Ϊ����
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
		
		//�����߳�
		Runnable task_send_word = new Runnable() {
			
			@Override
			public void run() {
				/*��ʾ������*/
				Message msg_open = new Message();
				msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
				mHandler.sendMessage(msg_open);
				
				/*�������磬�����ʻ�*/
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
				
				/*�رս�����*/
				Message msg_close = new Message();
				msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
				mHandler.sendMessage(msg_close);
			}
		};
		//�����߳�
		new Thread(task_send_word).start();
		
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
