package com.alexcai.whoispy;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Toast;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;

public class ActivityMain extends Activity {
	/*------------------------------
	 * 常量
	 * */
	private final String TAG = "whoispy.ActivityMain";
	/*messege类型*/
	private static final int MSG_FLAG_COMMON_ERR = 1;
	private static final int MSG_FLAG_PASSWD_ERR = 2;
	private static final int MSG_FLAG_NETWORK_ERR = 3;
	private static final int MSG_FLAG_LOGIN_SUCCESS = 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT = 5;
	
	/*------------------------------
	 * 属性
	 * */
	//游戏相关信息
	private Game gameInfo;
	//登录线程启动对象
	Runnable login_run = new Runnable() {
		
		@Override
		public void run() {
			task_login();
		}
	};
	
	/*------------------------------
	 * 方法
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.d(TAG, "onCreate!");
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*尝试登陆*/
		new Thread(login_run).start();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) 
				&& event.getAction() == KeyEvent.ACTION_DOWN)
		{
			Log.d(TAG, "Exit app!");
			//退出程序
			finish();		//finish仅仅只是退出Activity
			android.os.Process.killProcess(android.os.Process.myPid());
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/*--------------------
	 * 自定义方法
	 * */
	private void task_login(){
		//读取本地保存的用户信息并尝试登陆
		int ret = gameInfo.user.resumeUser();
		
		//创建消息
		Message msg = new Message();
		//根据不同返回值构造消息
		switch(ret)
		{
			//登陆成功
			case Err.NO_ERR:
			{
	            msg.what = MSG_FLAG_LOGIN_SUCCESS;
				break;
			}
			
			//用户名或密码错误
			case Err.ERR_PASSWD:
			{
	            msg.what = MSG_FLAG_PASSWD_ERR;
				break;
			}
			
			//网络错误
			case Err.ERR_NETWORK:
			{
	            msg.what = MSG_FLAG_NETWORK_ERR;   
				break;
			}
			
			//网络连接超时
			case Err.ERR_NETWORK_TIMEOUT:
			{
	            msg.what = MSG_FLAG_NETWORK_TIMEOUT;   
				break;
			}
			
			default:
			{
				msg.what = MSG_FLAG_COMMON_ERR;
				break;
			}
		}
		//发出消息
		mHandler.sendMessage(msg);  
	}
	
	//定义一个Handler
	ProgressDialog mProgDialog;
	
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what) {
	            case MSG_FLAG_PASSWD_ERR:
	            {
	            	//显示提示信息
	            	Toast hint = Toast.makeText(ActivityMain.this, getString(R.string.hint_passwd_error), Toast.LENGTH_SHORT);
	            	hint.show();
	            	//显示登录界面
					Intent i = new Intent(ActivityMain.this, ActivityLogin.class);
					startActivity(i);
					finish();		//关闭当前Activity
	            	break;
	            }
	            
	            case MSG_FLAG_NETWORK_ERR:
	            {
	            	Toast hint = Toast.makeText(ActivityMain.this, getString(R.string.hint_network_error), Toast.LENGTH_SHORT);
	            	hint.show();
	            	break;
	            }
	            
	            case MSG_FLAG_NETWORK_TIMEOUT:
	            {
	            	Toast hint = Toast.makeText(ActivityMain.this, getString(R.string.hint_network_timeout), Toast.LENGTH_SHORT);
	            	hint.show();
	            	break;
	            }
	            
	            case MSG_FLAG_LOGIN_SUCCESS:
	            {
	            	//显示主界面
					Intent i = new Intent(ActivityMain.this, ActivityMenu.class);
					startActivity(i);
					finish();		//关闭当前Activity
	            	//显示提示信息
					/*显示欢迎信息*/
					String text = getString(R.string.hint_welcome) + gameInfo.user.curr_nickname;
					Toast toast = Toast.makeText(ActivityMain.this, text, Toast.LENGTH_SHORT);
					toast.show();
	            	break;
	            }
	            
	            default:
	            {	
	            	//显示登录界面
					Intent i = new Intent(ActivityMain.this, ActivityLogin.class);
					startActivity(i);
					finish();		//关闭当前Activity
	            	break;
	            }
            }
        }  
    };
}
