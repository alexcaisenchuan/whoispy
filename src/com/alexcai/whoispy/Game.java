package com.alexcai.whoispy;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EncodingUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xmlpull.v1.XmlSerializer;

import com.alexcai.whoispy.Err.ExceptionCommon;
import com.alexcai.whoispy.Err.ExceptionNetwork;
import com.alexcai.whoispy.Err.ExceptionNetworkTimeOut;
import com.alexcai.whoispy.Err.ExceptionNotLogin;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Xml;

/**
 * ����࣬���ڱ���һ����ҵ������Ϣ
 * */
class Player {
	/*--------------------------
	 * ����
	 *--------------------------*/
	//private static final String TAG = "whoispy.Player";
	/* ��ɫ���� */
	static final int ROLE_NONE = 0; // �޽�ɫ��һ��ʼ����������
	static final int ROLE_SHIELD = 1; // �ݲ��μ���Ϸ
	static final int ROLE_CIVIL = 2; // ƽ��
	static final int ROLE_SPY = 3; // �Ե�
	static final int ROLE_TRICK = 4; // ���͵�
	/* ״̬���� */
	static final int STATUS_NORMAL = 0; // ��ͨ״̬
	static final int STATUS_OUT = 1; // ��ҳ���

	/*--------------------------
	 * ��̬��Ա
	 *--------------------------*/
	static private int current_id = 1; // ��ǰ����id����������ӵ���ң�Ȼ���Զ�����

	/*--------------------------
	 * ��ͨ��Ա
	 *--------------------------*/
	int id; 			// ����Ψһ����һ�����
	String name; 		// ����
	String phone_num; 	// �ֻ�����
	int role; 			// ��ɫ
	int status; 		// ���״̬��������Ϸ�����Ѿ����֣����ڱ��
	String word;		//���������ҵĴʻ�

	/*--------------------------
	 * ���췽��
	 *------------------------*/
	/**
	 * �������Ĺ��췽��
	 * 
	 * @param name
	 *            - �����
	 * @param phone_num
	 *            - ����ֻ���
	 * */
	public Player(String name, String phone_num) {
		// ��ʼ�����
		this.name = name;
		this.phone_num = phone_num;
		this.role = ROLE_NONE;
		this.status = STATUS_NORMAL;
		this.word = "";
		this.id = current_id;
		current_id++;

	}

	/**
	 * �����������췽��,ʹ��Ĭ�������������ֻ���
	 * */
	public Player() {
		// ���ô������Ĺ��캯��
		this("Someone", "0");
	}
	
	/**
	 * �ж�ĳ����ҽ�ɫ�Ƿ���Ч����������Ϸ������Ч��ң�
	 * @return
	 * */
	public boolean valid(){
		boolean valid;
		
		switch(this.role)
		{
			case ROLE_CIVIL:
			case ROLE_SPY:
			case ROLE_TRICK:
				valid = true;
				break;

			default:
				valid = false;
				break;
		}
		
		return valid;
	}
}

/**
 * �����࣬��Ҫ���ڷ��Ͷ���
 * */
class SMS {
	/*--------------------------
	 * ����
	 *-------------------------*/
	private final static String TAG = "whoispy.SMS";
	public static final String ACTION_SMS_SENT = "com.example.android.apis.os.SMS_SENT_ACTION";

	/*--------------------------
	 * ���췽��
	 *-------------------------*/

	/*--------------------------
	 * ���з���
	 *-------------------------*/
	static void send(String send_to, String msg_text) {
		try {
			Log.d(TAG, ">>>>>>>>>>>Send sms to :" + send_to);
			
			SmsManager sms = SmsManager.getDefault();

			List<String> messages = sms.divideMessage(msg_text);

			String recipient = send_to;

			for (String message : messages)
			{
				sms.sendTextMessage(recipient, null, message, null, null);
			}
		}
		catch (Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
		}

		return;
	}
}

/**
 * �û��࣬��Ҫ�����û���¼�������Ϣ������
 * */
class User {
	/*--------------------------
	 * ����
	 *-------------------------*/
	private final static String TAG = "whoispy.User";
	/*��Ϸ����*/
	final static String PREF_FILE_NAME		= "userPrefs";	//�����ļ���
	final static String PREF_TAG_PHONE 		= "phone";
	final static String PREF_TAG_PASSWD 	= "passwd";
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	String curr_session;			//��ǰsession id
	String curr_nickname;			//��ǰ�û��ǳ�
	int	   remote_rid;				//���������Ƽ��ʻ������rid��ţ��������е���ֹ�...
	int    remote_versionCode;		//��ǰӦ�õ����°汾�ţ��ŵ��ⲻ̫����...
	
	private ContextWrapper context_wrapper;		//Application Context
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	/**
	 * ���췽��
	 * */
	User(ContextWrapper context) {
		curr_session = "";
		curr_nickname = "";
		remote_rid = 0;
		
		this.context_wrapper = context;
	}
	
	/**
	 * ע�����û�
	 * @param  phone �ֻ��ţ�
	 * @param  nickname �ǳƣ�
	 * @param  passwd ���룻
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public int register(String phone, String nickname, String passwd) {
		int ret = Err.NO_ERR;
		
		try {
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone", phone));
			params.add(new BasicNameValuePair("nickname", nickname));
			params.add(new BasicNameValuePair("passwd", passwd));
			params.add(new BasicNameValuePair("mode", "register"));
			
			/*��������*/
			Respon respon = new Respon();
			int flag = Web.req_respon(Web.WEB_URL_USERS, params, respon, 3);
			
