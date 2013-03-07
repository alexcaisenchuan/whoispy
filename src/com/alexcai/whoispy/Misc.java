package com.alexcai.whoispy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * һЩ����ܵļ���
 * */
public class Misc {
	/**
	 * �ֻ���У��
	 * @param mobiles - ҪУ����ֻ���
	 * @return �����ֻ��ţ��򷵻�true�����򷵻�false��
	 * */
	public static boolean isMobileNO(String mobiles){       
        Pattern p = Pattern.compile("[0-9,+,\\-, ]+");		//ƥ��0~9��+��- �Լ��ո񣬶�����û������
        Matcher m = p.matcher(mobiles);    
        return m.matches();       
    }
	
	/**
	 * ��Ϊ�ϸ���ֻ���У��
	 * @param mobiles - �ֻ���
	 * @return true - �Ϸ��ֻ���; false - �Ƿ��ֻ��ţ�
	 * */
	public static boolean isMobileNOStrict(String mobiles){
		Pattern p = Pattern.compile("\\d{11}");		//11������
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}
	
	/**
	 * ����ֻ��ţ��������ֻ�����ʽ��ͳһ�ɴ�������ʽ
	 * */
	public static String standardizeMobileNO(String mobiles){
		String str = mobiles.replace("-", "");	//����"-"
		str = str.replace(" ", "");				//�����ո�
		str = str.replace("+86", "");			//����+86
		return str;
	}
}
