package com.alexcai.whoispy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ActivityLogin extends Activity{
	/*------------------------------
	 * ����
	 * */
	private static final String TAG = "whoispy.ActivityLogin";
	/*messege����*/
	private static final int MSG_FLAG_NO_PHONE = 1;
	private static final int MSG_FLAG_NO_PASSWD = 2;
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR = 3;
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR = 4;
	private static final int MSG_FLAG_LOGIN_ERR = 5;
	private static final int MSG_FLAG_PASSWD_ERR = 6;
	private static final int MSG_FLAG_NETWORK_ERR = 7;
	private static final int MSG_FLAG_LOGIN_SUCCESS = 8;
	private static final int MSG_FLAG_NETWORK_TIMEOUT = 9;
	
	/*--------------------------
	 * ����
	 * */
	/*����Ԫ��*/
	private EditText edit_phone;
	private EditText edit_passwd;
	private Button button_login;
	private TextView text_register;
	private ProgressDialog mProgDialog;
	/*��Ϸ�����Ϣ*/
	private Game gameInfo;
	/*��¼�߳�*/
	private Runnable login_run;
	/*���߳�Handler*/
	private Handler mHandler;
	
	/*--------------------------
	 * ����
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*�󶨽���Ԫ��*/
		edit_phone = (EditText)findViewById(R.id.phone);
		edit_passwd = (EditText)findViewById(R.id.passwd);
		button_login = (Button)findViewById(R.id.login);
		text_register = (TextView)findViewById(R.id.register);
		
		/*���߳�*/
		login_run = new Runnable() {
			@Override
			public void run() {
				task_login();
			}
		};
		
		/*����Handler*/
		mHandler = new Handler(){
			@Override
	        public void handleMessage(Message msg){
	        	handle_msg(msg);
	        }
	    };
		
		/*���õ���¼�*/
		button_login.setOnClickListener(new OnClickListener() {		//��¼��ť
			
			@Override
			public void onClick(View v) {
				new Thread(login_run).start();
			}
		});
		
		text_register.setOnClickListener(new OnClickListener() {	//ע�ᰴť
			
			@Override
			public void onClick(View v) {
				//����ע�����Activity
				Intent i = new Intent(ActivityLogin.this, ActivityRegister.class);
				startActivity(i);
			}
		});
	}
	
	/*-----------------------------
	 * �Զ��巽��
	 * ---------------------------*/
	/**
	 * ��¼�߳�
	 * */
	private void task_login() {
		/*��ȡ�ֻ��ż�����*/
		String phone_num = edit_phone.getText().toString();
		String passwd = edit_passwd.getText().toString();
		
		/*����У��*/
		if(0 == phone_num.length())
		{
			Message msg = new Message();
            msg.what = MSG_FLAG_NO_PHONE;  
            mHandler.sendMessage(msg);  
			return;
		}
		if(0 == passwd.length())
		{
			Message msg = new Message();
            msg.what = MSG_FLAG_NO_PASSWD;  
            mHandler.sendMessage(msg);  
			return;
		}
		
		/*������������ʾ����ʾ�����ڵ�¼��*/
		Message msg_open_progressbar = new Message();
		msg_open_progressbar.what = MSG_FLAG_OPEN_PROGRESS_BAR;
        mHandler.sendMessage(msg_open_progressbar);  
		
		/*���������ҳ�����е�¼*/
        int ret = gameInfo.user_login(phone_num, passwd);
        
        /*�رս�����*/
        Message msg_close_progressbar = new Message();
        msg_close_progressbar.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
        mHandler.sendMessage(msg_close_progressbar);  
		
        /*���ݵ�¼���������Ӧ����*/
        switch(ret)
        {
        	//��¼�ɹ�
        	case Err.NO_ERR:
	        {
	        	/*������ʾ*/
	        	Message msg = new Message();
				msg.what = MSG_FLAG_LOGIN_SUCCESS;
		        mHandler.sendMessage(msg);
		        /*�رյ�ǰActivity*/
		        this.finish();
				/*����¼�ɹ�����ת��������*/
				Intent i = new Intent(this, ActivityMenu.class);
				startActivity(i);
				break;
			}
			
	        //�û����������
        	case Err.ERR_PASSWD:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_PASSWD_ERR;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//�������
        	case Err.ERR_NETWORK:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_ERR;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//���糬ʱ
        	case Err.ERR_NETWORK_TIMEOUT:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_TIMEOUT;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//��������
        	case Err.ERR_COMMON:
        	default:
			{
				Message msg = new Message();
				msg.what = MSG_FLAG_LOGIN_ERR;
		        mHandler.sendMessage(msg);
		        break;
			}
        }
        
		return;
	}
	
	/**
	 * ���߳�Handler�Ĵ�����
	 * */
    private void handle_msg(Message msg){
        switch(msg.what) {  
	        case MSG_FLAG_NO_PHONE:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_no_phone), Toast.LENGTH_SHORT).show();
	            break;
	            
	        case MSG_FLAG_NO_PASSWD:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_no_passwd), Toast.LENGTH_SHORT).show();
	            break;
	            
	        case MSG_FLAG_OPEN_PROGRESS_BAR:
	        	/*������������ʾ����ʾ�����ڵ�¼��*/
	    		mProgDialog = new ProgressDialog(ActivityLogin.this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_login));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*�رս�����*/
				mProgDialog.dismiss();
	        	break;
	        	
	        case MSG_FLAG_LOGIN_ERR:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_login_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_PASSWD_ERR:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_passwd_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_ERR:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT).show();
	        	break;
	        	
	        case MSG_FLAG_NETWORK_TIMEOUT:
	        	Toast.makeText(ActivityLogin.this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT).show();
	        	break;
	        
	        case MSG_FLAG_LOGIN_SUCCESS:
	    		/*��ʾ��ӭ��Ϣ*/
	    		String text = getString(R.string.hint_welcome) + gameInfo.user.curr_nickname;
	    		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
	    		toast.show();
	    		break;
	        	
	        default:
	        	break;
        }
    }
}
