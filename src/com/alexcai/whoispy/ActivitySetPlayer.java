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
	 * ����
	 *------------------------*/
	private static final String TAG = "whoispy.PlayerSet";
	
	private static final int PICK_CONTACT_SUBACTIVITY = 2;
	
	//������ö���
	private final int PLAYER_SETTING_DELETE = 0;		//ɾ�����
	private final int PLAYER_SETTING_ATTEND = 1;		//������Ҳμӻ���ʱ���μ���Ϸ
	//List���
    private SimpleAdapter mAdapter;
    private ArrayList<Map<String, Object>> mData = new ArrayList<Map<String, Object>>();
    //Map Key
    private final String KEY_INDEX = "index";
    private final String KEY_INFO = "info";
    private final String KEY_IMG = "img";
    
	//��Ϸ�����Ϣ
	private Game gameInfo;
	
	/*--------------------------
	 * ��д����
	 *------------------------*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		/*���ý���*/
		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.activity_set_player);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_l_text_button_r);
		
		/*���ñ�������*/
		TextView title_text = (TextView)findViewById(R.id.title_text);
		title_text.setText(R.string.title_set_player);
		
		Log.d(TAG, "onCreate, this : " + this);
		
		/*��ȡ��Ϸ��Ϣ*/
		AppGameInfos infos = (AppGameInfos)(getApplicationContext());
		gameInfo = infos.game;
		
		/*������Ϸ�׶�*/
		gameInfo.game_set_step(Game.GAME_STEP_SET_PLAYER);
		
		/*��List*/
        mAdapter = new SimpleAdapter(this,
        							 mData,
        							 R.layout.list_set_player,
        							 new String[]{KEY_INDEX, KEY_IMG, KEY_INFO},
        							 new int[]{R.id.player_index, R.id.player_status_img, R.id.player_info});
        setListAdapter(mAdapter);
                
		/*�󶨽���Ԫ��*/
		//�򿪵绰����ť
    	Button phone_book_button = (Button)findViewById(R.id.phone_book_button);
		phone_book_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				phoneBook_open();
			}
		});
		
		//���������
		final EditText phone_num_text = (EditText)findViewById(R.id.phone_num_text);
		phone_num_text.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if((KeyEvent.KEYCODE_ENTER == keyCode) && (event.getAction() == KeyEvent.ACTION_DOWN))
				{
					/*��ȡ�ֻ���*/
					final String numPhone = phone_num_text.getText().toString();
					
					/*У���ֻ���*/
					if(player_phonenum_check(numPhone) == false)
					{
						return false;
					}
					
					/*Ҫ�������������*/
					final AlertDialog.Builder dlg = new AlertDialog.Builder(ActivitySetPlayer.this).setTitle(getResources().getString(R.string.hint_input_player_name));
					final EditText newText = new EditText(ActivitySetPlayer.this);
					newText.setText(getResources().getString(R.string.player_default_name));
					dlg.setView(newText);
					dlg.setPositiveButton(getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							/*����ϵ����ӵ�����б���*/
							String strName = newText.getText().toString();
							if(player_add(strName, numPhone))
							{
								phone_num_text.setText("");		//��Ӻ����
							}
						}
					});
					dlg.setNegativeButton(getResources().getString(R.string.button_cancel), null);
					dlg.show();
				}
				return false;
			}
		});
		
		//��һ����ť
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
		
		//��һ����ť
		ImageButton prev_button = (ImageButton)findViewById(R.id.title_button_left);
		prev_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(ActivitySetPlayer.this, ActivityMenu.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);		//���Activityջ��ActivityMain֮�ϵ�Activity
				startActivity(i);
				finish();		//Ҫ�ǵùرյ�ǰActivity
				Log.d(TAG, "finish!");
			}
		});
		
		//�м�ĸ����б�ť
		ImageButton center_button = (ImageButton)findViewById(R.id.title_button_center);
		center_button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				ad_show();
			}
		});
		
		//��ʾ֮ǰ������б�
		player_view();
	}
	
	@Override
	protected void onPause() {
		//��������б�XML��
		gameInfo.data_savePlayerList();
		//������Ϸ����
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
			/*��ȡ���id��*/
			String index = mData.get(position).get(KEY_INDEX).toString();
			final int player_id = Integer.parseInt(index);
			final int list_index = position;
			
			/*������ҵ�ǰ״̬������ʾ��Ϣ*/
			CharSequence[] items;
			if(Player.ROLE_SHIELD == gameInfo.player_get(player_id).role)		//����ʱû�μ���Ϸ,����ʾ�ָ��μ���Ϸ
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
	 * ˽�з���
	 *------------------------*/
	/**
	 * �򿪵绰��
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
	 * ��ȡ�绰�����صĽ��
	 * */
	private void phoneBook_getResult(Intent data)
	{
		Uri uriRet;
		String strName;		//��ϵ������
		String numPhone;	//��ϵ�˵绰
		//int typePhone;		//�绰����
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
				/* ����Ҫ��android.permission.READ_CONTACTSȨ�� */
				Cursor c = managedQuery(uriRet, null, null, null, null);
				/* ��Cursor�Ƶ�������ǰ�� */
				c.moveToFirst();
				/* ȡ�������˵����� */
				strName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
				/* ȡ�������˵ĵ绰 */
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
					/* 2.0��������User�趨����绰���룬��������ֻ��һ��绰������ʾ�� */
					numPhone = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
					//typePhone = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
					//resType = ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(typePhone);
					
					/*����ϵ����ӵ�����б���*/
					player_add(strName, numPhone);
				}
				else		//�Ҳ����绰��������ʾ
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
		/*�жϵ绰������Ч��*/
		if(Misc.isMobileNO(phone) == false)
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		
		/*�жϺ����Ƿ��Ѿ�����*/
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
	 * ��ʾ����б�
	 * */
	private void player_view()
	{
		//����Ƿ������
		Player player = gameInfo.player_get_first();
		while(player != null)
		{
			Log.d(TAG, "Resume player! id : " + player.id 
					+ ", name : " + player.name 
					+ ", phone : " + player.phone_num);
			
			//��ӽ���Ԫ��
			layout_add_one_player(player);
			
			//��һ�����
			player = gameInfo.player_get_next();
		}
	}
	
	/**
	 * ���һ�������
	 * 
	 * @param name - �������
	 * @param phone_num - ����ֻ���
	 * 
	 * @return ���ɹ�������true�����򷵻�false��
	 * */
	private boolean player_add(String name, String phone_num)
	{
		/*�жϵ绰������Ч��*/
		if(Misc.isMobileNO(phone_num) == false)
		{
			Toast t = Toast.makeText(this, getResources().getString(R.string.hint_phone_num_invalid), Toast.LENGTH_SHORT);
			t.setGravity(Gravity.CENTER, 0, 0);
			t.show();
			return false;
		}
		
		/*ͳһ�绰������ʽ*/
		phone_num = Misc.standardizeMobileNO(phone_num);
		
		/*��������Լ�����Ԫ��*/
		int id = gameInfo.player_add(name, phone_num);
		if(id > 0)										//�ɹ�������
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
		else if(Game.ERR_PHONE_NUM_EXIST == id)			//�����Ѿ�����
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
	 * ��һ����ҽ�������
	 * 
	 * @param which - Ҫ���е����ò���
	 * @param id - �������û�id��
	 * @param list_index - ĳ���û���Ӧ���б������
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
					gameInfo.player_remove(id);			//��������			
					mData.remove(list_index);
					mAdapter.notifyDataSetChanged();
					break;
				}
				
				case PLAYER_SETTING_ATTEND:				//���òμӻ��߲��μ���Ϸ
				{
					if(Player.ROLE_SHIELD == gameInfo.player_get(id).role)		//��ǰ���μӣ�����Ϊ�μ�
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
	 * �����������Ƿ�Ϸ�
	 * */
	private boolean player_check()
	{
		if(gameInfo.game_getValidPlayerNum() < Game.MIN_PLAYER_NUM)		//����Ч���̫�٣����޷���ʼ��Ϸ
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
	 * ���һ����ҵ�������
	 * */
	private boolean layout_add_one_player(final Player player)
	{
		/*�ж���Ч��*/
		if(player == null)
		{
			Log.d(TAG, "[layout_add_one_player()]: player == null , error!");
			return false;
		}
		
		/*������Ԫ��*/
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(KEY_INDEX, player.id);
		map.put(KEY_INFO, player.name + ":" + player.phone_num);
		if(Player.ROLE_SHIELD == player.role)
		{
			map.put(KEY_IMG, R.drawable.forbid);		//��ʾ��ֹͼƬ
		}
		else
		{
			map.put(KEY_IMG, R.drawable.none);			//����ʾͼƬ
		}
		mData.add(map);
		
		/*֪ͨ�б��������仯*/
		mAdapter.notifyDataSetChanged();
		
		return true;
	}
	
	/**
	 * ��ʾѯ�ʽ���
	 * */
	private void ad_show(){
		/*������ʾ����*/
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.hint_restart_game));
		builder.setIcon(android.R.drawable.ic_dialog_alert);		//ʹ��Android����ͼ��
		builder.setPositiveButton(			//����ȷ�ϰ�ť
			getString(R.string.button_ok), 
			new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//ɾ�����дʻ�
					start_new_game();
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
	 * ���������Ҽ���Ϸ���ϣ���ʼ����Ϸ
	 * */
	private void start_new_game() {
		/*������Ϸ��Ϣ*/
		gameInfo.game_new();
		
		/*���ý���*/
		//��������б�
		mData.removeAll(mData);
		mAdapter.notifyDataSetChanged();
		
		return;
	}
	
	/*--------------------------
	 * ���з���
	 *------------------------*/
	
}
