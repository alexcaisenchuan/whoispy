package com.alexcai.whoispy;

import java.net.UnknownHostException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/*--------------------------
 * 类型定义
 * */
class Respon {
	String str;		//响应字符串
	
	/**
	 * 构造函数
	 * */
	Respon(){
		str = "";
	}
}

/**
 * 网页访问通用类，其中包括与网络访问有关的常用方法
 * */
public class Web {
	/*------------------------------
	 * 常量
	 * */
	private final static String TAG = "whoispy.Web";
	/*连接设置*/
	final static int HTTP_CONNECTION_TIMEOUT = 8000;		//连接超时时间
	final static int HTTP_SOCKET_TIMEOUT 	 = 8000;		//Socket超时时间
	/*网页返回的字符串解析相关*/
	//flag
	final static String FLAG_RET = "ret";
	final static String FLAG_ERR_CODE = "err_code";
	final static String FLAG_SESSION_ID = "session_id";
	final static String FLAG_RET_WORD_ADD = "ret_word_add";	//添加词汇是否成功的信息
	final static String FLAG_NICKNAME = "nickname";
	final static String FLAG_WORD_LIST = "word_list";
	final static String FLAG_REMOTE_RID = "remote_rid";
	final static String FLAG_VERSION_CODE = "version_code";
	//err_code
	final static String CODE_NO_ERR = "success";
	final static String CODE_ERROR = "error";
	final static String CODE_ERR_DB = "err_db";
	final static String CODE_ERR_PHONE_EXIST = "err_phone_exist";
	final static String CODE_ERR_PASSWD = "err_passwd";
	final static String CODE_ERR_USER_OFFLINE = "err_user_offline";
	/*网页地址*/
	final static String WEB_URL_WORDS = "http://caisenchuan.web-184.com/whoispy/words.php";		//words.php
	final static String WEB_URL_USERS = "http://caisenchuan.web-184.com/whoispy/users.php";		//users.php
	
	/*--------------------------
	 * 属性
	 * */
	
	/*--------------------------
	 * 方法
	 * */
	/**
	 * 请求某个网页
	 * @param url - 要请求的网页地址
	 * @param params - 可以给网页发参数，采用NameVluePair的模式，不使用的话就置成null
	 * @param respon - 输出参数，存储网页返回的信息
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public static int req_respon(String url, List<NameValuePair> params, Respon respon) {
		int ret = Err.NO_ERR;		//返回值
	
		try{
			/*创建http对象*/
			HttpClient http = new DefaultHttpClient();
			HttpPost req = new HttpPost(url);
			
			/*设置超时时间*/
			HttpParams http_params = http.getParams();
			HttpConnectionParams.setConnectionTimeout(http_params, HTTP_CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(http_params, HTTP_SOCKET_TIMEOUT);
			
			/*设置参数*/
			if(params != null)
			{
				HttpEntity entity = new UrlEncodedFormEntity(params, "utf-8");		//设置编码方式
				req.setEntity(entity);
			}
			
			/*发出POST请求*/
			HttpResponse http_respon = http.execute(req);
			
			/*等待返回结果*/
			int status_code = http_respon.getStatusLine().getStatusCode();
			if(status_code == HttpStatus.SC_OK)
			{
				String str = EntityUtils.toString(http_respon.getEntity());
				Log.d(TAG, "Response str : " + str);
				
				//存储返回字符串
				respon.str = str;
			}
			else
			{
				Log.d(TAG, "getStatusCode() err!");
				ret = Err.ERR_NETWORK;
			}
		}
		catch(HttpHostConnectException e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_NETWORK;
		}
		catch(ClientProtocolException e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_NETWORK;
		}
		catch(UnknownHostException e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_NETWORK;
		}
		catch(ConnectTimeoutException e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_NETWORK_TIMEOUT;
		}
		catch(Exception e)
		{
			Log.d(TAG, "Exception : " + e.toString());
			ret = Err.ERR_COMMON;
		}
		
		return ret;
	}
	
	/**
	 * 带重试的请求
	 * @param url - 要请求的网页地址
	 * @param params - 可以给网页发参数，采用NameVluePair的模式，不使用的话就置成null
	 * @param respon - 输出参数，存储网页返回的信息
	 * @param retry - 重试次数
	 * @return NO_ERR : 成功，无错误； 其他错误代码 : 有错误，失败；
	 * */
	public static int req_respon(String url, List<NameValuePair> params, Respon respon, int retry) {
		int count = 0;
		int ret = Err.NO_ERR;
		
		while(count < retry)
		{
			Log.d(TAG, "req_respon , try : " + count);
			ret = req_respon(url, params, respon);		//返回值就按最后一次的算
			//若成功，或者不是网络引起的错误，则退出
			if((ret != Err.ERR_NETWORK) && 
			   (ret != Err.ERR_NETWORK_TIMEOUT))
			{
				break;
			}
			
			count++;
		}
		
		return ret;
	}
}
