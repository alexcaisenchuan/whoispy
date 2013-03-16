package com.alexcai.whoispy;

import android.app.Application;
import android.util.Log;

public class AppGameInfos extends Application{
	/*--------------------------
	 * 属性
	 *------------------------*/
	private static final String TAG = "whoispy.AppGameInfo";
	public Game game;		//游戏相关信息，放在这里可以让整个应用都读到
	
	/*--------------------------
	 * 重写方法
	 *------------------------*/
	public AppGameInfos() {
		//创建内部数据结构
		game = new Game(this);
	}
}
