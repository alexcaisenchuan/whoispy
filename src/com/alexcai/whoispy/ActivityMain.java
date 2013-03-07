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
	 * ����
	 * */
	private final String TAG = "whoispy.ActivityMain";
	/*messege����*/
	private static final int MSG_FLAG_COMMON_ERR = 1;
	private static final int MSG_FLAG_PASSWD_ERR = 2;
	private static final int MSG_FLAG_NETWORK_ERR = 3;
	private static final int MSG_FLAG_LOGIN_SUCCESS = 4;
	private static final int MSG_FLAG_NETWORK_TIMEOUT = 5;
	
	/*------------------------------
	 * ����
	 * */
	//��Ϸ�����Ϣ
	private Game gameInfo;
	//��¼�߳���������
	Runnable login_run = new Runnable() {
		
		@Override
		public void run() {
			task_login();
		}
	};
	
	/*------------------------------
	 * ����
	 * */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		Log.d(TAG, "onCreate!");
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*���Ե�½*/
		new Thread(login_run).start();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) 
				&& event.getAction() == KeyEvent.ACTION_DOWN)
		{
			Log.d(TAG, "Exit app!");
			//�˳�����
			finish();		//finish����ֻ���˳�Activity
			android.os.Process.killProcess(android.os.Process.myPid());
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/*--------------------
	 * �Զ��巽��
	 * */
	private void task_login(){
		//��ȡ���ر�����û���Ϣ�����Ե�½
		int ret = gameInfo.user.resumeUser();
		
		//������Ϣ
		Message msg = new Message();
		//���ݲ�ͬ����ֵ������Ϣ
		switch(ret)
		{
			//��½�ɹ�
			case Err.NO_ERR:
			{
	            msg.what = MSG_FLAG_LOGIN_SUCCESS;
				break;
			}
			
			//�û������������
			case Err.ERR_PASSWD:
			{
	            msg.what = MSG_FLAG_PASSWD_ERR;
				break;
			}
			
			//�������
			case Err.ERR_NETWORK:
			{
	            msg.what = MSG_FLAG_NETWORK_ERR;   
				break;
			}
			
			//�������ӳ�ʱ
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
		//������Ϣ
		mHandler.sendMessage(msg);  
	}
	
	//����һ��Handler
	ProgressDialog mProgDialog;
	
    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            switch(msg.what) {
	            case MSG_FLAG_PASSWD_ERR:
	            {
	            	//��ʾ��ʾ��Ϣ
	            	Toast hint = Toast.makeText(ActivityMain.this, getString(R.string.hint_passwd_error), Toast.LENGTH_SHORT);
	            	hint.show();
	            	//��ʾ��¼����
					Intent i = new Intent(ActivityMain.this, ActivityLogin.class);
					startActivity(i);
					finish();		//�رյ�ǰActivity
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
	            	//��ʾ������
					Intent i = new Intent(ActivityMain.this, ActivityMenu.class);
					startActivity(i);
					finish();		//�رյ�ǰActivity
	            	//��ʾ��ʾ��Ϣ
					/*��ʾ��ӭ��Ϣ*/
					String text = getString(R.string.hint_welcome) + gameInfo.user.curr_nickname;
					Toast toast = Toast.makeText(ActivityMain.this, text, Toast.LENGTH_SHORT);
					toast.show();
	            	break;
	            }
	            
	            default:
	            {	
	            	//��ʾ��¼����
					Intent i = new Intent(ActivityMain.this, ActivityLogin.class);
					startActivity(i);
					finish();		//�رյ�ǰActivity
	            	break;
	            }
            }
        }  
    };
}
