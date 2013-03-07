package com.alexcai.whoispy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 一些杂项功能的集合
 * */
public class Misc {
	/**
	 * 手机号校验
	 * @param mobiles - 要校验的手机号
	 * @return 若是手机号，则返回true，否则返回false；
	 * */
	public static boolean isMobileNO(String mobiles){       
        Pattern p = Pattern.compile("[0-9,+,\\-, ]+");		//匹配0~9，+，- 以及空格，对字数没有限制
        Matcher m = p.matcher(mobiles);    
        return m.matches();       
    }
	
	/**
	 * 较为严格的手机号校验
	 * @param mobiles - 手机号
	 * @return true - 合法手机号; false - 非法手机号；
	 * */
	public static boolean isMobileNOStrict(String mobiles){
		Pattern p = Pattern.compile("\\d{11}");		//11个数字
		Matcher m = p.matcher(mobiles);
		return m.matches();
	}
	
	/**
	 * 规格化手机号，将各种手机号形式都统一成纯数字形式
	 * */
	public static String standardizeMobileNO(String mobiles){
		String str = mobiles.replace("-", "");	//不带"-"
		str = str.replace(" ", "");				//不带空格
		str = str.replace("+86", "");			//不带+86
		return str;
	}
}
