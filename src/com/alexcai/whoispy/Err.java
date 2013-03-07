package com.alexcai.whoispy;

/**
 * ͨ�õĴ�������࣬ͳһ�����еĴ������
 * */
public class Err {
	/*--------------------------
	 * ����
	 *-------------------------*/
	/*�������*/
	final static int NO_ERR 				= 0;	//�޴���
	final static int ERR_COMMON 			= -1;	//ͨ�ô���
	final static int ERR_PASSWD 			= -2;	//�û������������
	final static int ERR_NETWORK  			= -3;	//�������
	final static int ERR_DATABASE 			= -4;	//���ݿ����
	final static int ERR_PHONE_EXIST 		= -5;	//�ֻ�����ע��
	final static int ERR_NETWORK_TIMEOUT 	= -6;	//���糬ʱ
	final static int ERR_USER_OFFLINE 		= -7;	//�û�������
	final static int ERR_IO 				= -8;	//IO�������������ļ���д��
	final static int ERR_FILE_NOT_FOUND 	= -9;	//�Ҳ����ļ�
	final static int ERR_PLAYER_INVALID 	= -10;	//��Ч���

	/**
	 * ͨ�ô����쳣
	 * */
	static class ExceptionCommon extends Exception {
		
		private static final long serialVersionUID = 1L;

		public ExceptionCommon() {
			super();		//����Exception��Ĺ��캯��
		}
	}
	
	/**
	 * ���糬ʱ�쳣
	 * */
	static class ExceptionNetworkTimeOut extends Exception {

		private static final long serialVersionUID = 1L;

		public ExceptionNetworkTimeOut() {
			super();		//����Exception��Ĺ��캯��
		}
	}
	
	/**
	 * �����쳣
	 * */
	static class ExceptionNetwork extends Exception {

		private static final long serialVersionUID = 1L;

		public ExceptionNetwork() {
			super();
		}
	}
}
