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
 * 玩家类，用于保存一个玩家的相关信息
 * */
class Player {
	/*--------------------------
	 * 常量
	 *--------------------------*/
	//private static final String TAG = "whoispy.Player";
	/* 角色定义 */
	static final int ROLE_NONE = 0; // 无角色，一开始都是这个身份
	static final int ROLE_SHIELD = 1; // 暂不参加游戏
	static final int ROLE_CIVIL = 2; // 平民
	static final int ROLE_SPY = 3; // 卧底
	static final int ROLE_TRICK = 4; // 打酱油的
	/* 状态定义 */
	static final int STATUS_NORMAL = 0; // 普通状态
	static final int STATUS_OUT = 1; // 玩家出局

	/*--------------------------
	 * 静态成员
	 *--------------------------*/
	static private int current_id = 1; // 当前可用id，赋给新添加的玩家，然后自动递增

	/*--------------------------
	 * 普通成员
	 *--------------------------*/
	int id; 			// 用于唯一区别一个玩家
	String name; 		// 姓名
	String phone_num; 	// 手机号码
	int role; 			// 角色
	int status; 		// 玩家状态，正在游戏或者已经出局，用于标记
	String word;		//发给这个玩家的词汇

	/*--------------------------
	 * 构造方法
	 *------------------------*/
	/**
	 * 带参数的构造方法
	 * 
	 * @param name
	 *            - 玩家名
	 * @param phone_num
	 *            - 玩家手机号
	 * */
	public Player(String name, String phone_num) {
		// 初始化玩家
		this.name = name;
		this.phone_num = phone_num;
		this.role = ROLE_NONE;
		this.status = STATUS_NORMAL;
		this.word = "";
		this.id = current_id;
		current_id++;

	}

	/**
	 * 不带参数构造方法,使用默认玩家名及玩家手机号
	 * */
	public Player() {
		// 调用带参数的构造函数
		this("Someone", "0");
	}
	
	/**
	 * 判断某个玩家角色是否有效（不参与游戏的是无效玩家）
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
 * 短信类，主要用于发送短信
 * */
class SMS {
	/*--------------------------
	 * 属性
	 *-------------------------*/
	private final static String TAG = "whoispy.SMS";
	public static final String ACTION_SMS_SENT = "com.example.android.apis.os.SMS_SENT_ACTION";

	/*--------------------------
	 * 构造方法
	 *-------------------------*/

	/*--------------------------
	 * 公有方法
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
 * 用户类，主要处理用户登录的相关信息及操作
 * */
class User {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	private final static String TAG = "whoispy.User";
	/*游戏保存*/
	final static String PREF_FILE_NAME		= "userPrefs";	//保存文件名
	final static String PREF_TAG_PHONE 		= "phone";
	final static String PREF_TAG_PASSWD 	= "passwd";
	
	/*--------------------------
	 * 属性
	 *-------------------------*/
	String curr_session;			//当前session id
	String curr_nickname;			//当前用户昵称
	int	   remote_rid;				//服务器上推荐词汇的最新rid编号，放在这有点奇怪哈...
	int    remote_versionCode;		//当前应用的最新版本号，放到这不太合理...
	
	private ContextWrapper context_wrapper;		//Application Context
	
	/*--------------------------
	 * 方法
	 *-------------------------*/
	/**
	 * 构造方法
	 * */
	User(ContextWrapper context) {
		curr_session = "";
		curr_nickname = "";
		remote_rid = 0;
		
		this.context_wrapper = context;
	}
	
	/**
	 * 注册新用户
	 * @param  phone 手机号；
	 * @param  nickname 昵称；
	 * @param  passwd 密码；
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public int register(String phone, String nickname, String passwd) {
		int ret = Err.NO_ERR;
		
		try {
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone", phone));
			params.add(new BasicNameValuePair("nickname", nickname));
			params.add(new BasicNameValuePair("passwd", passwd));
			params.add(new BasicNameValuePair("mode", "register"));
			
			/*发出请求*/
			Respon respon = new Respon();
			int flag = Web.req_respon(Web.WEB_URL_USERS, params, respon, 3);
			
			/*根据函数执行情况进行不同处理*/
			if(Err.NO_ERR == flag)
			{
				JSONObject json = new JSONObject(respon.str);		//将字符串解析成JSON格式
				String web_ret = json.getString(Web.FLAG_RET);		//读取成功代码
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
	 * 登录
	 * @param  phone 手机号;
	 * @param  passwd 密码;
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public int login(String phone, String passwd) {
		int ret = Err.NO_ERR;		//返回值
	
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone", phone));
			params.add(new BasicNameValuePair("passwd", passwd));
			params.add(new BasicNameValuePair("mode", "login"));
			
			/*发出请求*/
			Respon respon = new Respon();
			int flag = Web.req_respon(Web.WEB_URL_USERS, params, respon, 3);
			
			if(Err.NO_ERR == flag)
			{
				JSONObject res = new JSONObject(respon.str);
				String web_ret = res.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))
				{
					//保存用户信息
					saveUser(phone, passwd);
					//记录session_id
					this.curr_session = res.getString(Web.FLAG_SESSION_ID);
					//记录昵称
					this.curr_nickname = res.getString(Web.FLAG_NICKNAME);
					//记录remote_rid
					this.remote_rid = res.getInt(Web.FLAG_REMOTE_RID);
					//记录version_code
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
	 * 登出
	 * @return
	 */
	public boolean logout() {
		/*TODO 访问远程网页，进行登出*/
		
		/*清空本地用户信息*/
		this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PHONE, "").commit();
		this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).edit().putString(PREF_TAG_PASSWD, "").commit();

		/*清除session信息*/
		this.curr_session = "";
		
		return true;
	}
	
