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
 * ���Ͷ���
 * */
class Respon {
	String str;		//��Ӧ�ַ���
	
	/**
	 * ���캯��
	 * */
	Respon(){
		str = "";
	}
}

/**
 * ��ҳ����ͨ���࣬���а�������������йصĳ��÷���
 * */
public class Web {
	/*------------------------------
	 * ����
	 * */
	private final static String TAG = "whoispy.Web";
	/*��������*/
	final static int HTTP_CONNECTION_TIMEOUT = 8000;		//���ӳ�ʱʱ��
	final static int HTTP_SOCKET_TIMEOUT 	 = 8000;		//Socket��ʱʱ��
	/*��ҳ���ص��ַ����������*/
	//flag
	final static String FLAG_RET = "ret";
	final static String FLAG_ERR_CODE = "err_code";
	final static String FLAG_SESSION_ID = "session_id";
	final static String FLAG_RET_WORD_ADD = "ret_word_add";	//��Ӵʻ��Ƿ�ɹ�����Ϣ
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
	/*��ҳ��ַ*/
	final static String WEB_URL_WORDS = "http://caisenchuan.web-184.com/whoispy/words.php";		//words.php
	final static String WEB_URL_USERS = "http://caisenchuan.web-184.com/whoispy/users.php";		//users.php
	
	/*--------------------------
	 * ����
	 * */
	
	/*--------------------------
	 * ����
	 * */
	/**
	 * ����ĳ����ҳ
	 * @param url - Ҫ�������ҳ��ַ
	 * @param params - ���Ը���ҳ������������NameVluePair��ģʽ����ʹ�õĻ����ó�null
	 * @param respon - ����������洢��ҳ���ص���Ϣ
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public static int req_respon(String url, List<NameValuePair> params, Respon respon) {
		int ret = Err.NO_ERR;		//����ֵ
	
		try{
			/*����http����*/
			HttpClient http = new DefaultHttpClient();
			HttpPost req = new HttpPost(url);
			
			/*���ó�ʱʱ��*/
			HttpParams http_params = http.getParams();
			HttpConnectionParams.setConnectionTimeout(http_params, HTTP_CONNECTION_TIMEOUT);
			HttpConnectionParams.setSoTimeout(http_params, HTTP_SOCKET_TIMEOUT);
			
			/*���ò���*/
			if(params != null)
			{
				HttpEntity entity = new UrlEncodedFormEntity(params, "utf-8");		//���ñ��뷽ʽ
				req.setEntity(entity);
			}
			
			/*����POST����*/
			HttpResponse http_respon = http.execute(req);
			
			/*�ȴ����ؽ��*/
			int status_code = http_respon.getStatusLine().getStatusCode();
			if(status_code == HttpStatus.SC_OK)
			{
				String str = EntityUtils.toString(http_respon.getEntity());
				Log.d(TAG, "Response str : " + str);
				
				//�洢�����ַ���
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
	 * �����Ե�����
	 * @param url - Ҫ�������ҳ��ַ
	 * @param params - ���Ը���ҳ������������NameVluePair��ģʽ����ʹ�õĻ����ó�null
	 * @param respon - ����������洢��ҳ���ص���Ϣ
	 * @param retry - ���Դ���
	 * @return NO_ERR : �ɹ����޴��� ����������� : �д���ʧ�ܣ�
	 * */
	public static int req_respon(String url, List<NameValuePair> params, Respon respon, int retry) {
		int count = 0;
		int ret = Err.NO_ERR;
		
		while(count < retry)
		{
			Log.d(TAG, "req_respon , try : " + count);
			ret = req_respon(url, params, respon);		//����ֵ�Ͱ����һ�ε���
			//���ɹ������߲�����������Ĵ������˳�
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
