package com.metaisle.weik.data;

import android.database.sqlite.SQLiteDatabase;

import com.metaisle.util.Util;

public class UrlTable extends BaseTable{

	public static final String URL_TABLE = "URL_TABLE";

	public static final String URL_SHORT = "URL_SHORT";
	public static final String URL_LONG = "URL_LONG";
	public static final String URL_TYPE 	= "URL_TYPE";
	public static final String STATUS_ID = "STATUS_ID";
	public static final String CACHED_AT = "CACHED_AT";
	
	public static void onCreate(SQLiteDatabase db){
		
		Util.log("Create table " + URL_TABLE);
		db.execSQL("CREATE TABLE " + URL_TABLE + " (" 
				+ _ID				+ " INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ URL_SHORT	 	+ " TEXT UNIQUE NOT NULL," 
				+ URL_LONG	 	+ " TEXT," 
				+ URL_TYPE		+ " INTEGER,"
				+ STATUS_ID		+ " INTEGER,"
				+ CACHED_AT		+ " INTEGER"
				+ ");");
	}
	
	public static void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Util.log("onUpgrade");
		db.execSQL("DROP TABLE IF EXISTS " + URL_TABLE);
		onCreate(db);
	}
}
