package com.alexcai.whoispy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ActivitySetPlayer extends ListActivity{
	/*--------------------------
	 * 属性
	 *------------------------*/
	private static final String TAG = "whoispy.PlayerSet";
	
	private static final int PICK_CONTACT_SUBACTIVITY = 2;
	
	//玩家设置定义
	private final int PLAYER_SETTING_DELETE = 0;		//删除玩家
	private final int PLAYER_SETTING_ATTEND = 1;		//设置玩家参加或暂时不参加游戏
	//List相关
    private SimpleAdapter mAdapter;
    private ArrayList<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    //Map Key
    private final String KEY_INDEX = "index";
    private final String KEY_INFO = "info";
    private final String KEY_IMG = "img";
    
	//游戏相关信息
	private Game gameInfo;
	
	/*--------------------------
	 * 重写方法
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*设置界面*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_player);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_text_button_r);
		
		/*设置标题文字*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_player);
		
		Log.d(TAG, "onCreate, this : " + this);
		
		/*读取游戏信息*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*设置游戏阶段*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_PLAYER);
		
		/*绑定List*/
        mAdapter = new SimpleAdapter(this,
        							 mData,
        							 R.layout.list_set_player,
        							 new String[]{KEY_INDEX, KEY_IMG, KEY_INFO},
        							 new int[]{R.id.player_index, R.id.player_status_img, R.id.player_info});
        setListAdapter(mAdapter);
                
		/*绑定界面元素*/
		//打开电话本按钮
    	Button phone_book_button = (Button)findViewById(R.id.phone_book_button);
		phone_book_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				phoneBook_open();
			}
		});
		
		//号码输入框
		final EditText phone_num_text = (EditText)findViewById(R.id.phone_num_text);
		phone_num_text.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((KeyEvent.KEYCODE_ENTER == keyCode) && (event.getAction() == KeyEvent.ACTION_DOWN))
				{
					/*读取手机号*/
					final String numPhone = phone_num_text.getText().toString();
					
					/*校验手机号*/
					if(player_phonenum_check(numPhone) == false)
					{
						return false;
					}
					
					/*要求输入玩家名字*/
					final AlertDialog.Builder dlg = new AlertDialog.Builder(ActivitySetPlayer.this).setTitle(getResources().getString(R.string.hint_input_player_name));
					final EditText newText = new EditText(ActivitySetPlayer.this);
					newText.setText(getResources().getString(R.string.player_default_name));
					dlg.setView(newText);
					dlg.setPositiveButton(getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							/*将联系人添加到玩家列表里*/
							String strName = newText.getText().toString();
							if(player_add(strName, numPhone))
							{
								phone_num_text.setText("");		//添加后清空
							}
						}
					});
					dlg.setNegativeButton(getResources().getString(R.string.button_cancel), null);
					dlg.show();
				}
				return false;
			}
		});
		
		//下一步按钮
		ImageButton next_button = (ImageButton)findViewById(R.id.title_button_right);
		next_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(player_check())
				{
					Intent i = new Intent(ActivitySetPlayer.this, ActivitySetRoleNum.class);
					startActivity(i);
				}
			}
		});
		
		//上一步按钮
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetPlayer.this, ActivityMenu.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//清空Activity栈中ActivityMain之上的Activity
				startActivity(i);
				finish();		//要记得关闭当前Activity
				Log.d(TAG, "finish!");
			}
		});
		
		//中间的更新列表按钮
		ImageButton center_button = (ImageButton)findViewById(R.id.title_button_center);
		center_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ad_show();
			}
		});
		
		//显示之前的玩家列表
		player_view();
	}
	
	@Override
	protected void onPause() {
		//保存玩家列表到XML中
		gameInfo.data_savePlayerList();
		//保存游戏参数
		gameInfo.data_saveGamePref();
		
		super.onPause();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult() requestCode : " + requestCode + ", resultCode : " + resultCode + ", data " + data);
		
		switch (requestCode)
		{
			case PICK_CONTACT_SUBACTIVITY:
				phoneBook_getResult(data);
				break;
			
			default:
				break;
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		
		try {
			/*读取玩家id号*/
			String index = mData.get(position).get(KEY_INDEX).toString();
			final int player_id = Integer.parseInt(index);
			final int list_index = position;
			
			/*根据玩家当前状态设置提示信息*/
			CharSequence[] items;
			if(Player.ROLE_SHIELD == gameInfo.player_get(player_id).role)		//若暂时没参加游戏,则显示恢复参加游戏
			{
				items = new CharSequence[]{
					getResources().getString(R.string.delete_player), 
					getResources().getString(R.string.set_player_resume)};
			}
			else
			{
				items = new CharSequence[]{
					getResources().getString(R.string.delete_player), 
					getResources().getString(R.string.set_player_shield)};
			}
			
			Log.d(TAG, "player_id : " + player_id + ", list_index : " + list_index);
			
			AlertDialog.Builder dlg = new AlertDialog.Builder(ActivitySetPlayer.this);
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
	 * 私有方法
	 *------------------------*/
	/**
	 * 打开电话本
	 * */
	private void phoneBook_open()
	{
		startActivityForResult(
			//first param
			new Intent(
				Intent.ACTION_PICK,
				android.provider.ContactsContract.Contacts.CONTENT_URI
			),
			//second param
			PICK_CONTACT_SUBACTIVITY
		);
	}
	
	/**
	 * 读取电话本返回的结果
	 * */
	private void phoneBook_getResult(Intent data)
	{
		Uri uriRet;
		String strName;		//联系人名字
		String numPhone;	//联系人电话
		//int typePhone;		//电话类型
		//int resType;
		
		if(null == data)
		{
			Log.d(TAG, "phoneBook_getResult(): No data!");
			return;
		}
		
		uriRet = data.getData();
		if (uriRet != null)
		{
			try
			{
				/* 必须要有android.permission.READ_CONTACTS权限 */
				Cursor c = managedQuery(uriRet, null, null, null, null);
				/* 将Cursor移到资料最前端 */
				c.moveToFirst();
				/* 取得联络人的姓名 */
				strName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				/* 取得联络人的电话 */
				int contactId = c.getInt(c.getColumnIndex(ContactsContract.Contacts._ID));
				Cursor phones = getContentResolver()
						        .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
								       null,
								       ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
								       null,
								       null);
				
				if (phones.getCount() > 0)
				{
					phones.moveToFirst();
					/* 2.0可以允许User设定多组电话号码，但本范例只捞一组电话号码作示范 */
					numPhone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					//typePhone = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
					//resType = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(typePhone);
					
					/*将联系人添加到玩家列表里*/
					player_add(strName, numPhone);
				}
				else		//找不到电话号码则提示
				{
					Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_none), Toast.LENGTH_SHORT);
					t.setGravity(Gravity.CENTER, 0, 0);
					t.show();
				}
				
				phones.close();
			}
			catch(Exception e)
			{
				Log.d(TAG, "Exception : " + e.toString());
			}
		}
	}
	
	private boolean player_phonenum_check(String phone)
	{
		/*判断电话号码有效性*/
		if(Misc.isMobileNO(phone) == false)
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		
		/*判断号码是否已经存在*/
		if(Game.ERR_PHONE_NUM_EXIST == gameInfo.player_check_exist(phone))
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_exist), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		
		return true;
	}
	
	/**
	 * 显示玩家列表
	 * */
	private void player_view()
	{
		//检查是否有玩家
		Player player = gameInfo.player_get_first();
		while(player != null)
		{
			Log.d(TAG, "Resume player! id : " + player.id 
					+ ", name : " + player.name 
					+ ", phone : " + player.phone_num);
			
			//添加界面元素
			layout_add_one_player(player);
			
			//下一个玩家
			player = gameInfo.player_get_next();
		}
	}
	
	/**
	 * 添加一名新玩家
	 * 
	 * @param name - 玩家名字
	 * @param phone_num - 玩家手机号
	 * 
	 * @return 若成功，返回true；否则返回false；
	 * */
	private boolean player_add(String name, String phone_num)
	{
		/*判断电话号码有效性*/
		if(Misc.isMobileNO(phone_num) == false)
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		
		/*统一电话号码形式*/
		phone_num = Misc.standardizeMobileNO(phone_num);
		
		/*添加数据以及界面元素*/
		int id = gameInfo.player_add(name, phone_num);
		if(id > 0)										//成功添加玩家
		{
			Player player = gameInfo.player_get(id);
			if(player != null)
			{
				layout_add_one_player(player);
				return true;
			}
			else
			{
				return false;
			}
		}
		else if(Game.ERR_PHONE_NUM_EXIST == id)			//号码已经存在
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_exist), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * 对一名玩家进行设置
	 * 
	 * @param which - 要进行的设置操作
	 * @param id - 操作的用户id号
	 * @param list_index - 某个用户对应的列表项序号
	 * */
	private boolean player_setting(int which, int id, int list_index)
	{
		try
		{
			Map<String, Object> map;
			
			switch(which)
			{
				case PLAYER_SETTING_DELETE:
				{
					gameInfo.player_remove(id);			//设置数据			
					mData.remove(list_index);
					mAdapter.notifyDataSetChanged();
					break;
				}
				
				case PLAYER_SETTING_ATTEND:				//设置参加或者不参加游戏
				{
					if(Player.ROLE_SHIELD == gameInfo.player_get(id).role)		//当前不参加，设置为参加
					{
						gameInfo.player_setRole(id, Player.ROLE_NONE);
						map = mData.get(list_index);
						map.put(KEY_IMG, R.drawable.none);
						mAdapter.notifyDataSetChanged();
					}
					else
					{
						gameInfo.player_setRole(id, Player.ROLE_SHIELD);
						map = mData.get(list_index);
						map.put(KEY_IMG, R.drawable.forbid);
						mAdapter.notifyDataSetChanged();
					}
					break;
				}
				
				default:
				{
					return false;
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e);
		}
		
		return true;
	}
	
	/**
	 * 检查玩家设置是否合法
	 * */
	private boolean player_check()
	{
		if(gameInfo.game_getValidPlayerNum() < Game.MIN_PLAYER_NUM)		//若有效玩家太少，则无法开始游戏
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_not_enough_player), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		else
		{
			return true;
		}
	}
	
	/**
	 * 添加一个玩家到界面上
	 * */
	private boolean layout_add_one_player(final Player player)
	{
		/*判断有效性*/
		if(player == null)
		{
			Log.d(TAG, "[layout_add_one_player()]: player == null , error!");
			return false;
		}
		
		/*设置新元素*/
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_INDEX, player.id);
		map.put(KEY_INFO, player.name + ":" + player.phone_num);
		if(Player.ROLE_SHIELD == player.role)
		{
			map.put(KEY_IMG, R.drawable.forbid);		//显示禁止图片
		}
		else
		{
			map.put(KEY_IMG, R.drawable.none);			//不显示图片
		}
		mData.add(map);
		
		/*通知列表适配器变化*/
		mAdapter.notifyDataSetChanged();
		
		return true;
	}
	
	/**
	 * 显示询问界面
	 * */
	private void ad_show(){
		/*创建提示窗口*/
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.hint_restart_game));
		builder.setIcon(android.R.drawable.ic_dialog_alert);		//使用Android内置图标
		builder.setPositiveButton(			//设置确认按钮
			getString(R.string.button_ok), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//删除所有词汇
					start_new_game();
				}
		});
		builder.setNegativeButton(			//设置取消按钮
			getString(R.string.button_cancel), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//什么都不做
				}
		});
		
		/*显示提示窗口*/
		AlertDialog ad = builder.create();;
		ad.show();
	}
	
	/**
	 * 清除所有玩家及游戏资料，开始新游戏
	 * */
	private void start_new_game() {
		/*重置游戏信息*/
		gameInfo.game_new();
		
		/*重置界面*/
		//重置玩家列表
		mData.removeAll(mData);
		mAdapter.notifyDataSetChanged();
		
		return;
	}
	
	/*--------------------------
	 * 公有方法
	 *------------------------*/
	
}
