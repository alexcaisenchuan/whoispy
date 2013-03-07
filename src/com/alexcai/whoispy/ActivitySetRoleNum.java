package com.alexcai.whoispy;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySetRoleNum extends Activity{
	/*--------------------------
	 * ����
	 *------------------------*/
	static final String TAG = "whoispy.ActivitySetRoleNum";
	
	/*����Ԫ��*/
	private EditText text_spy_player;
	private EditText text_trick_player;
	
	//��Ϸ�����Ϣ
	private Game gameInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*���ý���*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_role_num);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_r);
		
		/*���ñ�������*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_role_num);
		
		Log.d(TAG, "onCreate, this : " + this);
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*������Ϸ�׶�*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_ROLE_NUM);
		
		/*���ǵ�һ������������������Ƽ�����Ҹ���*/
		if((gameInfo.game_getSpyNum() == 0) && (gameInfo.game_getTrickNum() == 0))
		{
			gameInfo.game_calcRoleNum();
		}
		
		/*��Ԫ��*/
		//��һ����ť
		ImageButton next_button = (ImageButton)findViewById(R.id.title_button_right);
		next_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(check_role_num_valid())
				{
					Intent i = new Intent(ActivitySetRoleNum.this, ActivitySetWord.class);
					startActivity(i);
				}
			}
		});
		
		//��һ����ť
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetRoleNum.this, ActivitySetPlayer.class);
				startActivity(i);
				finish();		//Ҫ�ǵùرյ�ǰActivity
				Log.d(TAG, "finish!");
			}
		});
		
		//��ʾ������
		TextView text_total_player = (TextView)findViewById(R.id.total_player_num);
		text_total_player.setText(String.valueOf(gameInfo.game_getValidPlayerNum()));

		//��ʾ�Ƽ��Ľ�ɫ��
		text_spy_player = (EditText)findViewById(R.id.spy_player_num);			//�Ե׸���
		text_spy_player.setText(String.valueOf(gameInfo.game_getSpyNum()));
		text_trick_player = (EditText)findViewById(R.id.trick_player_num);		//���͸���
		text_trick_player.setText(String.valueOf(gameInfo.game_getTrickNum()));
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		//������Ϸ����
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	/*--------------------------
	 * ˽�з���
	 *------------------------*/
	/**
	 * ����ɫ�����Ƿ�����������򱣴��ɫ����
	 * 
	 * @return ����������true�����򷵻�false��
	 * */
	private boolean check_role_num_valid()
	{
		int valid_num = gameInfo.game_getValidPlayerNum();
		String spy_num_s;
		String trick_num_s;
		int spy_num = 0;
		int trick_num = 0;
		boolean valid = true;		//�Ƿ���Ч�ı�־
		
		try
		{
			spy_num_s = text_spy_player.getText().toString();
			trick_num_s = text_trick_player.getText().toString();
			//���ַ�������Ϊ��0
			if(trick_num_s.length() == 0)
			{
				trick_num = 0;
			}
			else
			{
				trick_num = Integer.parseInt(trick_num_s);
			}
			//���ַ�������Ϊ��0
			if(spy_num_s.length() == 0)
			{
				spy_num = 0;
			}
			else
			{
				spy_num = Integer.parseInt(spy_num_s);
			}
			
			//Ҫ��
			//1���Ե�����͵ļ�����С����������
			if(spy_num + trick_num >= valid_num)
			{
				valid = false;
			}
		}
		catch(NumberFormatException e)
		{
			Log.d(TAG, "NumberFormatException : " + e.toString());
			valid = false;
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			valid = false;
		}
		
		if(valid)
		{
			//�����Ե�����͵ĸ���
			gameInfo.game_setSpyNum(spy_num);
			gameInfo.game_setTrickNum(trick_num);
		}
		else
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_role_num_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
		}
		
		return valid;
	}
}
