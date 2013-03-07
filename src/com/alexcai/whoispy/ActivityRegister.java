package com.alexcai.whoispy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class ActivityRegister extends Activity{
	/*----------------------------
	 * ����
	 *----------------------------*/
	private static final String TAG = "whoispy.ActivityRegister";
	/*messege����*/
	private static final int MSG_FLAG_COMMON_ERR 				= 1;	//ͨ�ô���
	private static final int MSG_FLAG_REGISTER_SUCCESS 			= 2;	//ע��ɹ�
	private static final int MSG_FLAG_INVALID_PHONE 			= 3;	//�ֻ��ŷǷ�
	private static final int MSG_FLAG_INVALID_NICKNAME 			= 4;	//�ǳƷǷ�
	private static final int MSG_FLAG_INVALID_PASSWD 			= 5;	//����Ƿ�
	private static final int MSG_FLAG_INVALID_PASSWD_CONFIRM 	= 6;	//ȷ������Ƿ�
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 		= 7;	//�򿪽����� 
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 		= 8;	//�رս�����
	private static final int MSG_FLAG_PHONE_EXIST_ERR 			= 9;	//�ֻ����Ѵ���
	private static final int MSG_FLAG_NETWORK_ERR 				= 10;	//�������
	private static final int MSG_FLAG_NETWORK_TIMEOUT 			= 11;	//�������ӳ�ʱ
	
	/*----------------------------
	 * ����
	 *----------------------------*/
	/*����Ԫ��*/
	private EditText edit_phone;
	private EditText edit_nickname;
	private EditText edit_passwd;
	private EditText edit_passwd_confirm;
	private Button	 button_register;
	private ProgressDialog mProgDialog;		//��ʾ������
	/*��Ϸ�����Ϣ*/
	private Game gameInfo;
	/*ע���߳�*/
	private Runnable register_run;
	/*���߳�Handler*/
	private Handler mHandler;
	
	/*----------------------------
	 * ��д����
	 *----------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*�󶨽���Ԫ��*/
		edit_phone = (EditText)findViewById(R.id.phone);
		edit_nickname = (EditText)findViewById(R.id.nickname);
		edit_passwd = (EditText)findViewById(R.id.passwd);
		edit_passwd_confirm = (EditText)findViewById(R.id.passwd_confirm);
		button_register = (Button)findViewById(R.id.register);
		
		/*�����̺߳���*/
		register_run = new Runnable() {
			@Override
			public void run() {
				task_register();
			}
		};
		
		/*����Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
	        	handle_msg(msg);
	        }
		};
		
		/*����������¼�*/
		button_register.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//����ע���߳�
				new Thread(register_run).start();
			}
		});
	}
	
	/*-----------------------------
	 * �Զ��巽��
	 * ---------------------------*/
	/**
	 * ע���߳�
	 * */
	private void task_register() {
		/*��ȡ�ֻ����룬У��*/
		String phone = edit_phone.getText().toString();
		if(!Misc.isMobileNOStrict(phone))
		{
			//������ʾ
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PHONE;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*��ȡ�ǳƣ�У��*/
		String nickname = edit_nickname.getText().toString();
		if((nickname.length() < 3) || (nickname.length() > 15))		//�ǳƳ�����3~15֮��
		{
			//������ʾ
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_NICKNAME;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*��ȡ���룬У��*/
		String passwd = edit_passwd.getText().toString();
		String passwd_confirm = edit_passwd_confirm.getText().toString();
		//���볤��Ҫ��6~32֮��
		if((passwd.length() < 6) || (passwd.length() > 32))
		{
			//������ʾ
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PASSWD;
			mHandler.sendMessage(msg);
			return;
		}
		//������ȷ������Ҫһ��
		if(!passwd.equals(passwd_confirm))
		{
			//������ʾ
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PASSWD_CONFIRM;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*����ע�������*/
		Message msg_open_progress_bar = new Message();
		msg_open_progress_bar.what = MSG_FLAG_OPEN_PROGRESS_BAR;
		mHandler.sendMessage(msg_open_progress_bar);
		
		/*������ҳ������ע��*/
		int ret = gameInfo.user.register(phone, nickname, passwd);
		
		/*�رս�����*/
		Message msg_close_progress_bar = new Message();
		msg_close_progress_bar.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
		mHandler.sendMessage(msg_close_progress_bar);
		
		/*����ע����������Ӧ����*/
		switch(ret)
		{
			case Err.NO_ERR:
			{
				/*������ʾ*/
				Message msg = new Message();
				msg.what = MSG_FLAG_REGISTER_SUCCESS;
				mHandler.sendMessage(msg);
				/*�رյ�ǰActivity*/
				finish();
				break;
			}
			
			case Err.ERR_NETWORK:
			{
				/*������ʾ*/
				Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_ERR;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_NETWORK_TIMEOUT:
			{
				/*������ʾ*/
				Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_TIMEOUT;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_PHONE_EXIST:
			{
				/*������ʾ*/
				Message msg = new Message();
				msg.what = MSG_FLAG_PHONE_EXIST_ERR;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_COMMON:
			default:
			{
				/*������ʾ*/
				Message msg = new Message();
				msg.what = MSG_FLAG_COMMON_ERR;
				mHandler.sendMessage(msg);
				break;
			}
		}
	}
	
	/**
	 * ���߳�Handler������
	 * */
	private void handle_msg(Message msg){
		switch(msg.what)
		{
			case MSG_FLAG_OPEN_PROGRESS_BAR:
				mProgDialog = new ProgressDialog(ActivityRegister.this);
				mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mProgDialog.setMessage(getString(R.string.hint_register));
				mProgDialog.setCancelable(true);
				mProgDialog.show();
				break;
			
			case MSG_FLAG_CLOSE_PROGRESS_BAR:
				mProgDialog.dismiss();
				break;
				
			case MSG_FLAG_REGISTER_SUCCESS:
				Toast.makeText(this, getString(R.string.hint_register_success), Toast.LENGTH_SHORT).show();
				break;
			
			case MSG_FLAG_COMMON_ERR:
				Toast.makeText(this, getString(R.string.hint_register_error), Toast.LENGTH_SHORT).show();
				break;
				
			case MSG_FLAG_INVALID_NICKNAME:
				Toast.makeText(this, getString(R.string.hint_invalid_nickname), Toast.LENGTH_SHORT).show();
				break;
				
			case MSG_FLAG_INVALID_PASSWD:
				Toast.makeText(this, getString(R.string.hint_invalid_passwd), Toast.LENGTH_SHORT).show();
				break;
				
			case MSG_FLAG_INVALID_PASSWD_CONFIRM:
				Toast.makeText(this, getString(R.string.hint_invalid_passwd_confirm), Toast.LENGTH_SHORT).show();
				break;
				
			case MSG_FLAG_INVALID_PHONE:
				Toast.makeText(this, getString(R.string.hint_invalid_phone), Toast.LENGTH_SHORT).show();
				break;
				
			case MSG_FLAG_NETWORK_ERR:
				Toast.makeText(this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
				break;
			
			case MSG_FLAG_NETWORK_TIMEOUT:
				Toast.makeText(this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
				break;
			
			case MSG_FLAG_PHONE_EXIST_ERR:
				Toast.makeText(this, getString(R.string.hint_phone_exist_error), Toast.LENGTH_SHORT).show();
				break;
			
			default:
				Log.d(TAG, "Unknown flag : " + msg.what);
				break;
		}
	}
}