			/*���ݺ���ִ��������в�ͬ����*/
			if(Err.NO_ERR == flag)
			{
				JSONObject json = new JSONObject(respon.str);		//���ַ���������JSON��ʽ
				String web_ret = json.getString(Web.FLAG_RET);		//��ȡ�ɹ�����
				if(web_ret.equals(Web.CODE_NO_ERR))
				{
					ret = Err.NO_ERR;
				}
				else
				{
					String web_err_code = json.getString(Web.FLAG_ERR_CODE);
					if(web_err_code.equals(Web.CODE_ERR_PHONE_EXIST))
					{
						ret = Err.ERR_PHONE_EXIST;
					}
					else
					{
						ret = Err.ERR_COMMON;
					}
				}
			}
			else
			{
				ret = flag;
			}
		}
		catch(Exception e){
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ��¼
	 * @param  phone �ֻ���;
	 * @param  passwd ����;
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public int login(String phone, String passwd) {
		int ret = Err.NO_ERR;		//����ֵ
	
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone", phone));
			params.add(new BasicNameValuePair("passwd", passwd));
			params.add(new BasicNameValuePair("mode", "login"));
			
			/*��������*/
			Respon respon = new Respon();
			int flag = Web.req_respon(Web.WEB_URL_USERS, params, respon, 3);
			
			if(Err.NO_ERR == flag)
			{
				JSONObject res = new JSONObject(respon.str);
				String web_ret = res.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))
				{
					//�����û���Ϣ
					saveUser(phone, passwd);
					//��¼session_id
					this.curr_session = res.getString(Web.FLAG_SESSION_ID);
					//��¼�ǳ�
					this.curr_nickname = res.getString(Web.FLAG_NICKNAME);
					//��¼remote_rid
					this.remote_rid = res.getInt(Web.FLAG_REMOTE_RID);
					//��¼version_code
					this.remote_versionCode = res.getInt(Web.FLAG_VERSION_CODE);
					
					ret = Err.NO_ERR;
				}
				else
				{
					String err_code = res.getString(Web.FLAG_ERR_CODE);
					if(err_code.equals(Web.CODE_ERR_PASSWD))
					{
						ret = Err.ERR_PASSWD;
					}
					else
					{
						ret = Err.ERR_COMMON;
					}
				}
			}
			else
			{
				ret = flag;
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * �ǳ�
	 * @return
	 */
	public boolean logout() {
		/*TODO ����Զ����ҳ�����еǳ�*/
		
		/*��ձ����û���Ϣ*/
		this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PHONE, "").commit();
		this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PASSWD, "").commit();

		/*���session��Ϣ*/
		this.curr_session = "";
		
		return true;
	}
	
	/**
	 * �����û���Ϣ
	 * */
	public boolean saveUser(String phone, String passwd) {
		try{
			EncrypAES aes = new EncrypAES();		//���ܶ���
			
			/*����*/
			String encryp_phone = aes.Encrytor(phone);
			String encryp_passwd = aes.Encrytor(passwd);
			
			/*����*/
			this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PHONE, encryp_phone).commit();
			this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PASSWD, encryp_passwd).commit();
		}
		catch(Exception e){
			Log.d(TAG, "Exception : " + e.toString());
			e.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	/**
	 * ��ȡ�����û���Ϣ�����Ե�¼
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public int resumeUser() {
		int ret = Err.ERR_COMMON;		//����ֵ
		
		try{
			/*��ȡ�����ļ�*/
			String saved_phone = this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_PHONE, "");
			String saved_passwd = this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_PASSWD, "");
			
			/*���ֻ��Ż����벻���ڣ�����Ϊ�����򷵻سɹ�*/
			if(saved_phone.equals("") || saved_passwd.equals(""))
			{
				return Err.ERR_COMMON;
			}
			
			/*�����ֻ��ż�����*/
			EncrypAES aes = new EncrypAES();		//���ܶ���
			
			String decryp_phone = aes.Decryptor(saved_phone);
			String decryp_passwd = aes.Decryptor(saved_passwd);
			
			/*���Ե�½*/
			ret = login(decryp_phone, decryp_passwd);
		}
		catch(Exception e){
			Log.d(TAG, "Exception : " + e.toString());
			e.printStackTrace();
			return Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ��ȡ������ǰ�û������дʻ�
	 * @throws ExceptionNetwork 
	 * @throws ExceptionNetworkTimeOut 
	 * @throws ExceptionCommon 
	 * @throws ExceptionNotLogin 
	 * */
	public ArrayList<OneWord> get_all_word() 
		throws ExceptionNetwork, ExceptionNetworkTimeOut, ExceptionCommon, ExceptionNotLogin
	{
		ArrayList<OneWord> word_list = new ArrayList<OneWord>();
		int ret = Word.search(curr_session, word_list);
		
		switch(ret)
		{
			case Err.NO_ERR:
				break;
				
			case Err.ERR_NETWORK:
				throw new Err.ExceptionNetwork();
			
			case Err.ERR_NETWORK_TIMEOUT:
				throw new Err.ExceptionNetworkTimeOut();
				
			case Err.ERR_NOT_LOGIN:
				throw new Err.ExceptionNotLogin();
				
			case Err.ERR_COMMON:
			default:
				throw new Err.ExceptionCommon();
		}
		
		return word_list;
	}
	
	/**
	 * ɾ�������ʻ�
	 * */
	public int del_word(ArrayList<OneWord> word_list){
		/*����JSON�ַ���*/
		JSONArray arr = new JSONArray();
		for(OneWord word : word_list)
		{
			try {
				JSONObject obj = new JSONObject();
				obj.put("wid", word.wid);
				
				arr.put(obj);
			}
			catch(Exception e)
			{
				Log.d(TAG, "Exception : " + e.toString());
			}
		}
		
		/*������ҳ��ɾ���ʻ�*/
		String wid_list = arr.toString();
		HashMap<String, String> ret_list = new HashMap<String, String>();
		//Log.d(TAG, "wid_list : " + wid_list);
		int ret = Word.del_list(curr_session, wid_list, ret_list);
		
		return ret;
	}
}

/**
 * ��Ӵʻ��Ƿ�ɹ��Ľ������
 * */
class RetWordAdd {
	/*--------------------------
	 * ����
	 *-------------------------*/
	String phone;
	String ret;
	String err_code;
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	/**
	 * ���췽��
	 * */
	public RetWordAdd(String phone, String ret, String err_code) {
		this.phone = phone;
		this.ret = ret;
		this.err_code = err_code;
	}
}

/**
 * һ���ʻ㣬���а�����wid�������ˡ��ʻ����ݡ�����ʱ�����Ϣ
 * */
class OneWord {
	/*--------------------------
	 * ����
	 *-------------------------*/
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	String wid;		//�����ݿ��еļ�¼
	String sender;	//������
	String word;	//�ʻ�����
	String time;	//����ʱ��
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	/**
	 * ���췽��
	 * */
	OneWord(String wid, String sender, String word, String time) {
		this.wid = wid;
		this.sender = sender;
		this.word = word;
		this.time = time;
	}
}

/**
 * �ʻ��࣬��Ҫ�ṩ�ʻ����ӡ���ѯ��ɾ���Ȳ�����WEB�ϵĲ�����
 * */
class Word {
	/*--------------------------
	 * ����
	 *-------------------------*/
	private final static String TAG = "whoispy.User";
	/*TAGs*/
	private final static String TAG_WID = "wid";
	private final static String TAG_WORD = "word";
	private final static String TAG_SENDER = "sender";
	private final static String TAG_TIME = "time";
	private final static String TAG_IS_DEL = "is_del";
	

