package com.alexcai.whoispy;

/**
 * 通用的错误代码类，统一工程中的错误代码
 * */
public class Err {
	/*--------------------------
	 * 常量
	 *-------------------------*/
	/*错误代码*/
	final static int NO_ERR 				= 0;	//无错误
	final static int ERR_COMMON 			= -1;	//通用错误
	final static int ERR_PASSWD 			= -2;	//用户名或密码错误
	final static int ERR_NETWORK  			= -3;	//网络错误
	final static int ERR_DATABASE 			= -4;	//数据库错误
	final static int ERR_PHONE_EXIST 		= -5;	//手机号已注册
	final static int ERR_NETWORK_TIMEOUT 	= -6;	//网络超时
	final static int ERR_USER_OFFLINE 		= -7;	//用户不在线
	final static int ERR_IO 				= -8;	//IO操作错误，例如文件读写等
	final static int ERR_FILE_NOT_FOUND 	= -9;	//找不到文件
	final static int ERR_PLAYER_INVALID 	= -10;	//无效玩家

	/**
	 * 通用错误异常
	 * */
	static class ExceptionCommon extends Exception {
		
		private static final long serialVersionUID = 1L;

		public ExceptionCommon() {
			super();		//调用Exception类的构造函数
		}
	}
	
	/**
	 * 网络超时异常
	 * */
	static class ExceptionNetworkTimeOut extends Exception {

		private static final long serialVersionUID = 1L;

		public ExceptionNetworkTimeOut() {
			super();		//调用Exception类的构造函数
		}
	}
	
	/**
	 * 网络异常
	 * */
	static class ExceptionNetwork extends Exception {

		private static final long serialVersionUID = 1L;

		public ExceptionNetwork() {
			super();
		}
	}
}