	/**
	 * 保存用户信息
	 * */
	public boolean saveUser(String phone, String passwd) {
		try{
			EncrypAES aes = new EncrypAES();		//解密对象
			
			/*加密*/
			String encryp_phone = aes.Encrytor(phone);
			String encryp_passwd = aes.Encrytor(passwd);
			
			/*保存*/
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
	 * 读取本地用户信息并尝试登录
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public int resumeUser() {
		int ret = Err.ERR_COMMON;		//返回值
		
		try{
			/*读取保存文件*/
			String saved_phone = this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_PHONE, "");
			String saved_passwd = this.context_wrapper.getSharedPreferences(PREF_FILE_NAME, Activity.MODE_PRIVATE).getString(PREF_TAG_PASSWD, "");
			
			/*若手机号或密码不存在，则认为错，否则返回成功*/
			if(saved_phone.equals("") || saved_passwd.equals(""))
			{
				return Err.ERR_COMMON;
			}
			
			/*解密手机号及密码*/
			EncrypAES aes = new EncrypAES();		//解密对象
			
			String decryp_phone = aes.Decryptor(saved_phone);
			String decryp_passwd = aes.Decryptor(saved_passwd);
			
			/*尝试登陆*/
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
	 * 读取发给当前用户的所有词汇
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
	 * 删除多条词汇
	 * */
	public int del_word(ArrayList<OneWord> word_list){
		/*构造JSON字符串*/
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
		
		/*访问网页，删除词汇*/
		String wid_list = arr.toString();
		HashMap<String, String> ret_list = new HashMap<String, String>();
		//Log.d(TAG, "wid_list : " + wid_list);
		int ret = Word.del_list(curr_session, wid_list, ret_list);
		
		return ret;
	}
}

/**
 * 添加词汇是否成功的结果保存
 * */
class RetWordAdd {
	/*--------------------------
	 * 属性
	 *-------------------------*/
	String phone;
	String ret;
	String err_code;
	
	/*--------------------------
	 * 方法
	 *-------------------------*/
	/**
	 * 构造方法
	 * */
	public RetWordAdd(String phone, String ret, String err_code) {
		this.phone = phone;
		this.ret = ret;
		this.err_code = err_code;
	}
}

/**
 * 一个词汇，其中包括：wid、发词人、词汇内容、发词时间等信息
 * */
class OneWord {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	
	/*--------------------------
	 * 属性
	 *-------------------------*/
	String wid;		//在数据库中的记录
	String sender;	//发词人
	String word;	//词汇内容
	String time;	//发词时间
	
	/*--------------------------
	 * 方法
	 *-------------------------*/
	/**
	 * 构造方法
	 * */
	OneWord(String wid, String sender, String word, String time) {
		this.wid = wid;
		this.sender = sender;
		this.word = word;
		this.time = time;
	}
}

/**
 * 词汇类，主要提供词汇的添加、查询、删除等操作（WEB上的操作）
 * */
class Word {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	private final static String TAG = "whoispy.User";
	/*TAGs*/
	private final static String TAG_WID = "wid";
	private final static String TAG_WORD = "word";
	private final static String TAG_SENDER = "sender";
	private final static String TAG_TIME = "time";
	private final static String TAG_IS_DEL = "is_del";
	

	/*--------------------------
	 * 属性
	 *-------------------------*/
	
	/*--------------------------
	 * 方法
	 *-------------------------*/
	/**
	 * 添加词汇
	 * @param phone_to - 要发给的手机号
	 * @param word - 要发的词汇
	 * @param session_id - 当前登录用户的session_id
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public static int add(String session_id, String phone_to, String word){
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("phone_to", phone_to));
			params.add(new BasicNameValuePair("word", word));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "add"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//添加成功
				{
					ret = Err.NO_ERR;
				}
				else									//添加失败
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
	 * 添加多个词汇
	 * @param session_id - 当前登录用户的session_id
	 * @param word_list - 要发出的词汇列表，json格式封装
	 * @param ret_list - 输出参数，是一个HashMap，其中Key是手机号，Value是success或error
	 * */
	public static int add_list(String session_id, 
			                   String word_list, 
			                   HashMap<String, String> ret_list){
		
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("word_list", word_list));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "add_list"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//添加成功
				{
					//读取添加结果数组
					JSONArray arr = json.getJSONArray("ret_word_add");
					//依次读取各条数据
					for(int i = 0; i < arr.length(); i++)
					{
						JSONObject one_ret = arr.getJSONObject(i);
						
						//读取各个信息
						String phone = one_ret.getString("phone");
						String ret_t = one_ret.getString("ret");
						
						//添加到返回列表中
						ret_list.put(phone, ret_t);
					}
				}
				else									//添加失败
				{
					String err_code = json.getString(Web.FLAG_ERR_CODE);
					if(err_code.equals(Web.CODE_ERR_NOT_LOGIN))		//若没有登录
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
	 * 查询发给当前用户的所有词汇
	 * @param session_id - 当前登录用户的session_id
	 * @param word_list - 返回参数，用于保存查询到的词汇列表；
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public static int search(String session_id, ArrayList<OneWord> word_list){
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "search"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//查询成功
				{
					JSONArray json_word_list = json.getJSONArray("word_list");
					for(int i = 0; i < json_word_list.length(); i++)
					{
						//从json字符串中解析各个字段
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
						
						//创建新的OneWord对象
						OneWord one_word = new OneWord(wid, sender, word, time);
						
						//将对象加到队列中
						word_list.add(one_word);
					}
				}
				else									//查询失败
				{
					String err_code = json.getString(Web.FLAG_ERR_CODE);
					if(err_code.equals(Web.CODE_ERR_NOT_LOGIN))		//未登录
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
	 * 删除数据库中的某条词汇
	 * */
	public static int del(String session_id, String wid){
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("wid", wid));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "del"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//删除成功
				{
					ret = Err.NO_ERR;
				}
				else									//删除失败
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
	 * 删除多个词汇
	 * @param session_id - 当前登录用户的session_id
	 * @param wid_list - 要删除的词汇列表，json格式封装
	 * @param ret_list - 输出参数，是一个HashMap，其中Key是手机号，Value是success或error
	 * */
	public static int del_list(String session_id,
							   String wid_list, 
			                   HashMap<String, String> ret_list){
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("wid_list", wid_list));
			params.add(new BasicNameValuePair("session_id", session_id));
			params.add(new BasicNameValuePair("mode", "del_list"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//删除成功
				{
					//读取添加结果数组
					JSONArray arr = json.getJSONArray("ret_word_del");
					//依次读取各条数据
					for(int i = 0; i < arr.length(); i++)
					{
						JSONObject one_ret = arr.getJSONObject(i);
						
						//读取各个信息
						String wid = one_ret.getString("wid");
						String ret_t = one_ret.getString("ret");
						
						//添加到返回列表中
						ret_list.put(wid, ret_t);
					}
				}
				else									//删除失败
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
	 * 将词汇列表转换成json字符串
	 * @param word_list - 要保存的词汇列表
	 * @return
	 * */
	public static String array_to_string(ArrayList<OneWord> word_list) throws JSONException{

		/*创建JSON对象*/
		JSONArray arr = new JSONArray();
		
		/*依次读取各个词汇，添加到数组中*/
		for(OneWord element : word_list)
		{
			JSONObject obj = new JSONObject();
			obj.put(TAG_WID, element.wid);
			obj.put(TAG_WORD, element.word);
			obj.put(TAG_SENDER, element.sender);
			obj.put(TAG_TIME, element.time);
			
			arr.put(obj);
		}
		
		/*转换成字符串*/
		String str = arr.toString();
		//Log.d(TAG, "Json str : " + str);
		
		return str;
	}
	
	/**
	 * 从json字符串中恢复出词汇列表
	 * @param str - 要恢复的json字符串
	 * */
	public static ArrayList<OneWord> string_to_array(String str) throws JSONException{
		
		ArrayList<OneWord> word_list = new ArrayList<OneWord>();
		JSONArray arr = new JSONArray(str);		//从字符串创建json对象
	
		/*依次读取各个词汇的内容*/
		for(int i = 0; i < arr.length(); i++)
		{
			JSONObject obj = arr.getJSONObject(i);
			String wid = obj.getString(TAG_WID);
			String word = obj.getString(TAG_WORD);
			String sender = obj.getString(TAG_SENDER);
			String time = obj.getString(TAG_TIME);
			
			/*添加词汇到列表中*/
			OneWord one_word = new OneWord(wid, sender, word, time);
			word_list.add(one_word);
		}
		
		return word_list;
	}
}

/**
 * 推荐词汇类，与推荐词汇相关的数据以及操作方法都在此类中
 * */
class RecommendWord {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	private final String TAG = "whoispy.RecommendWord";
	/*网页返回字段*/
	private String TAG_CIVIL_WORD = "c";
	private String TAG_TRICK_WORD = "t";
	private String TAG_CLIENT_RID = "client_rid";
	private String TAG_WORD_LIST = "word_list";
	/*保存文件名*/
	private String FILE_NAME_RECOMMEND_WORD = "recommend_word.json";
	
	/*--------------------------
	 * 内部类
	 *-------------------------*/
	/**
	 * 词汇组
	 * */
	class WordPair {
		String civil_word;		//好人词汇
		String trick_word;		//傻子词汇
		
		public WordPair(String civil_word, String trick_word)
		{
			this.civil_word = civil_word;
			this.trick_word = trick_word;
		}
	}
	
	/*--------------------------
	 * 属性
	 *-------------------------*/
	private ContextWrapper context_wrapper;		//应用程序上下文，用于读写本地文件等
	private int current_index = 0;				//记录读取到哪个单词，不适用随机读取，而是使用顺序读取方式
	private int client_rid = 0;					//当前最新的词汇组rid
	private int remote_rid = 0; 				//服务器上最新的词汇组rid
	private String session_id; 					//当前会话session_id；
	private ArrayList<WordPair> word_pair_list = new ArrayList<WordPair>(); 		//内置的词汇组列表

	/*--------------------------
	 * 方法
	 *-------------------------*/
	public RecommendWord(ContextWrapper context_wrapper) {
		this.context_wrapper = context_wrapper;
	}
	
	/**
	 * 是否需要读取远程词汇
	 * @return true - 需要； false - 不需要；
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
	 * 读取服务器上的词组
	 * @return 错误代码；
	 * */
	public int get_remote_word()
	{
		int ret = Err.NO_ERR;
		
		try{
			/*设置参数*/
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("session_id", this.session_id));
			params.add(new BasicNameValuePair("client_rid", String.valueOf(this.client_rid)));
			params.add(new BasicNameValuePair("mode", "recommend_search"));
			
			/*开始连接*/
			Respon respon = new Respon();
			ret = Web.req_respon(Web.WEB_URL_WORDS, params, respon, 3);
			
			/*根据返回结果做处理*/
			if(Err.NO_ERR == ret)
			{
				//创建JSON对象
				JSONObject json = new JSONObject(respon.str);
				
				//读取成功标志
				String web_ret = json.getString(Web.FLAG_RET);
				if(web_ret.equals(Web.CODE_NO_ERR))		//查询成功
				{
					//更新本地rid以及远程rid的值
					int rid = json.getInt(Web.FLAG_REMOTE_RID);
					this.remote_rid = rid;
					this.client_rid = rid;
					
					//保存词汇列表
					JSONArray json_word_list = json.getJSONArray(Web.FLAG_WORD_LIST);
					for(int i = 0; i < json_word_list.length(); i++)
					{
						//从json字符串中解析各个字段
						JSONObject json_word = json_word_list.getJSONObject(i);
						String civil_word = json_word.getString(TAG_CIVIL_WORD);
						String trick_word = json_word.getString(TAG_TRICK_WORD);
						
						Log.d(TAG, "----------------------");
						Log.d(TAG, "civil_word : " + civil_word);
						Log.d(TAG, "trick_word : " + trick_word);
						
						//创建新的OneWord对象
						WordPair one_word = new WordPair(civil_word, trick_word);
						
						//将对象加到队列中
						this.word_pair_list.add(one_word);
					}
				}
				else									//查询失败
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
	 * 设置remote_rid、session_id
	 * */
	public void set_server_data(int remote_rid, String session_id)
	{
		this.remote_rid = remote_rid;
		this.session_id = session_id;
	}
	
	/**
	 * 读取一组词汇
	 * @param word - 输出参数，保存查询到的词汇；
	 * @return 错误代码；
	 * */
	public int read_word(WordPair word) {
		int ret = Err.NO_ERR;
		
		/*先给词汇赋默认值*/
		word.civil_word = "";
		word.trick_word = "";
		
		/*挑选词汇*/
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
	 * 从文件中读取推荐词组的列表
	 * */
	public void read_words_from_file()
	{
		try {
			/*读取文件*/
			InputStream fin = context_wrapper.openFileInput(FILE_NAME_RECOMMEND_WORD);
			int len = fin.available();
			byte buff[] = new byte[len];
			fin.read(buff);
			String str = EncodingUtils.getString(buff, "utf-8");
			fin.close();
			
			/*创建json对象*/
			JSONObject json = new JSONObject(str);
			
			/*解析client_rid*/
			this.client_rid = json.getInt(TAG_CLIENT_RID);
			Log.d(TAG, "client_rid : " + this.client_rid);
			
			/*解析词汇列表*/
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
	 * 把推荐词组列表存储到文件中
	 * */
	public void save_words_to_file()
	{
		try{
			/*将rid及词汇列表转换成json格式*/
			JSONObject json = new JSONObject();
			//client_rid
			json.put(TAG_CLIENT_RID, this.client_rid);
			//词汇列表
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
			
			/*保存成文件*/
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
 * 游戏类，提供游戏相关信息存储以及游戏进程控制的函数
 * */
public class Game {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	private final static String TAG = "whoispy.Game";

	/* 错误代码 */
	final static int NO_ERR = 0; // 无错误，成功
	final static int ERR_SET = -1; // 设置错误
	final static int ERR_GET = -2; // 查询错误
	final static int ERR_VAL = -3; // 错误的参数
	final static int ERR_PHONE_NUM_EXIST = -4; // 号码已存在
	final static int ERR_PLAYER_NOT_FIND = -5; // 未找到对应玩家

	/* 常用常量 */
	final static int MIN_PLAYER_NUM = 2; // 最小玩家个数

	/*游戏阶段*/
	final static int GAME_STEP_SET_PLAYER 	= 0;		//设置玩家
	final static int GAME_STEP_SET_ROLE_NUM = 1;		//设置角色个数
	final static int GAME_STEP_SET_WORD		= 2;		//设置单词
	final static int GAME_STEP_GAME_VIEW	= 3;		//游戏界面
	
	/*游戏保存*/
	final static String PREF_FILE_NAME		= "gamePrefs";	//保存文件名
	final static String PREF_TAG_CIVIL_WORD = "civil_word";
	final static String PREF_TAG_TRICK_WORD = "trick_word";
	final static String PREF_TAG_SPY_NUM	= "spy_num";
	final static String PREF_TAG_TRICK_NUM  = "trick_num";
	final static String PREF_TAG_ROUND      = "round";
	final static String PREF_TAG_STEP       = "step";
	
	/* 玩家列表保存 */
	// 存储文件
	private static final String XML_FILE_USER_LIST = "user_list.xml";
	// TAG
	private static final String XML_TAG_NAMESPACE = ""; // 命名空间
	private static final String XML_TAG_ROOT = "player_list"; // 根节点
	private static final String XML_TAG_PLAYER = "player";
	private static final String XML_TAG_NAME = "name";
	private static final String XML_TAG_PHONE_NUM = "phone_num";
	private static final String XML_TAG_ROLE = "role";
	private static final String XML_TAG_STATUS = "status";
	
	/*词汇列表保存*/
	private static final String FILE_WORD_LIST = "word_list.json";		//保存文件名
	
	/*--------------------------
	 * 内部类
	 *-------------------------*/
	/**
	 * 玩家列表XML文件的处理类
	 * */
	class PlayerListHandle extends DefaultHandler {
		private static final String TAG = "whoispy.Game.UserHandle";

		private ArrayList<Player> player_list; // 临时存放的玩家列表
		private String current_tag = ""; // 当前正在访问的TAG
		private Player player_new = null; // 临时的玩家对象

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

			// 保存 TAG NAME
			this.current_tag = localName;

			// 若是player TAG,则创建新玩家
			if (localName.equals(XML_TAG_PLAYER)) {
				this.player_new = new Player();
			}

			super.startElement(uri, localName, qName, attributes);
		}

		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException {

			/* 添加玩家信息 */
			String str = new String(ch, start, length); // 读取标签里的内容
			//Log.d(TAG, str);
			if (this.current_tag != "") {
				if (this.current_tag.equals(XML_TAG_NAME)) // name
				{
					// 保存名字
					this.player_new.name = str;
				} else if (this.current_tag.equals(XML_TAG_PHONE_NUM)) // phone_num
				{
					// 保存电话号码
					this.player_new.phone_num = str;
				} else if (this.current_tag.equals(XML_TAG_ROLE)) // role
				{
					// 保存角色
					this.player_new.role = Integer.parseInt(str);
				} else if (this.current_tag.equals(XML_TAG_STATUS)) // status
				{
					// 保存状态
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

			/* 遇到结束标签，添加一个玩家 */
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
		 * 对外函数
		 * */
		/**
		 * 返回构造好的player_list,此函数应该在完成xml解析后调用
		 * */
		public ArrayList<Player> getPlayerList() {
			return player_list;
		}
	}

	/*--------------------------
	 * 属性
	 *-------------------------*/
	/* 用户信息 */
	User user;									//当前用户
	/* 推荐词汇信息 */
	RecommendWord recommend_word;
	/* 用户是否想升级，在一次应用启动中，只会显示一次提示升级信息 */
	boolean want_update = true;					//默认想升级，点击一次后就不想升级了
	
	/* 游戏信息 */
	private ArrayList<Player> player_list;		//玩家列表
	private String civil_word;					//平民词汇
	private String trick_word;					//打酱油的词汇
	private int spy_num;						//卧底数目
	private int trick_num;						//打酱油数目
	private int round;							//回合
	private int step;							//游戏步骤
	
	/* 其他 */
	private int cursor_player; 					// 用于玩家读取的游标
	private ContextWrapper context_wrapper;		//Application Context

	/*--------------------------
	 * 构造方法
	 *-------------------------*/
	public Game(ContextWrapper wrapper) {
		/*保存Application上下文*/
		this.context_wrapper = wrapper;
		
		/*重置用户信息*/
		this.user = new User(wrapper);
		
		/*重置推荐词汇*/
		this.recommend_word = new RecommendWord(wrapper);
		
		/*重置游戏信息*/
		game_info_reset();
	}

	/*--------------------------
	 * 私有方法
	 *-------------------------*/
	/**
	 * 分配角色
	 * */
	private void role_allocate() {
		int player_index = 0;

		// 把所有有效玩家的id存储在临时数组里
		ArrayList<Integer> player_id_list = new ArrayList<Integer>();
		for (Player element : player_list) {
			if (element.role != Player.ROLE_SHIELD) // 若玩家参加游戏
			{
				player_id_list.add(element.id);
			}
		}

		// 设置随机数种子
		Random r = new Random();

		// 挑选卧底
		for (int i = 0; i < this.spy_num; i++) {
			// 随机选择玩家
			player_index = r.nextInt(player_id_list.size()); // 通过取余方法限制范围
			Log.d(TAG, "spy " + i + ", player_index : " + player_index
					+ ", player_num : " + player_id_list.size());
			// 设置玩家身份
			player_setRole(player_id_list.get(player_index), Player.ROLE_SPY);
			// 将选中的玩家从队列删除
			player_id_list.remove(player_index);
		}

		// 挑选打酱油的
		for (int i = 0; i < this.trick_num; i++) {
			// 随机选择玩家
			player_index = r.nextInt(player_id_list.size()); // 通过取余方法限制范围
			Log.d(TAG, "trick " + i + ", player_index : " + player_index
					+ ", player_num : " + player_id_list.size());
			// 设置玩家身份
			player_setRole(player_id_list.get(player_index), Player.ROLE_TRICK);
			// 将选中的玩家从队列删除
			player_id_list.remove(player_index);
		}

		// 剩下的是平民
		for (int i = 0; i < player_id_list.size(); i++) {
			// 设置玩家身份
			player_setRole(player_id_list.get(i), Player.ROLE_CIVIL);
		}

		return;
	}

	/**
	 * 根据玩家身份将其对应的词汇设置好
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
	 * 重置游戏信息
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
	 * 公有方法
	 *-------------------------*/
	/* =================用户相关================ */
	/**
	 * 用户登录
	 * */
	public int user_login(String phone, String passwd){
		int ret = user.login(phone, passwd);		//进行登录
		recommend_word.set_server_data(user.remote_rid, user.curr_session);		//设置推荐词汇相关数据
		
		return ret;
	}
	
	/**
	 * 用户恢复登录
	 * */
	public int user_resume(){
		int ret = user.resumeUser();		//进行登录
		recommend_word.set_server_data(user.remote_rid, user.curr_session);		//设置推荐词汇相关数据
		
		return ret;
	}
	
	/* =================玩家相关================ */
	/**
	 * 添加新玩家
	 * 
	 * @param name
	 *            - 玩家名字
	 * @param phone_num
	 *            - 玩家手机号码
	 * 
	 * @return 若成功，返回添加玩家的id号，> 0 若失败，返回小于0的错误代码
	 * */
	public int player_add(String name, String phone_num) {
		for (Player element : player_list) {
			if (element.phone_num.equals(phone_num)) // 若电话号码已经存在,则返回错误
			{
				return ERR_PHONE_NUM_EXIST;
			}
		}

		// 创建新玩家，添加到玩家列表中，返回其id
		Player player = new Player(name, phone_num);
		player_list.add(player);

		return player.id;
	}

	/**
	 * 检查某个号码是否存在
	 * */
	public int player_check_exist(String phone_num) {
		for (Player element : player_list) {
			if (element.phone_num.equals(phone_num)) // 若电话号码已经存在,则返回错误
			{
				return ERR_PHONE_NUM_EXIST;
			}
		}

		return NO_ERR;
	}

	/**
	 * 删除一个玩家
	 * 
	 * @param id
	 *            - 要删除玩家的id号
	 * 
	 * @return 若成功返回NO_ERR，若找不到玩家返回ERR_PLAYER_NOT_FIND；
	 * */
	public int player_remove(int id) {
		for (Player element : player_list) {
			if (id == element.id) {
				player_list.remove(element); // 删除数据
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * 设置一个玩家的角色
	 * 
	 * @param id
	 *            - 要设置的玩家
	 * @param role
	 *            - 要设置的角色
	 * 
	 * @return 若成功返回NO_ERR，若找不到玩家返回ERR_PLAYER_NOT_FIND；
	 * */
	public int player_setRole(int id, int role) {
		for (Player element : player_list) {
			if (id == element.id) // 设置新Host
			{
				element.role = role;
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * 设置一个玩家的状态
	 * 
	 * @param id
	 *            - 要设置的玩家
	 * @param status
	 *            - 要设置的状态
	 * 
	 * @return 若成功返回NO_ERR，若找不到玩家返回ERR_PLAYER_NOT_FIND；
	 * */
	public int player_setStatus(int id, int status) {
		for (Player element : player_list) {
			if (id == element.id) // 设置新Host
			{
				element.status = status;
				return NO_ERR;
			}
		}

		return ERR_PLAYER_NOT_FIND;
	}

	/**
	 * 根据玩家id获取玩家对象
	 * 
	 * @param id
	 *            - 玩家id
	 * 
	 * @return 若成功，返回对应的玩家对象；否则，返回null；
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
	 * 得到玩家列表的第一个玩家
	 * 
	 * @return 若成功，返回对应的玩家对象；若失败，则返回null；
	 * */
	public Player player_get_first() {
		if (player_list.size() > 0) // 必须有元素才行
		{
			cursor_player = 0;
			return player_list.get(0);
		} else // 若没有元素则返回null
		{
			return null;
		}
	}

	/**
	 * 读取下一个玩家，用于遍历玩家列表，Game类会自己维护一个当前游标
	 * 
	 * @return 若成功，返回对应的玩家对象；若失败，则返回null；
	 * */
	public Player player_get_next() {
		if (cursor_player < (player_list.size() - 1)) // 不能超出边界
		{
			cursor_player++;
			return player_list.get(cursor_player);
		} else // 若超出边界则返回null
		{
			return null;
		}
	}

	
	/**
	 * 向某个玩家发出短信或数据库记录，告知其词条
	 * 
	 * @param player
	 *            - 要发给的玩家
	 * 
	 * @return NO_ERR : 成功; 其他错误代码 : 有错误，失败；
	 * */
	public int player_send_word(Player player) {
		int ret = Err.NO_ERR;

		//若玩家有效，则发出词汇
		if(player.valid())
		{
			String word = player.word; 					//短信内容
			String phone_num = player.phone_num;		//手机号码
			
			//访问相关网页，发出词汇
			ret = Word.add(user.curr_session, phone_num, word);
			switch(ret)
			{
				case Err.NO_ERR:
				case Err.ERR_NETWORK:			//网络错误不发短信
				case Err.ERR_NETWORK_TIMEOUT:
					break;
					
				case Err.ERR_COMMON:			//用户不在线，则发短信
				default:
					SMS.send(phone_num, word);
					break;
			}
		}

		return ret;
	}
	
	
	/* =================游戏相关================ */
	/**
	 * 返回当前是第几局
	 * */
	public int game_getRound() {
		return round;
	}

	/**
	 * 增加局数
	 * */
	public void game_addRound() {
		round++;
	}
	
	/**
	 * 设置平民的词汇
	 * */
	public void game_setCivilWord(String word) {
		this.civil_word = word;
	}

	/**
	 * 设置打酱油的词汇
	 * */
	public void game_setTrickWord(String word) {
		this.trick_word = word;
	}

	/**
	 * 查询平民的词汇
	 * */
	public String game_getCivilWord() {
		return this.civil_word;
	}

	/**
	 * 查询打酱油的词汇
	 * */
	public String game_getTrickWord() {
		return this.trick_word;
	}

	/**
	 * 查找有效玩家个数
	 * 
	 * @return 有效玩家个数
	 * */
	public int game_getValidPlayerNum() {
		int valid_num = 0;

		for (Player element : player_list) {
			if (element.role != Player.ROLE_SHIELD) // 只要不是不参加游戏的玩家，就是有效玩家
			{
				valid_num++;
			}
		}

		Log.d(TAG, "Valid player num : " + valid_num);

		return valid_num;
	}

	/**
	 * 设置卧底的个数
	 * 
	 * @param spy_num
	 *            - 卧底的个数；
	 * */
	public void game_setSpyNum(int spy_num) {
		this.spy_num = spy_num;
	}

	/**
	 * 设置打酱油的个数
	 * 
	 * @param trick_num
	 *            - 打酱油的个数；
	 * */
	public void game_setTrickNum(int trick_num) {
		this.trick_num = trick_num;
	}

	/**
	 * 读取卧底的个数
	 * 
	 * @return 返回卧底的个数；
	 * */
	public int game_getSpyNum() {
		return this.spy_num;
	}

	/**
	 * 读取打酱油的个数
	 * 
	 * @return 返回打酱油的个数；
	 * */
	public int game_getTrickNum() {
		return this.trick_num;
	}

	/**
	 * 计算各个角色的个数
	 * */
	public void game_calcRoleNum() {
		int valid_num = game_getValidPlayerNum();

		if (valid_num < 2) // 0~1人，囧
		{
			spy_num = 0;
			trick_num = 0;
		} else if (valid_num <= 5) // 0~5人
		{
			spy_num = 1;
			trick_num = 0;
		} else if (valid_num <= 10) // 6~10人
		{
			spy_num = 1;
			trick_num = 1;
		} else if (valid_num <= 15) // 11~15人
		{
			spy_num = 2;
			trick_num = 1;
		} else if (valid_num <= 20) // 16~20人
		{
			spy_num = 3;
			trick_num = 2;
		} else // 20人以上
		{
			spy_num = (valid_num / 8) + 1;
			trick_num = spy_num - 1;
		}
	}

	/**
	 * 设置游戏，主要进行角色分配与词汇生成
	 * */
	public boolean game_setting() {
		/* 测试用，打印所有信息 
		for (Player element : player_list) {
			Log.d(TAG, "id : " + element.id + ", name : " + element.name
					+ ", phone_num : " + element.phone_num + ", role : "
					+ element.role);
		}
		Log.d(TAG, "spy_num : " + this.spy_num);
		Log.d(TAG, "trick_num : " + this.trick_num);
		Log.d(TAG, "civil_word : " + this.civil_word);
		Log.d(TAG, "trick_word : " + this.trick_word);*/

		/* 局数++ */
		round++;
		
		/* 所有玩家状态恢复 */
		for(Player element : player_list)
		{
			element.status = Player.STATUS_NORMAL;
		}
		
		/* 分配角色 */
		role_allocate();

		/* 分配词汇 */
		word_set();
		
		return true;
	}

	/**
	 * 重新开始一局游戏
	 * */
	public void game_replay() {
		/* 重置角色数目以及词汇 */
		civil_word = "";
		trick_word = "";

		/* 重置玩家状态 */
		for (Player element : player_list) {
			element.status = Player.STATUS_NORMAL;
		}

		return;
	}

	/**
	 * 重置游戏信息
	 * */
	public void game_new() {
		game_info_reset();
	}

	/**
	 * 继续游戏
	 * */
	public void game_continue() {
		data_readGamePref();						//读取游戏配置
		
		recommend_word.read_words_from_file();		//读取推荐词汇列表
		
		if(!data_readPlayerList())					//从XML读取玩家列表
		{
			game_info_reset();						//若失败，则复位游戏信息
		}
	}
	
	/**
	 * 设置游戏阶段
	 * */
	public void game_set_step(int step){
		Log.d(TAG, "Set game step : " + step);
		
		this.step = step;
	}

	/**
	 * 读取游戏处于的阶段
	 * */
	public int game_get_step(){
		return this.step;
	}
	
	/**
	 * 向所有玩家发出短信
	 * @return NO_ERR : 成功; 其他错误代码 : 有错误，失败；
	 * */
	public int game_send_word_to_all() {
		int ret = Err.NO_ERR;
		
		/*构造json文本，其中包括各个玩家的手机号及词汇*/
		JSONArray arr = new JSONArray();
		
		/*依次遍历各个玩家*/
		for(Player player : player_list)
		{
			/*判断玩家角色是否有效*/
			if(!player.valid())
			{
				continue;
			}
			
			/*构造json对象*/
			try {
				JSONObject obj = new JSONObject();
				obj.put("phone", player.phone_num);
				obj.put("word", player.word);
				
				arr.put(obj);		//添加到数组中
			}
			catch(JSONException e)
			{
				Log.d(TAG, "Exception : " + e.toString());
			}
		}
		
		/*调用函数，访问网络*/
		String word_list = arr.toString();
		HashMap<String, String> ret_list = new HashMap<String, String>();
		Log.d(TAG, "word_list :" + word_list);
		ret = Word.add_list(user.curr_session, word_list, ret_list);
		if(Err.NO_ERR == ret)
		{
			/*解析从服务端返回的信息，判断各个手机号是否成功添加词汇*/
			for(Player player : this.player_list)
			{
				if(player.valid())		//若是有效玩家
				{
					//查找HashMap中对应手机号的ret值，
					//若是success，则什么都不做，否则发出短信
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

	/* =================数据相关================ */
	/**
	 * 将玩家列表保存到XML文件中
	 * @return 若成功，返回true；否则返回false；
	 * */
	public boolean data_savePlayerList() {
		try {
			/*=================构造XML文件====================*/
			XmlSerializer serializer = Xml.newSerializer();
			StringWriter writer = new StringWriter();

			/* 设置输出目标 */
			serializer.setOutput(writer);

			/* <?xml version=”1.0″ encoding=”UTF-8″ standalone=”yes”?> */
			serializer.startDocument("UTF-8", true);

			/* 打印根节点: <player_list> */
			serializer.startTag(XML_TAG_NAMESPACE, XML_TAG_ROOT);
			/*
			 * 依次打印各个玩家信息 <player> <name>Alex</name>
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

			/* 关闭文档 */
			serializer.endDocument();

			/*=================写到文件中====================*/
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
	 * 从XML文件中读取玩家列表
	 * 
	 * @return 若成功，返回true；若失败，返回false；
	 * */
	public boolean data_readPlayerList() {
		boolean ret = false; // 默认失败

		try {
			/* 创建XML读取实例 */
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader reader = sp.getXMLReader();

			/* 创建读取句柄 */
			PlayerListHandle handle = new PlayerListHandle();
			reader.setContentHandler(handle);

			/* 开始读取 */
			FileInputStream in = context_wrapper.openFileInput(XML_FILE_USER_LIST);
			InputSource is = new InputSource(in);
			reader.parse(is);

			/* 保存读取完成的玩家列表 */
			player_list = handle.getPlayerList();
			word_set();		//设置玩家词汇
			ret = true; 	// 完成读取

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
	 * 保存游戏信息
	 * @return 若成功，返回true；否则，返回false；
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
	 * 读取游戏信息
	 * @return 若成功，返回true；否则，返回false；
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
	 * 保存词汇列表到文件中
	 * */
	public int data_saveWordList(ArrayList<OneWord> word_list) {
		int ret = Err.NO_ERR;
		
		try {
			/*将数组转换成字符串*/
			String str = Word.array_to_string(word_list);
			
			/*存储到文件*/
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
	 * 从文件中读取词汇列表
	 * @throws Exception 
	 * */
	public ArrayList<OneWord> data_readWordList() throws Exception {
		ArrayList<OneWord> word_list = new ArrayList<OneWord>();
		
		try{
			/*读取文件*/
			InputStream fin = context_wrapper.openFileInput(FILE_WORD_LIST);
			int len = fin.available();
			byte[] buff = new byte[len];
			fin.read(buff);
			String str = EncodingUtils.getString(buff, "UTF-8");
			fin.close();
			
			/*将字符串转换为数组*/
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
	 * 删除保存文件中的所有词汇
	 * */
	public void data_delWordList() {
		try {
			/*将数组转换成字符串*/
			JSONArray arr = new JSONArray();;
			String str = arr.toString();
			
			/*存储到文件*/
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
	
	/* =================系统相关================ */
	public boolean sys_need_update() {
		boolean ret = false;
		PackageManager pm = context_wrapper.getPackageManager();
		PackageInfo pi;
		int versionCode;
		
		try {
			/*读取当前应用的versionCode*/
			pi = pm.getPackageInfo(context_wrapper.getPackageName(), 0);
			versionCode = pi.versionCode;
			/*与服务器上的versionCode做对比，若比较旧则需要更新*/
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
