package com.alexcai.whoispy;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES�ӽ�����
 * */
public class EncrypAES {

	// ��Կ�ַ���
	private String key = "al1x2a3c4i5e6c7u"; // <-128bit key
	// SecretKey ���𱣴�Գ���Կ
	private SecretKeySpec deskey;
	// Cipher������ɼ��ܻ���ܹ���
	private Cipher c;
	// ���ֽ����鸺�𱣴���ܵĽ��
	private byte[] cipherByte;

	public EncrypAES() throws NoSuchAlgorithmException, NoSuchPaddingException {
		// ������Կ
		byte[] keyBytes = key.getBytes();
		deskey = new SecretKeySpec(keyBytes, "AES");
		// ����Cipher����,ָ����֧�ֵ�DES�㷨
		c = Cipher.getInstance("AES");
	}

	/**
	 * ���ַ�������
	 * 
	 * @param str
	 *            - Ҫ���ܵ��ַ���
	 * @return ���ܺ���ַ���
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String Encrytor(String str) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		// ������Կ����Cipher������г�ʼ����ENCRYPT_MODE��ʾ����ģʽ
		c.init(Cipher.ENCRYPT_MODE, deskey);
		// ��������ַ���ת��Ϊbyte����
		byte[] b = str.getBytes();
		// ���ܣ���������cipherByte
		cipherByte = c.doFinal(b);
		// �����ܺ��byte����ת��Ϊ16������ʽ
		String hexStr = parseByte2HexStr(cipherByte);
		// ���ؼ��ܺ���ַ���
		return hexStr;
	}

	/**
	 * ���ַ�������
	 * 
	 * @param buff
	 *            - Ҫ���ܵ��ַ���(16�����ַ�����ʽ)
	 * @return ���ܺ���ַ���
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	public String Decryptor(String buff) throws InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException {
		// ������Կ����Cipher������г�ʼ����DECRYPT_MODE��ʾ����ģʽ
		c.init(Cipher.DECRYPT_MODE, deskey);
		// ���ַ���ת��Ϊbyte����
		byte[] b = parseHexStr2Byte(buff);
		// ����
		cipherByte = c.doFinal(b);
		// ���ؽ��ܺ���ַ���
		return new String(cipherByte);
	}

	/**
	 * ��������ת����16����
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
	 * ��16����ת��Ϊ������
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
