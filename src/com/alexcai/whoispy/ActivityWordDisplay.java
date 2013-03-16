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
	 * ����
	 * */
	private static final String TAG = "whoispy.ActivityWordDisplay";
	/*�б��KEY*/
	private final String KEY_WORD = "word";		//�ʻ�
	private final String KEY_SENDER = "sender";	//������
	private final String KEY_TIME = "time";		//����ʱ��
	/*Msg����*/
	private final int MSG_FLAG_LISTVIEW_REFRESH 			= 1;	//����ListView
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 	= 2;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 	= 3;
	private static final int MSG_FLAG_NETWORK_ERR 			= 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT 		= 5;
	private static final int MSG_FLAG_COMMON_ERR			= 6;	//δ֪����
	
	/*--------------------------
	 * ����
	 * */
	/*��Ϸ�����Ϣ*/
	private Game gameInfo;
	/*�ʻ��б�*/
	private ArrayList<OneWord> word_list = new ArrayList<OneWord>();
	/*ListView���*/
	private SimpleAdapter mAdapt;
	private ArrayList<Map<String, Object>> mArray = new ArrayList<Map<String,Object>>(); 
	/*���߳�Handler*/
	private Handler mHandler;
	/*����Ԫ��*/
	private ProgressDialog mProgDialog;
	
	/*--------------------------
	 * �Զ�����
	 * */
	/*��ȡ�߳�*/
	class ThreadReadRemoteWord implements Runnable{
		@Override
		public void run() {
			read_remote_word();
		}
	}
	
	/*--------------------------
	 * ����
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*���ý���*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_word_display);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_recycle);
		
		/*���ñ�������*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_word_display);
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*��List*/
		mAdapt = new SimpleAdapter(this, 
								   mArray,
								   R.layout.list_word_display,
								   new String[]{KEY_WORD, KEY_SENDER, KEY_TIME},
								   new int[]{R.id.word, R.id.sender, R.id.time});
		setListAdapter(mAdapt);
		
		/*��Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				handler_msg(msg);
			};
		};
		
		/*�󶨽���Ԫ��*/
		//ˢ�°�ť
		Button button_refresh = (Button)findViewById(R.id.refresh_button);
		button_refresh.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				new Thread(new ThreadReadRemoteWord()).start();
			}
		});
		
		//��һ����ť
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivityWordDisplay.this, ActivityMenu.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//���Activityջ��ActivityMain֮�ϵ�Activity
				startActivity(i);
				finish();		//Ҫ�ǵùرյ�ǰActivity
			}
		});
		
		//ɾ���ʻ㰴ť
		ImageButton recycle_button =  (ImageButton)findViewById(R.id.title_button_recycle);
		recycle_button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//������ʾ����ѯ���Ƿ�ȷ��ɾ���ʻ�
				ad_show();
			}
		});
		
		/*��ȡ�ʻ�*/
		read_local_word();									//��ȡ���شʻ�
		new Thread(new ThreadReadRemoteWord()).start();		//�����̣߳���ȡԶ�̴ʻ�
	}
	
	@Override
	protected void onPause() {
		//����ʻ��б�
		save_word_to_file();
		
		super.onPause();
	}
	
	/*-----------------------------
	 * �Զ��巽��
	 * ---------------------------*/
	/**
	 * ��ȡ���ش洢�Ĵʻ�
	 * */
	private void read_local_word() {
		try{
			/*��ȡ�ʻ��б�*/
			ArrayList<OneWord> word_list = gameInfo.data_readWordList();
			
			/*���ʻ��б���ӵ�ԭ�����б��У�������ʾ��������*/
			add_word_list(word_list);
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
		}
	}
	
	/**
	 * ��ȡ�������ϵĴʻ�
	 * */
	private void read_remote_word() {
		/*��ʾ������*/
		Message msg_open = new Message();
		msg_open.what = MSG_FLAG_OPEN_PROGRESS_BAR;
		mHandler.sendMessage(msg_open);
		
		try {
			/*��ȡ�ʻ��б�*/
			ArrayList<OneWord> word_list = gameInfo.user.get_all_word();
			
			/*���ʻ���ӵ�ԭ�����б��У�������ʾ�ڽ�����*/
			add_word_list(word_list);
			
			/*ɾ��Զ�����ݿ��еĸ����ʻ�*/
			//gameInfo.user.del_word(word_list);		//�ڷ�����Ѿ��Ѷ����Ĵʻ�ɾ����
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
		
		/*�رս�����*/
		Message msg_close = new Message();
		msg_close.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
		mHandler.sendMessage(msg_close);
	}
	
	/**
	 * ���ʻ㱣�浽�����ļ���
	 * */
	private void save_word_to_file() {
		gameInfo.data_saveWordList(this.word_list);
	}
	
	/**
	 * ɾ�����дʻ�
	 * */
	private void del_all_word() {
		//ɾ��Adapter��Ӧ�����еĴʻ�
		mArray.removeAll(mArray);
		mAdapt.notifyDataSetChanged();		//���½���
		
		//ɾ��word_list�еĴʻ�
		this.word_list.removeAll(word_list);
	}
	
	/**
	 * ���ʻ���ӵ�ԭ�����б��У�������ʾ�ڽ�����
	 * */
	private void add_word_list(ArrayList<OneWord> word_list){
		
		for(OneWord element : word_list)
		{
			/*��ӵ�ԭ�����б���*/
			this.word_list.add(element);
			
			/*����Msg�����½���*/
			Message msg = new Message();
			msg.what = MSG_FLAG_LISTVIEW_REFRESH;		//��Ϣ����
			Bundle b = new Bundle();					//��Ϣ��������
			b.putString(KEY_WORD, element.word);
			b.putString(KEY_SENDER, element.sender);
			b.putString(KEY_TIME, element.time);
			msg.setData(b);
			mHandler.sendMessage(msg);					//������Ϣ
		}
		
		return;
	}
	
	/**
	 * ��ʾѯ�ʽ���
	 * */
	private void ad_show(){
		/*������ʾ����*/
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.title_del_words));
		builder.setIcon(android.R.drawable.ic_dialog_alert);		//ʹ��Android����ͼ��
		builder.setPositiveButton(			//����ȷ�ϰ�ť
			getString(R.string.button_ok), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//ɾ�����дʻ�
					del_all_word();
				}
		});
		builder.setNegativeButton(			//����ȡ����ť
			getString(R.string.button_cancel), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//ʲô������
				}
		});
		
		/*��ʾ��ʾ����*/
		AlertDialog ad = builder.create();;
		ad.show();
	}
	
	/**
	 * ���߳�Handler����msg�ĺ���
	 * */
	private void handler_msg(Message msg){
		switch(msg.what)
		{
			case MSG_FLAG_LISTVIEW_REFRESH:
			{
				//��ȡBundle����
				Bundle b = msg.getData();
				String word = b.getString(KEY_WORD);
				String sender = b.getString(KEY_SENDER);
				String time = b.getString(KEY_TIME);
				
				//1. ��������
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(KEY_WORD, word);
				map.put(KEY_SENDER, sender);
				map.put(KEY_TIME, time);
				
				//2. �������
				mArray.add(map);
				
				//3. ֪ͨ���������½���
				mAdapt.notifyDataSetChanged();
				
				break;
			}
			
	        case MSG_FLAG_OPEN_PROGRESS_BAR:
	        	/*������������ʾ����ʾ�����ڵ�¼��*/
	    		mProgDialog = new ProgressDialog(ActivityWordDisplay.this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_read_words));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*�رս�����*/
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
