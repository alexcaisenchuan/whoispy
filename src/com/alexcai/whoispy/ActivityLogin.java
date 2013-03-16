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
	 * 常量
	 * */
	private static final String TAG = "whoispy.ActivityLogin";
	/*messege类型*/
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
	 * 属性
	 * */
	/*界面元素*/
	private EditText edit_phone;
	private EditText edit_passwd;
	private Button button_login;
	private TextView text_register;
	private ProgressDialog mProgDialog;
	/*游戏相关信息*/
	private Game gameInfo;
	/*登录线程*/
	private Runnable login_run;
	/*主线程Handler*/
	private Handler mHandler;
	
	/*--------------------------
	 * 方法
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*绑定界面元素*/
		edit_phone = (EditText)findViewById(R.id.phone);
		edit_passwd = (EditText)findViewById(R.id.passwd);
		button_login = (Button)findViewById(R.id.login);
		text_register = (TextView)findViewById(R.id.register);
		
		/*绑定线程*/
		login_run = new Runnable() {
			@Override
			public void run() {
				task_login();
			}
		};
		
		/*设置Handler*/
		mHandler = new Handler(){
			@Override
	        public void handleMessage(Message msg){
	        	handle_msg(msg);
	        }
	    };
		
		/*设置点击事件*/
		button_login.setOnClickListener(new OnClickListener() {		//登录按钮
			
			@Override
			public void onClick(View v) {
				new Thread(login_run).start();
			}
		});
		
		text_register.setOnClickListener(new OnClickListener() {	//注册按钮
			
			@Override
			public void onClick(View v) {
				//启动注册界面Activity
				Intent i = new Intent(ActivityLogin.this, ActivityRegister.class);
				startActivity(i);
			}
		});
	}
	
	/*-----------------------------
	 * 自定义方法
	 * ---------------------------*/
	/**
	 * 登录线程
	 * */
	private void task_login() {
		/*读取手机号及密码*/
		String phone_num = edit_phone.getText().toString();
		String passwd = edit_passwd.getText().toString();
		
		/*进行校验*/
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
		
		/*弹出进度条提示，显示“正在登录”*/
		Message msg_open_progressbar = new Message();
		msg_open_progressbar.what = MSG_FLAG_OPEN_PROGRESS_BAR;
        mHandler.sendMessage(msg_open_progressbar);  
		
		/*访问相关网页，进行登录*/
        int ret = gameInfo.user_login(phone_num, passwd);
        
        /*关闭进度条*/
        Message msg_close_progressbar = new Message();
        msg_close_progressbar.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
        mHandler.sendMessage(msg_close_progressbar);  
		
        /*根据登录结果进行相应处理*/
        switch(ret)
        {
        	//登录成功
        	case Err.NO_ERR:
	        {
	        	/*进行提示*/
	        	Message msg = new Message();
				msg.what = MSG_FLAG_LOGIN_SUCCESS;
		        mHandler.sendMessage(msg);
		        /*关闭当前Activity*/
		        this.finish();
				/*若登录成功则跳转到主界面*/
				Intent i = new Intent(this, ActivityMenu.class);
				startActivity(i);
				break;
			}
			
	        //用户名密码错误
        	case Err.ERR_PASSWD:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_PASSWD_ERR;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//网络错误
        	case Err.ERR_NETWORK:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_ERR;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//网络超时
        	case Err.ERR_NETWORK_TIMEOUT:
        	{
        		Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_TIMEOUT;
		        mHandler.sendMessage(msg);
		        break;
        	}
        	
        	//其他错误
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
	 * 主线程Handler的处理函数
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
	        	/*弹出进度条提示，显示“正在登录”*/
	    		mProgDialog = new ProgressDialog(ActivityLogin.this);
	    		mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
	    		mProgDialog.setMessage(getString(R.string.hint_login));
	    		mProgDialog.setCancelable(true);
	    		mProgDialog.show();
	        	break;
	        	
	        case MSG_FLAG_CLOSE_PROGRESS_BAR:
	        	/*关闭进度条*/
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
	    		/*显示欢迎信息*/
	    		String text = getString(R.string.hint_welcome) + gameInfo.user.curr_nickname;
	    		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
	    		toast.show();
	    		break;
	        	
	        default:
	        	break;
        }
    }
}
