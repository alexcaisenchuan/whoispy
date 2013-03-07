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
	 * 属性
	 *------------------------*/
	static final String TAG = "whoispy.ActivitySetRoleNum";
	
	/*界面元素*/
	private EditText text_spy_player;
	private EditText text_trick_player;
	
	//游戏相关信息
	private Game gameInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_role_num);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_r);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_role_num);
		
		Log.d(TAG, "onCreate, this : " + this);
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*设置游戏阶段*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_ROLE_NUM);
		
		/*若是第一次设置人数，则计算推荐的玩家个数*/
		if((gameInfo.game_getSpyNum() == 0) && (gameInfo.game_getTrickNum() == 0))
		{
			gameInfo.game_calcRoleNum();
		}
		
		/*绑定元素*/
		//下一步按钮
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
		
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetRoleNum.this, ActivitySetPlayer.class);
				startActivity(i);
				finish();		//要记得关闭当前Activity
				Log.d(TAG, "finish!");
			}
		});
		
		//显示总人数
		TextView text_total_player = (TextView)findViewById(R.id.total_player_num);
		text_total_player.setText(String.valueOf(gameInfo.game_getValidPlayerNum()));

		//显示推荐的角色数
		text_spy_player = (EditText)findViewById(R.id.spy_player_num);			//卧底个数
		text_spy_player.setText(String.valueOf(gameInfo.game_getSpyNum()));
		text_trick_player = (EditText)findViewById(R.id.trick_player_num);		//打酱油个数
		text_trick_player.setText(String.valueOf(gameInfo.game_getTrickNum()));
	}
	
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		//保存游戏参数
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	/*--------------------------
	 * 私有方法
	 *------------------------*/
	/**
	 * 检查角色个数是否合理，若合理则保存角色个数
	 * 
	 * @return 若合理，返回true；否则返回false；
	 * */
	private boolean check_role_num_valid()
	{
		int valid_num = gameInfo.game_getValidPlayerNum();
		String spy_num_s;
		String trick_num_s;
		int spy_num = 0;
		int trick_num = 0;
		boolean valid = true;		//是否有效的标志
		
		try
		{
			spy_num_s = text_spy_player.getText().toString();
			trick_num_s = text_trick_player.getText().toString();
			//空字符串则认为是0
			if(trick_num_s.length() == 0)
			{
				trick_num = 0;
			}
			else
			{
				trick_num = Integer.parseInt(trick_num_s);
			}
			//空字符串则认为是0
			if(spy_num_s.length() == 0)
			{
				spy_num = 0;
			}
			else
			{
				spy_num = Integer.parseInt(spy_num_s);
			}
			
			//要求：
			//1、卧底与打酱油的加起来小于总人数；
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
			//设置卧底与打酱油的个数
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
