package com.alexcai.whoispy;

import android.app.Application;
import android.util.Log;

public class AppGameInfos extends Application{
	/*--------------------------
	 * ����
	 *------------------------*/
	private static final String TAG = "whoispy.AppGameInfo";
	public Game game;		//��Ϸ�����Ϣ�������������������Ӧ�ö�����
	
	/*--------------------------
	 * ��д����
	 *------------------------*/
	public AppGameInfos() {
		//�����ڲ����ݽṹ
		game = new Game(this);
	}
}