	/*--------------------------
	 * ����
	 *-------------------------*/
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	/**
	 * ��Ӵʻ�
	 * @param phone_to - Ҫ�������ֻ���
	 * @param word - Ҫ���Ĵʻ�
	 * @param session_id - ��ǰ��¼�û���session_id
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public static int add(String session_id, String phone_to, String word){
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone_to", phone_to));
			params.add(new BasicNameValuePair("word", word));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "add"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//��ӳɹ�
				{
					ret = Err.NO_ERR;
				}
				else									//���ʧ��
				{
					ret = Err.ERR_COMMON;
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ��Ӷ���ʻ�
	 * @param session_id - ��ǰ��¼�û���session_id
	 * @param word_list - Ҫ�����Ĵʻ��б�json��ʽ��װ
	 * @param ret_list - �����������һ��HashMap������Key���ֻ��ţ�Value��success��error
	 * */
	public static int add_list(String session_id, 
			                   String word_list, 
			                   HashMap<String, String> ret_list){
		
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("word_list", word_list));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "add_list"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//��ӳɹ�
				{
					//��ȡ��ӽ������
					JSONArray arr = json.getJSONArray("ret_word_add");
					//���ζ�ȡ��������
					for(int i = 0; i < arr.length(); i++)
					{
						JSONObject one_ret = arr.getJSONObject(i);
						
						//��ȡ������Ϣ
						String phone = one_ret.getString("phone");
						String ret_t = one_ret.getString("ret");
						
						//��ӵ������б���
						ret_list.put(phone, ret_t);
					}
				}
				else									//���ʧ��
				{
					String err_code = json.getString(Web.FLAG_ERR_CODE);
					if(err_code.equals(Web.CODE_ERR_NOT_LOGIN))		//��û�е�¼
					{
						ret = Err.ERR_NOT_LOGIN;
					}
					else
					{
						ret = Err.ERR_COMMON;
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ��ѯ������ǰ�û������дʻ�
	 * @param session_id - ��ǰ��¼�û���session_id
	 * @param word_list - ���ز��������ڱ����ѯ���Ĵʻ��б�
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public static int search(String session_id, ArrayList<OneWord> word_list){
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "search"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//��ѯ�ɹ�
				{
					JSONArray json_word_list = json.getJSONArray("word_list");
					for(int i = 0; i < json_word_list.length(); i++)
					{
						//��json�ַ����н��������ֶ�
						JSONObject json_word = json_word_list.getJSONObject(i);
						String wid = json_word.getString(TAG_WID);
						String sender = json_word.getString(TAG_SENDER);
						String word = json_word.getString(TAG_WORD);
						String time = json_word.getString(TAG_TIME);
						String is_del = json_word.getString(TAG_IS_DEL);
						
						Log.d(TAG, "----------------------");
						Log.d(TAG, "wid : " + wid);
						Log.d(TAG, "sender : " + sender);
						Log.d(TAG, "word : " + word);
						Log.d(TAG, "time : " + time);
						Log.d(TAG, "is_del : " + is_del);
						
						//�����µ�OneWord����
						OneWord one_word = new OneWord(wid, sender, word, time);
						
						//������ӵ�������
						word_list.add(one_word);
					}
				}
				else									//��ѯʧ��
				{
					String err_code = json.getString(Web.FLAG_ERR_CODE);
					if(err_code.equals(Web.CODE_ERR_NOT_LOGIN))		//δ��¼
					{
						ret = Err.ERR_NOT_LOGIN;
					}
					else
					{
						ret = Err.ERR_COMMON;
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ɾ�����ݿ��е�ĳ���ʻ�
	 * */
	public static int del(String session_id, String wid){
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("wid", wid));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "del"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//ɾ���ɹ�
				{
					ret = Err.NO_ERR;
				}
				else									//ɾ��ʧ��
				{
					ret = Err.ERR_COMMON;
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ɾ������ʻ�
	 * @param session_id - ��ǰ��¼�û���session_id
	 * @param wid_list - Ҫɾ���Ĵʻ��б�json��ʽ��װ
	 * @param ret_list - �����������һ��HashMap������Key���ֻ��ţ�Value��success��error
	 * */
	public static int del_list(String session_id,
							   String wid_list, 
			                   HashMap<String, String> ret_list){
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("wid_list", wid_list));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "del_list"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//ɾ���ɹ�
				{
					//��ȡ��ӽ������
					JSONArray arr = json.getJSONArray("ret_word_del");
					//���ζ�ȡ��������
					for(int i = 0; i < arr.length(); i++)
					{
						JSONObject one_ret = arr.getJSONObject(i);
						
						//��ȡ������Ϣ
						String wid = one_ret.getString("wid");
						String ret_t = one_ret.getString("ret");
						
						//��ӵ������б���
						ret_list.put(wid, ret_t);
					}
				}
				else									//ɾ��ʧ��
				{
					ret = Err.ERR_COMMON;
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ���ʻ��б�ת����json�ַ���
	 * @param word_list - Ҫ����Ĵʻ��б�
	 * @return
	 * */
	public static String array_to_string(ArrayList<OneWord> word_list) throws JSONException{

		/*����JSON����*/
		JSONArray arr = new JSONArray();
		
		/*���ζ�ȡ�����ʻ㣬��ӵ�������*/
		for(OneWord element : word_list)
		{
			JSONObject obj = new JSONObject();
			obj.put(TAG_WID, element.wid);
			obj.put(TAG_WORD, element.word);
			obj.put(TAG_SENDER, element.sender);
			obj.put(TAG_TIME, element.time);
			
			arr.put(obj);
		}
		
		/*ת�����ַ���*/
		String str = arr.toString();
		//Log.d(TAG, "Json str : " + str);
		
		return str;
	}
	
	/**
	 * ��json�ַ����лָ����ʻ��б�
	 * @param str - Ҫ�ָ���json�ַ���
	 * */
	public static ArrayList<OneWord> string_to_array(String str) throws JSONException{
		
		ArrayList<OneWord> word_list = new ArrayList<OneWord>();
		JSONArray arr = new JSONArray(str);		//���ַ�������json����
	
		/*���ζ�ȡ�����ʻ������*/
		for(int i = 0; i < arr.length(); i++)
		{
			JSONObject obj = arr.getJSONObject(i);
			String wid = obj.getString(TAG_WID);
			String word = obj.getString(TAG_WORD);
			String sender = obj.getString(TAG_SENDER);
			String time = obj.getString(TAG_TIME);
			
			/*��Ӵʻ㵽�б���*/
			OneWord one_word = new OneWord(wid, sender, word, time);
			word_list.add(one_word);
		}
		
		return word_list;
	}
}

/**
 * �Ƽ��ʻ��࣬���Ƽ��ʻ���ص������Լ������������ڴ�����
 * */
class RecommendWord {
	/*--------------------------
	 * ����
	 *-------------------------*/
	private final String TAG = "whoispy.RecommendWord";
	/*��ҳ�����ֶ�*/
	private String TAG_CIVIL_WORD = "c";
	private String TAG_TRICK_WORD = "t";
	private String TAG_CLIENT_RID = "client_rid";
	private String TAG_WORD_LIST = "word_list";
	/*�����ļ���*/
	private String FILE_NAME_RECOMMEND_WORD = "recommend_word.json";
	
	/*--------------------------
	 * �ڲ���
	 *-------------------------*/
	/**
	 * �ʻ���
	 * */
	class WordPair {
		String civil_word;		//���˴ʻ�
		String trick_word;		//ɵ�Ӵʻ�
		
		public WordPair(String civil_word, String trick_word)
		{
			this.civil_word = civil_word;
			this.trick_word = trick_word;
		}
	}
	
	/*--------------------------
	 * ����
	 *-------------------------*/
	private ContextWrapper context_wrapper;		//Ӧ�ó��������ģ����ڶ�д�����ļ���
	private int current_index = 0;				//��¼��ȡ���ĸ����ʣ������������ȡ������ʹ��˳���ȡ��ʽ
	private int client_rid = 0;					//��ǰ���µĴʻ���rid
	private int remote_rid = 0; 				//�����������µĴʻ���rid
	private String session_id; 					//��ǰ�Ựsession_id��
	private ArrayList<WordPair> word_pair_list = new ArrayList<WordPair>(); 		//���õĴʻ����б�

	/*--------------------------
	 * ����
	 *-------------------------*/
	public RecommendWord(ContextWrapper context_wrapper) {
		this.context_wrapper = context_wrapper;
	}
	
	/**
	 * �Ƿ���Ҫ��ȡԶ�̴ʻ�
	 * @return true - ��Ҫ�� false - ����Ҫ��
	 * */
	public boolean need_to_read_remote_words()
	{
		Log.d(TAG, "client_rid : " + client_rid + ", remote_rid : " + remote_rid);
		if(client_rid < remote_rid)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * ��ȡ�������ϵĴ���
	 * @return ������룻
	 * */
	public int get_remote_word()
	{
		int ret = Err.NO_ERR;
		
		try{
			/*���ò���*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("session_id", this.session_id));
			params.add(new BasicNameValuePair("client_rid", String.valueOf(this.client_rid)));
			params.add(new BasicNameValuePair("mode", "recommend_search"));
			
			/*��ʼ����*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*���ݷ��ؽ��������*/
			if(Err.NO_ERR == ret)
			{
				//����JSON����
				JSONObject json = new JSONObject(respon.str);
				
				//��ȡ�ɹ���־
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//��ѯ�ɹ�
				{
					//���±���rid�Լ�Զ��rid��ֵ
					int rid = json.getInt(Web.FLAG_REMOTE_RID);
					this.remote_rid = rid;
					this.client_rid = rid;
					
					//����ʻ��б�
					JSONArray json_word_list = json.getJSONArray(Web.FLAG_WORD_LIST);
					for(int i = 0; i < json_word_list.length(); i++)
					{
						//��json�ַ����н��������ֶ�
						JSONObject json_word = json_word_list.getJSONObject(i);
						String civil_word = json_word.getString(TAG_CIVIL_WORD);
						String trick_word = json_word.getString(TAG_TRICK_WORD);
						
						Log.d(TAG, "----------------------");
						Log.d(TAG, "civil_word : " + civil_word);
						Log.d(TAG, "trick_word : " + trick_word);
						
						//�����µ�OneWord����
						WordPair one_word = new WordPair(civil_word, trick_word);
						
						//������ӵ�������
						this.word_pair_list.add(one_word);
					}
				}
				else									//��ѯʧ��
				{
					ret = Err.ERR_COMMON;
				}
			}
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ����remote_rid��session_id
	 * */
	public void set_server_data(int remote_rid, String session_id)
	{
		this.remote_rid = remote_rid;
		this.session_id = session_id;
	}
	
	/**
	 * ��ȡһ��ʻ�
	 * @param word - ��������������ѯ���Ĵʻ㣻
	 * @return ������룻
	 * */
	public int read_word(WordPair word) {
		int ret = Err.NO_ERR;
		
		/*�ȸ��ʻ㸳Ĭ��ֵ*/
		word.civil_word = "";
		word.trick_word = "";
		
		/*��ѡ�ʻ�*/
		if(word_pair_list.size() > 0)
		{
			if(current_index >= word_pair_list.size())
			{
				current_index = 0;
			}
			
			word.civil_word = word_pair_list.get(current_index).civil_word;
			word.trick_word = word_pair_list.get(current_index).trick_word;
			
			current_index++;
		}
		
		return ret;
	}
	
	/**
	 * ���ļ��ж�ȡ�Ƽ�������б�
	 * */
	public void read_words_from_file()
	{
		try {
			/*��ȡ�ļ�*/
			InputStream fin = context_wrapper.openFileInput(FILE_NAME_RECOMMEND_WORD);
			int len = fin.available();
			byte buff[] = new byte[len];
			fin.read(buff);
			String str = EncodingUtils.getString(buff, "utf-8");
			fin.close();
			
			/*����json����*/
			JSONObject json = new JSONObject(str);
			
			/*����client_rid*/
			this.client_rid = json.getInt(TAG_CLIENT_RID);
			Log.d(TAG, "client_rid : " + this.client_rid);
			
			/*�����ʻ��б�*/
			JSONArray array = json.getJSONArray(TAG_WORD_LIST);
			ArrayList<WordPair> word_pair_list = new ArrayList<RecommendWord.WordPair>();
			String civil_word;
			String trick_word;
			for(int i = 0; i < array.length(); i++)
			{
				JSONObject element = array.getJSONObject(i);
				civil_word = element.getString(TAG_CIVIL_WORD);
				trick_word = element.getString(TAG_TRICK_WORD);
				
				WordPair word = new WordPair(civil_word, trick_word);
				word_pair_list.add(word);
				
				//Log.d(TAG, "add word pair : " + civil_word + ", " + trick_word);
			}
			this.word_pair_list = word_pair_list;
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e);
			e.printStackTrace();
		}
		
		return;
	}
	
	/**
	 * ���Ƽ������б�洢���ļ���
	 * */
	public void save_words_to_file()
	{
		try{
			/*��rid���ʻ��б�ת����json��ʽ*/
			JSONObject json = new JSONObject();
			//client_rid
			json.put(TAG_CLIENT_RID, this.client_rid);
			//�ʻ��б�
			JSONArray array = new JSONArray();
			for(WordPair element : word_pair_list)
			{
				JSONObject obj = new JSONObject();
				obj.put(TAG_CIVIL_WORD, element.civil_word);
				obj.put(TAG_TRICK_WORD, element.trick_word);
				
				array.put(obj);
			}
			json.put(TAG_WORD_LIST, array);
			//to string
			String str = json.toString();
			//Log.d(TAG, "json : " + str);
			
			/*������ļ�*/
			OutputStream os = context_wrapper.openFileOutput(FILE_NAME_RECOMMEND_WORD, Context.MODE_PRIVATE);
			OutputStreamWriter ow = new OutputStreamWriter(os);
			ow.write(str);
			ow.close();
			os.close();
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			e.printStackTrace();
		}
		
		return;
	}
}

/**
 * ��Ϸ�࣬�ṩ��Ϸ�����Ϣ�洢�Լ���Ϸ���̿��Ƶĺ���
 * */
public class Game {
	/*--------------------------
	 * ����
	 *-------------------------*/
	private final static String TAG = "whoispy.Game";

	/* ������� */
	final static int NO_ERR = 0; // �޴��󣬳ɹ�
	final static int ERR_SET = -1; // ���ô���
	final static int ERR_GET = -2; // ��ѯ����
	final static int ERR_VAL = -3; // ����Ĳ���
	final static int ERR_PHONE_NUM_EXIST = -4; // �����Ѵ���
	final static int ERR_PLAYER_NOT_FIND = -5; // δ�ҵ���Ӧ���

	/* ���ó��� */
	final static int MIN_PLAYER_NUM = 2; // ��С��Ҹ���

	/*��Ϸ�׶�*/
	final static int GAME_STEP_SET_PLAYER 	= 0;		//�������
	final static int GAME_STEP_SET_ROLE_NUM = 1;		//���ý�ɫ����
	final static int GAME_STEP_SET_WORD		= 2;		//���õ���
	final static int GAME_STEP_GAME_VIEW	= 3;		//��Ϸ����
	
	/*��Ϸ����*/
	final static String PREF_FILE_NAME		= "gamePrefs";	//�����ļ���
	final static String PREF_TAG_CIVIL_WORD = "civil_word";
	final static String PREF_TAG_TRICK_WORD = "trick_word";
	final static String PREF_TAG_SPY_NUM	= "spy_num";
	final static String PREF_TAG_TRICK_NUM  = "trick_num";
	final static String PREF_TAG_ROUND      = "round";
	final static String PREF_TAG_STEP       = "step";
	
	/* ����б��� */
	// �洢�ļ�
	private static final String XML_FILE_USER_LIST = "user_list.xml";
	// TAG
	private static final String XML_TAG_NAMESPACE = ""; // �����ռ�
	private static final String XML_TAG_ROOT = "player_list"; // ���ڵ�
	private static final String XML_TAG_PLAYER = "player";
	private static final String XML_TAG_NAME = "name";
	private static final String XML_TAG_PHONE_NUM = "phone_num";
	private static final String XML_TAG_ROLE = "role";
	private static final String XML_TAG_STATUS = "status";
	
	/*�ʻ��б���*/
	private static final String FILE_WORD_LIST = "word_list.json";		//�����ļ���
	
	/*--------------------------
	 * �ڲ���
	 *-------------------------*/
	/**
	 * ����б�XML�ļ��Ĵ�����
	 * */
	class PlayerListHandle extends DefaultHandler {
		private static final String TAG = "whoispy.Game.UserHandle";

		private ArrayList<Player> player_list; // ��ʱ��ŵ�����б�
		private String current_tag = ""; // ��ǰ���ڷ��ʵ�TAG
		private Player player_new = null; // ��ʱ����Ҷ���

		@Override
		public void startDocument() throws SAXException {
			//Log.d(TAG, "~~~~~~~~~~~~~~~~~startDocument~~~~~~~~~~~~~~");
			player_list = new ArrayList<Player>();
			super.startDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			//Log.d(TAG, "---------startElement(" + localName + ")-----------");

			// ���� TAG NAME
			this.current_tag = localName;

			// ����player TAG,�򴴽������
			if (localName.equals(XML_TAG_PLAYER)) {
				this.player_new = new Player();
			}

			super.startElement(uri, localName, qName, attributes);
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {

			/* ��������Ϣ */
			String str = new String(ch, start, length); // ��ȡ��ǩ�������
			//Log.d(TAG, str);
			if (this.current_tag != "") {
				if (this.current_tag.equals(XML_TAG_NAME)) // name
				{
					// ��������
					this.player_new.name = str;
				} else if (this.current_tag.equals(XML_TAG_PHONE_NUM)) // phone_num
				{
					// ����绰����
					this.player_new.phone_num = str;
				} else if (this.current_tag.equals(XML_TAG_ROLE)) // role
				{
					// �����ɫ
					this.player_new.role = Integer.parseInt(str);
				} else if (this.current_tag.equals(XML_TAG_STATUS)) // status
				{
					// ����״̬
					this.player_new.status = Integer.parseInt(str);
				} else {
					Log.d(TAG, "Unknown tag : " + this.current_tag);
				}
			}

			super.characters(ch, start, length);
		}

		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			//Log.d(TAG, "---------endElement(" + localName + ")-----------");

			/* ����������ǩ�����һ����� */
			if (localName.equals(XML_TAG_PLAYER)) {
				player_list.add(player_new);
				/*Log.d(TAG, "name " + player_new.name + ", phone "
						+ player_new.phone_num + ", role " + player_new.role
						+ ", status " + player_new.status);*/
			}

			super.endElement(uri, localName, qName);
		}

		@Override
		public void endDocument() throws SAXException {
			//Log.d(TAG, "~~~~~~~~~~~~~~~~~endDocument~~~~~~~~~~~~~~");

			super.endDocument();
		}

		/*--------------------
		 * ���⺯��
		 * */
		/**
		 * ���ع���õ�player_list,�˺���Ӧ�������xml���������
		 * */
		public ArrayList<Player> getPlayerList() {
			return player_list;
		}
	}

	/*--------------------------
	 * ����
	 *-------------------------*/
	/* �û���Ϣ */
	User user;									//��ǰ�û�
	/* �Ƽ��ʻ���Ϣ */
	RecommendWord recommend_word;
	/* �û��Ƿ�����������һ��Ӧ�������У�ֻ����ʾһ����ʾ������Ϣ */
	boolean want_update = true;					//Ĭ�������������һ�κ�Ͳ���������
	
	/* ��Ϸ��Ϣ */
	private ArrayList<Player> player_list;		//����б�
	private String civil_word;					//ƽ��ʻ�
	private String trick_word;					//���͵Ĵʻ�
	private int spy_num;						//�Ե���Ŀ
	private int trick_num;						//������Ŀ
	private int round;							//�غ�
	private int step;							//��Ϸ����
	
	/* ���� */
	private int cursor_player; 					// ������Ҷ�ȡ���α�
	private ContextWrapper context_wrapper;		//Application Context

	/*--------------------------
	 * ���췽��
	 *-------------------------*/
	public Game(ContextWrapper wrapper) {
		/*����Application������*/
		this.context_wrapper = wrapper;
		
		/*�����û���Ϣ*/
		this.user = new User(wrapper);
		
		/*�����Ƽ��ʻ�*/
		this.recommend_word = new RecommendWord(wrapper);
		
		/*������Ϸ��Ϣ*/
		game_info_reset();
	}

	/*--------------------------
	 * ˽�з���
	 *-------------------------*/
	/**
	 * �����ɫ
	 * */
	private void role_allocate() {
		int player_index = 0;

		// ��������Ч��ҵ�id�洢����ʱ������
		ArrayList<Integer> player_id_list = new ArrayList<Integer>();
		for (Player element : player_list) {
			if (element.role != Player.ROLE_SHIELD) // ����Ҳμ���Ϸ
			{
				player_id_list.add(element.id);
			}
		}

		// �������������
		Random r = new Random();

		// ��ѡ�Ե�
		for (int i = 0; i < this.spy_num; i++) {
			// ���ѡ�����
			player_index = r.nextInt(player_id_list.size()); // ͨ��ȡ�෽�����Ʒ�Χ
			Log.d(TAG, "spy " + i + ", player_index : " + player_index
					+ ", player_num : " + player_id_list.size());
			// ����������
			player_setRole(player_id_list.get(player_index), Player.ROLE_SPY);
			// ��ѡ�е���ҴӶ���ɾ��
			player_id_list.remove(player_index);
		}

		// ��ѡ���͵�
		for (int i = 0; i < this.trick_num; i++) {
			// ���ѡ�����
			player_index = r.nextInt(player_id_list.size()); // ͨ��ȡ�෽�����Ʒ�Χ
			Log.d(TAG, "trick " + i + ", player_index : " + player_index
					+ ", player_num : " + player_id_list.size());
			// ����������
			player_setRole(player_id_list.get(player_index), Player.ROLE_TRICK);
			// ��ѡ�е���ҴӶ���ɾ��
			player_id_list.remove(player_index);
		}

		// ʣ�µ���ƽ��
		for (int i = 0; i < player_id_list.size(); i++) {
			// ����������
			player_setRole(player_id_list.get(i), Player.ROLE_CIVIL);
		}

		return;
	}

	/**
	 * ���������ݽ����Ӧ�Ĵʻ����ú�
	 * */
	private void word_set() {
		for(Player player : this.player_list)
		{
			switch (player.role)
			{
				case Player.ROLE_CIVIL:
					player.word = new String("Round " + this.round + ": " + this.civil_word);
					break;

				case Player.ROLE_SPY:
					player.word = new String("Round " + this.round + ": ");
					break;

				case Player.ROLE_TRICK:
					player.word = new String("Round " + this.round + ": " + this.trick_word);
					break;

				default:
					player.word = "";
					break;
			}
		}
		
		return;
	}
	
	/**
	 * ������Ϸ��Ϣ
	 * */
	private void game_info_reset()
	{
		player_list 	= new ArrayList<Player>();
		civil_word 		= new String("");
		trick_word 		= new String("");
		spy_num 		= 0;
		trick_num 		= 0;
		round 			= 0;
		cursor_player 	= 0;
	}
	
	/*--------------------------
	 * ���з���
	 *-------------------------*/
	/* =================�û����================ */
	/**
	 * �û���¼
	 * */
	public int user_login(String phone, String passwd){
		int ret = user.login(phone, passwd);		//���е�¼
		recommend_word.set_server_data(user.remote_rid, user.curr_session);		//�����Ƽ��ʻ��������
		
		return ret;
	}
	
	/**
	 * �û��ָ���¼
	 * */
	public int user_resume(){
		int ret = user.resumeUser();		//���е�¼
		recommend_word.set_server_data(user.remote_rid, user.curr_session);		//�����Ƽ��ʻ��������
		
		return ret;
	}
	
	/* =================������================ */
	/**
	 * ��������
	 * 
	 * @param name
	 *            - �������
	 * @param phone_num
	 *            - ����ֻ�����
	 * 
	 * @return ���ɹ������������ҵ�id�ţ�> 0 ��ʧ�ܣ�����С��0�Ĵ������
	 * */
	public int player_add(String name, String phone_num) {
		for (Player element : player_list) {
			if (element.phone_num.equals(phone_num)) // ���绰�����Ѿ�����,�򷵻ش���
			{
				return ERR_PHONE_NUM_EXIST;
			}
		}

		// ��������ң���ӵ�����б��У�������id
		Player player = new Player(name, phone_num);
		player_list.add(player);

		return player.id;
	}

	/**
	 * ���ĳ�������Ƿ����
	 * */
	public int player_check_exist(String phone_num) {
		for (Player element : player_list) {
			if (element.phone_num.equals(phone_num)) // ���绰�����Ѿ�����,�򷵻ش���
			{
				return ERR_PHONE_NUM_EXIST;
			}
		}

		return NO_ERR;
	}

	/**
	 * ɾ��һ�����
	 * 
	 * @param id
	 *            - Ҫɾ����ҵ�id��
	 * 
	 * @return ���ɹ�����NO_ERR�����Ҳ�����ҷ���ERR_PLAYER_NOT_FIND��
	 * */
	public int player_remove(int id) {
		for (Player element : player_list) {
			if (id == element.id) {
				player_list.remove(element); // ɾ������
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * ����һ����ҵĽ�ɫ
	 * 
	 * @param id
	 *            - Ҫ���õ����
	 * @param role
	 *            - Ҫ���õĽ�ɫ
	 * 
	 * @return ���ɹ�����NO_ERR�����Ҳ�����ҷ���ERR_PLAYER_NOT_FIND��
	 * */
	public int player_setRole(int id, int role) {
		for (Player element : player_list) {
			if (id == element.id) // ������Host
			{
				element.role = role;
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * ����һ����ҵ�״̬
	 * 
	 * @param id
	 *            - Ҫ���õ����
	 * @param status
	 *            - Ҫ���õ�״̬
	 * 
	 * @return ���ɹ�����NO_ERR�����Ҳ�����ҷ���ERR_PLAYER_NOT_FIND��
	 * */
	public int player_setStatus(int id, int status) {
		for (Player element : player_list) {
			if (id == element.id) // ������Host
			{
				element.status = status;
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * �������id��ȡ��Ҷ���
	 * 
	 * @param id
	 *            - ���id
	 * 
	 * @return ���ɹ������ض�Ӧ����Ҷ��󣻷��򣬷���null��
	 * */
	public Player player_get(int id) {
		for (Player element : player_list) {
			if (element.id == id) {
				return element;
			}
		}

		Log.d(TAG, "Couldn't find player by id : " + id);

		return null;
	}

	public void player_set(int id, Player player) {

	}

	/**
	 * �õ�����б�ĵ�һ�����
	 * 
	 * @return ���ɹ������ض�Ӧ����Ҷ�����ʧ�ܣ��򷵻�null��
	 * */
	public Player player_get_first() {
		if (player_list.size() > 0) // ������Ԫ�ز���
		{
			cursor_player = 0;
			return player_list.get(0);
		} else // ��û��Ԫ���򷵻�null
		{
			return null;
		}
	}

	/**
	 * ��ȡ��һ����ң����ڱ�������б�Game����Լ�ά��һ����ǰ�α�
	 * 
	 * @return ���ɹ������ض�Ӧ����Ҷ�����ʧ�ܣ��򷵻�null��
	 * */
	public Player player_get_next() {
		if (cursor_player < (player_list.size() - 1)) // ���ܳ����߽�
		{
			cursor_player++;
			return player_list.get(cursor_player);
		} else // �������߽��򷵻�null
		{
			return null;
		}
	}

	
	/**
	 * ��ĳ����ҷ������Ż����ݿ��¼����֪�����
	 * 
	 * @param player
	 *            - Ҫ���������
	 * 
	 * @return NO_ERR : �ɹ�; ����������� : �д���ʧ�ܣ�
	 * */
	public int player_send_word(Player player) {
		int ret = Err.NO_ERR;

		//�������Ч���򷢳��ʻ�
		if(player.valid())
		{
			String word = player.word; 					//��������
			String phone_num = player.phone_num;		//�ֻ�����
			
			//���������ҳ�������ʻ�
			ret = Word.add(user.curr_session, phone_num, word);
			switch(ret)
			{
				case Err.NO_ERR:
				case Err.ERR_NETWORK:			//������󲻷�����
				case Err.ERR_NETWORK_TIMEOUT:
					break;
					
				case Err.ERR_COMMON:			//�û������ߣ��򷢶���
				default:
					SMS.send(phone_num, word);
					break;
			}
		}

		return ret;
	}
	
	
	/* =================��Ϸ���================ */
	/**
	 * ���ص�ǰ�ǵڼ���
	 * */
	public int game_getRound() {
		return round;
	}

	/**
	 * ���Ӿ���
	 * */
	public void game_addRound() {
		round++;
	}
	
	/**
	 * ����ƽ��Ĵʻ�
	 * */
	public void game_setCivilWord(String word) {
		this.civil_word = word;
	}

	/**
	 * ���ô��͵Ĵʻ�
	 * */
	public void game_setTrickWord(String word) {
		this.trick_word = word;
	}

	/**
	 * ��ѯƽ��Ĵʻ�
	 * */
	public String game_getCivilWord() {
		return this.civil_word;
	}

	/**
	 * ��ѯ���͵Ĵʻ�
	 * */
	public String game_getTrickWord() {
		return this.trick_word;
	}

	/**
	 * ������Ч��Ҹ���
	 * 
	 * @return ��Ч��Ҹ���
	 * */
	public int game_getValidPlayerNum() {
		int valid_num = 0;

		for (Player element : player_list) {
			if (element.role != Player.ROLE_SHIELD) // ֻҪ���ǲ��μ���Ϸ����ң�������Ч���
			{
				valid_num++;
			}
		}

		Log.d(TAG, "Valid player num : " + valid_num);

		return valid_num;
	}

	/**
	 * �����Ե׵ĸ���
	 * 
	 * @param spy_num
	 *            - �Ե׵ĸ�����
	 * */
	public void game_setSpyNum(int spy_num) {
		this.spy_num = spy_num;
	}

	/**
	 * ���ô��͵ĸ���
	 * 
	 * @param trick_num
	 *            - ���͵ĸ�����
	 * */
	public void game_setTrickNum(int trick_num) {
		this.trick_num = trick_num;
	}

	/**
	 * ��ȡ�Ե׵ĸ���
	 * 
	 * @return �����Ե׵ĸ�����
	 * */
	public int game_getSpyNum() {
		return this.spy_num;
	}

	/**
	 * ��ȡ���͵ĸ���
	 * 
	 * @return ���ش��͵ĸ�����
	 * */
	public int game_getTrickNum() {
		return this.trick_num;
	}

	/**
	 * ���������ɫ�ĸ���
	 * */
	public void game_calcRoleNum() {
		int valid_num = game_getValidPlayerNum();

		if (valid_num < 2) // 0~1�ˣ���
		{
			spy_num = 0;
			trick_num = 0;
		} else if (valid_num <= 5) // 0~5��
		{
			spy_num = 1;
			trick_num = 0;
		} else if (valid_num <= 10) // 6~10��
		{
			spy_num = 1;
			trick_num = 1;
		} else if (valid_num <= 15) // 11~15��
		{
			spy_num = 2;
			trick_num = 1;
		} else if (valid_num <= 20) // 16~20��
		{
			spy_num = 3;
			trick_num = 2;
		} else // 20������
		{
			spy_num = (valid_num / 8) + 1;
			trick_num = spy_num - 1;
		}
	}

	/**
	 * ������Ϸ����Ҫ���н�ɫ������ʻ�����
	 * */
	public boolean game_setting() {
		/* �����ã���ӡ������Ϣ 
		for (Player element : player_list) {
			Log.d(TAG, "id : " + element.id + ", name : " + element.name
					+ ", phone_num : " + element.phone_num + ", role : "
					+ element.role);
		}
		Log.d(TAG, "spy_num : " + this.spy_num);
		Log.d(TAG, "trick_num : " + this.trick_num);
		Log.d(TAG, "civil_word : " + this.civil_word);
		Log.d(TAG, "trick_word : " + this.trick_word);*/

		/* ����++ */
		round++;
		
		/* �������״̬�ָ� */
		for(Player element : player_list)
		{
			element.status = Player.STATUS_NORMAL;
		}
		
		/* �����ɫ */
		role_allocate();

		/* ����ʻ� */
		word_set();
		
		return true;
	}

	/**
	 * ���¿�ʼһ����Ϸ
	 * */
	public void game_replay() {
		/* ���ý�ɫ��Ŀ�Լ��ʻ� */
		civil_word = "";
		trick_word = "";

		/* �������״̬ */
		for (Player element : player_list) {
			element.status = Player.STATUS_NORMAL;
		}

		return;
	}

	/**
	 * ������Ϸ��Ϣ
	 * */
	public void game_new() {
		game_info_reset();
	}

	/**
	 * ������Ϸ
	 * */
	public void game_continue() {
		data_readGamePref();						//��ȡ��Ϸ����
		
		recommend_word.read_words_from_file();		//��ȡ�Ƽ��ʻ��б�
		
		if(!data_readPlayerList())					//��XML��ȡ����б�
		{
			game_info_reset();						//��ʧ�ܣ���λ��Ϸ��Ϣ
		}
	}
	
	/**
	 * ������Ϸ�׶�
	 * */
	public void game_set_step(int step){
		Log.d(TAG, "Set game step : " + step);
		
		this.step = step;
	}

	/**
	 * ��ȡ��Ϸ���ڵĽ׶�
	 * */
	public int game_get_step(){
		return this.step;
	}
	
	/**
	 * ��������ҷ�������
	 * @return NO_ERR : �ɹ�; ����������� : �д���ʧ�ܣ�
	 * */
	public int game_send_word_to_all() {
		int ret = Err.NO_ERR;
		
		/*����json�ı������а���������ҵ��ֻ��ż��ʻ�*/
		JSONArray arr = new JSONArray();
		
		/*���α����������*/
		for(Player player : player_list)
		{
			/*�ж���ҽ�ɫ�Ƿ���Ч*/
			if(!player.valid())
			{
				continue;
			}
			
			/*����json����*/
			try {
				JSONObject obj = new JSONObject();
				obj.put("phone", player.phone_num);
				obj.put("word", player.word);
				
				arr.put(obj);		//��ӵ�������
			}
			catch(JSONException e)
			{
				Log.d(TAG, "Exception : " + e.toString());
			}
		}
		
		/*���ú�������������*/
		String word_list = arr.toString();
		HashMap<String, String> ret_list = new HashMap<String, String>();
		Log.d(TAG, "word_list :" + word_list);
		ret = Word.add_list(user.curr_session, word_list, ret_list);
		if(Err.NO_ERR == ret)
		{
			/*�����ӷ���˷��ص���Ϣ���жϸ����ֻ����Ƿ�ɹ���Ӵʻ�*/
			for(Player player : this.player_list)
			{
				if(player.valid())		//������Ч���
				{
					//����HashMap�ж�Ӧ�ֻ��ŵ�retֵ��
					//����success����ʲô�����������򷢳�����
					String ret_get = ret_list.get(player.phone_num);
					if(ret_get != null && ret_get.equals(Web.CODE_NO_ERR))
					{
						Log.d(TAG, "Add phone : " + player.phone_num + " 's word success!");
					}
					else
					{
						SMS.send(player.phone_num, player.word);
					}
				}
			}
		}
		
		return ret;
	}

	/* =================�������================ */
	/**
	 * ������б��浽XML�ļ���
	 * @return ���ɹ�������true�����򷵻�false��
	 * */
	public boolean data_savePlayerList() {
		try {
			/*=================����XML�ļ�====================*/
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();

			/* �������Ŀ�� */
			serializer.setOutput(writer);

			/* <?xml version=��1.0�� encoding=��UTF-8�� standalone=��yes��?> */
			serializer.startDocument("UTF-8", true);

			/* ��ӡ���ڵ�: <player_list> */
			serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_ROOT);
			/*
			 * ���δ�ӡ���������Ϣ <player> <name>Alex</name>
			 * <phone_num>12345678901</phone_num> <role>1</role>
			 * <status>1</status> </player>
			 */
			for (Player element : player_list) {
				/* <player> */
				serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_PLAYER);
				/* <name>Alex</name> */
				serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_NAME);
				serializer.text(element.name);
				serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_NAME);
				/* <phone_num>12345678901</phone_num> */
				serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_PHONE_NUM);
				serializer.text(element.phone_num);
				serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_PHONE_NUM);
				/* <role>1</role> */
				serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_ROLE);
				serializer.text(String.valueOf(element.role));
				serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_ROLE);
				/* <status>1</status> */
				serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_STATUS);
				serializer.text(String.valueOf(element.status));
				serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_STATUS);
				/* </player> */
				serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_PLAYER);
			}
			/* </player_list> */
			serializer.endTag(XML_TAG_NAMESPACE, XML_TAG_ROOT);

			/* �ر��ĵ� */
			serializer.endDocument();

			/*=================д���ļ���====================*/
			OutputStream os = context_wrapper.openFileOutput(XML_FILE_USER_LIST, Context.MODE_PRIVATE);
			OutputStreamWriter osw = new OutputStreamWriter(os);
			osw.write(writer.toString());
			osw.close();
			os.close();
			
		} catch (FileNotFoundException e) {
			Log.d(TAG, "FileNotFoundException : " + e);
			return false;
		} catch (IOException e) {
			Log.d(TAG, "IOException : " + e);
			return false;
		} catch (Exception e) {
			Log.d(TAG, "Exception : " + e);
			return false;
		}

		return true;
	}

	/**
	 * ��XML�ļ��ж�ȡ����б�
	 * 
	 * @return ���ɹ�������true����ʧ�ܣ�����false��
	 * */
	public boolean data_readPlayerList() {
		boolean ret = false; // Ĭ��ʧ��

		try {
			/* ����XML��ȡʵ�� */
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader reader = sp.getXMLReader();

			/* ������ȡ��� */
			PlayerListHandle handle = new PlayerListHandle();
			reader.setContentHandler(handle);

			/* ��ʼ��ȡ */
			FileInputStream in = context_wrapper.openFileInput(XML_FILE_USER_LIST);
			InputSource is = new InputSource(in);
			reader.parse(is);

			/* �����ȡ��ɵ�����б� */
			player_list = handle.getPlayerList();
			word_set();		//������Ҵʻ�
			ret = true; 	// ��ɶ�ȡ

		} catch (FileNotFoundException e) {
			Log.d(TAG, "File not found!");
			e.printStackTrace();
		} catch (IOException e) {
			Log.d(TAG, "IOException!");
			e.printStackTrace();
		} catch (SAXException e) {
			Log.d(TAG, "SAXException");
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			Log.d(TAG, "ParserConfigurationException");
			e.printStackTrace();
		}

		if(ret)
		{
			Log.d(TAG, "Success read player_list from XML file!");
		}
		
		return ret;
	}
	
	/**
	 * ������Ϸ��Ϣ
	 * @return ���ɹ�������true�����򣬷���false��
	 * */
	public boolean data_saveGamePref() {
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_CIVIL_WORD, this.civil_word).commit();
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_TRICK_WORD, this.trick_word).commit();
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putInt(PREF_TAG_SPY_NUM, this.spy_num).commit();
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putInt(PREF_TAG_TRICK_NUM, this.trick_num).commit();
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putInt(PREF_TAG_ROUND, this.round).commit();
		context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putInt(PREF_TAG_STEP, this.step).commit();
		
		return true;
	}
	
	/**
	 * ��ȡ��Ϸ��Ϣ
	 * @return ���ɹ�������true�����򣬷���false��
	 * */
	public boolean data_readGamePref() {
		this.civil_word = context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_CIVIL_WORD, "");
		this.trick_word = context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_TRICK_WORD, "");
		this.spy_num 	= context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getInt(PREF_TAG_SPY_NUM, 0);
		this.trick_num  = context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getInt(PREF_TAG_TRICK_NUM, 0);
		this.round		= context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getInt(PREF_TAG_ROUND, 1);
		this.step		= context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getInt(PREF_TAG_STEP, GAME_STEP_SET_PLAYER);
		
		return true;
	}
	
	/**
	 * ����ʻ��б��ļ���
	 * */
	public int data_saveWordList(ArrayList<OneWord> word_list) {
		int ret = Err.NO_ERR;
		
		try {
			/*������ת�����ַ���*/
			String str = Word.array_to_string(word_list);
			
			/*�洢���ļ�*/
			OutputStream os = context_wrapper.openFileOutput(FILE_WORD_LIST, Context.MODE_PRIVATE);
			OutputStreamWriter ow = new OutputStreamWriter(os);
			ow.write(str);
			ow.close();
			os.close();
		}
		catch(JSONException e)
		{
			Log.d(TAG, "Json exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		catch(IOException e)
		{
			Log.d(TAG, "IO exception : " + e.toString());
			ret = Err.ERR_IO;
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * ���ļ��ж�ȡ�ʻ��б�
	 * @throws Exception 
	 * */
	public ArrayList<OneWord> data_readWordList() throws Exception {
		ArrayList<OneWord> word_list = new ArrayList<OneWord>();
		
		try{
			/*��ȡ�ļ�*/
			InputStream fin = context_wrapper.openFileInput(FILE_WORD_LIST);
			int len = fin.available();
			byte[] buff = new byte[len];
			fin.read(buff);
			String str = EncodingUtils.getString(buff, "UTF-8");
			fin.close();
			
			/*���ַ���ת��Ϊ����*/
			word_list = Word.string_to_array(str);
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			throw e;
		}
		
		return word_list;
	}
	
	/**
	 * ɾ�������ļ��е����дʻ�
	 * */
	public void data_delWordList() {
		try {
			/*������ת�����ַ���*/
			JSONArray arr = new JSONArray();;
			String str = arr.toString();
			
			/*�洢���ļ�*/
			OutputStream os = context_wrapper.openFileOutput(FILE_WORD_LIST, Context.MODE_PRIVATE);
			OutputStreamWriter ow = new OutputStreamWriter(os);
			ow.write(str);
			ow.close();
			os.close();
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
		}
	}
	
	/* =================ϵͳ���================ */
	public boolean sys_need_update() {
		boolean ret = false;
		PackageManager pm = context_wrapper.getPackageManager();
		PackageInfo pi;
		int versionCode;
		
		try {
			/*��ȡ��ǰӦ�õ�versionCode*/
			pi = pm.getPackageInfo(context_wrapper.getPackageName(), 0);
			versionCode = pi.versionCode;
			/*��������ϵ�versionCode���Աȣ����ȽϾ�����Ҫ����*/
			if(user.remote_versionCode > versionCode)
			{
				ret = true;
			}
		}
		catch (NameNotFoundException e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			e.printStackTrace();
		}
		
		return ret;
	}
}
