package com.alexcai.whoispy;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES加解密类
 * */
public class EncrypAES {

	// 密钥字符串
	private String key = "al1x2a3c4i5e6c7u"; // <-128bit key
	// SecretKey 负责保存对称密钥
	private SecretKeySpec deskey;
	// Cipher负责完成加密或解密工作
	private Cipher c;
	// 该字节数组负责保存加密的结果
	private byte[] cipherByte;

	public EncrypAES() throws NoSuchAlgorithmException, NoSuchPaddingException {
		// 生成密钥
		byte[] keyBytes = key.getBytes();
		deskey = new SecretKeySpec(keyBytes, "AES");
		// 生成Cipher对象,指定其支持的DES算法
		c = Cipher.getInstance("AES");
	}

	/**
	 * 对字符串加密
	 * 
	 * @param str
	 *            - 要加密的字符串
	 * @return 加密后的字符串
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String Encrytor(String str) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		// 根据密钥，对Cipher对象进行初始化，ENCRYPT_MODE表示加密模式
		c.init(Cipher.ENCRYPT_MODE, deskey);
		// 将输入的字符串转换为byte数组
		byte[] b = str.getBytes();
		// 加密，结果保存进cipherByte
		cipherByte = c.doFinal(b);
		// 将加密后的byte数组转换为16进制形式
		String hexStr = parseByte2HexStr(cipherByte);
		// 返回加密后的字符串
		return hexStr;
	}

	/**
	 * 对字符串解密
	 * 
	 * @param buff
	 *            - 要解密的字符串(16进制字符串形式)
	 * @return 解密后的字符串
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String Decryptor(String buff) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		// 根据密钥，对Cipher对象进行初始化，DECRYPT_MODE表示加密模式
		c.init(Cipher.DECRYPT_MODE, deskey);
		// 将字符串转换为byte数组
		byte[] b = parseHexStr2Byte(buff);
		// 解密
		cipherByte = c.doFinal(b);
		// 返回解密后的字符串
		return new String(cipherByte);
	}

	/**
	 * 将二进制转换成16进制
	 * 
	 * @param buf
	 * @return
	 */
	public static String parseByte2HexStr(byte buf[]) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 将16进制转换为二进制
	 * 
	 * @param hexStr
	 * @return
	 */
	public static byte[] parseHexStr2Byte(String hexStr) {
		if (hexStr.length() < 1)
			return null;
		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2),
					16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}
}
