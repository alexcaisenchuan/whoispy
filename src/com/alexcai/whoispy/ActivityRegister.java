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
	 * 常量
	 *----------------------------*/
	private static final String TAG = "whoispy.ActivityRegister";
	/*messege类型*/
	private static final int MSG_FLAG_COMMON_ERR 				= 1;	//通用错误
	private static final int MSG_FLAG_REGISTER_SUCCESS 			= 2;	//注册成功
	private static final int MSG_FLAG_INVALID_PHONE 			= 3;	//手机号非法
	private static final int MSG_FLAG_INVALID_NICKNAME 			= 4;	//昵称非法
	private static final int MSG_FLAG_INVALID_PASSWD 			= 5;	//密码非法
	private static final int MSG_FLAG_INVALID_PASSWD_CONFIRM 	= 6;	//确认密码非法
	private static final int MSG_FLAG_OPEN_PROGRESS_BAR 		= 7;	//打开进度条 
	private static final int MSG_FLAG_CLOSE_PROGRESS_BAR 		= 8;	//关闭进度条
	private static final int MSG_FLAG_PHONE_EXIST_ERR 			= 9;	//手机号已存在
	private static final int MSG_FLAG_NETWORK_ERR 				= 10;	//网络错误
	private static final int MSG_FLAG_NETWORK_TIMEOUT 			= 11;	//网络连接超时
	
	/*----------------------------
	 * 属性
	 *----------------------------*/
	/*界面元素*/
	private EditText edit_phone;
	private EditText edit_nickname;
	private EditText edit_passwd;
	private EditText edit_passwd_confirm;
	private Button	 button_register;
	private ProgressDialog mProgDialog;		//提示进度条
	/*游戏相关信息*/
	private Game gameInfo;
	/*注册线程*/
	private Runnable register_run;
	/*主线程Handler*/
	private Handler mHandler;
	
	/*----------------------------
	 * 重写方法
	 *----------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*绑定界面元素*/
		edit_phone = (EditText)findViewById(R.id.phone);
		edit_nickname = (EditText)findViewById(R.id.nickname);
		edit_passwd = (EditText)findViewById(R.id.passwd);
		edit_passwd_confirm = (EditText)findViewById(R.id.passwd_confirm);
		button_register = (Button)findViewById(R.id.register);
		
		/*设置线程函数*/
		register_run = new Runnable() {
			@Override
			public void run() {
				task_register();
			}
		};
		
		/*设置Handler*/
		mHandler = new Handler(){
			@Override
			public void handleMessage(Message msg){
	        	handle_msg(msg);
	        }
		};
		
		/*设置鼠标点击事件*/
		button_register.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//启动注册线程
				new Thread(register_run).start();
			}
		});
	}
	
	/*-----------------------------
	 * 自定义方法
	 * ---------------------------*/
	/**
	 * 注册线程
	 * */
	private void task_register() {
		/*读取手机号码，校验*/
		String phone = edit_phone.getText().toString();
		if(!Misc.isMobileNOStrict(phone))
		{
			//进行提示
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PHONE;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*读取昵称，校验*/
		String nickname = edit_nickname.getText().toString();
		if((nickname.length() < 3) || (nickname.length() > 15))		//昵称长度在3~15之间
		{
			//进行提示
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_NICKNAME;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*读取密码，校验*/
		String passwd = edit_passwd.getText().toString();
		String passwd_confirm = edit_passwd_confirm.getText().toString();
		//密码长度要在6~32之间
		if((passwd.length() < 6) || (passwd.length() > 32))
		{
			//进行提示
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PASSWD;
			mHandler.sendMessage(msg);
			return;
		}
		//密码与确认密码要一致
		if(!passwd.equals(passwd_confirm))
		{
			//进行提示
			Message msg = new Message();
			msg.what = MSG_FLAG_INVALID_PASSWD_CONFIRM;
			mHandler.sendMessage(msg);
			return;
		}
		
		/*弹出注册进度条*/
		Message msg_open_progress_bar = new Message();
		msg_open_progress_bar.what = MSG_FLAG_OPEN_PROGRESS_BAR;
		mHandler.sendMessage(msg_open_progress_bar);
		
		/*访问网页，进行注册*/
		int ret = gameInfo.user.register(phone, nickname, passwd);
		
		/*关闭进度条*/
		Message msg_close_progress_bar = new Message();
		msg_close_progress_bar.what = MSG_FLAG_CLOSE_PROGRESS_BAR;
		mHandler.sendMessage(msg_close_progress_bar);
		
		/*根据注册结果进行相应处理*/
		switch(ret)
		{
			case Err.NO_ERR:
			{
				/*进行提示*/
				Message msg = new Message();
				msg.what = MSG_FLAG_REGISTER_SUCCESS;
				mHandler.sendMessage(msg);
				/*关闭当前Activity*/
				finish();
				break;
			}
			
			case Err.ERR_NETWORK:
			{
				/*进行提示*/
				Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_ERR;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_NETWORK_TIMEOUT:
			{
				/*进行提示*/
				Message msg = new Message();
				msg.what = MSG_FLAG_NETWORK_TIMEOUT;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_PHONE_EXIST:
			{
				/*进行提示*/
				Message msg = new Message();
				msg.what = MSG_FLAG_PHONE_EXIST_ERR;
				mHandler.sendMessage(msg);
				break;
			}
			
			case Err.ERR_COMMON:
			default:
			{
				/*进行提示*/
				Message msg = new Message();
				msg.what = MSG_FLAG_COMMON_ERR;
				mHandler.sendMessage(msg);
				break;
			}
		}
	}
	
	/**
	 * 主线程Handler处理函数
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
