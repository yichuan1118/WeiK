package com.metaisle.weik.data;

import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.metaisle.util.Util;

public class Provider extends ContentProvider {
	private static final String AUTHORITY = "com.metaisle.weik.data.Provider";

	private static final int MATCH_TIMELINE = 101;
	private static final int MATCH_TIMELINE_ID = 102;

	private static final int MATCH_USER = 201;
	private static final int MATCH_USER_ID = 202;

	private static final int MATCH_MESSAGE = 301;
	private static final int MATCH_MESSAGE_ID = 302;

	private static final int MATCH_COMMENT = 401;
	private static final int MATCH_COMMENT_ID = 402;

	private static final int MATCH_FOLLOWER = 501;
	private static final int MATCH_FRIEND = 601;

	private static final int MATCH_URL = 701;

	public static final Uri TIMELINE_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + TimelineTable.TIMELINE_TABLE);

	public static final Uri USER_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + UserTable.USER_TABLE);

	public static final Uri MESSAGE_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + MessageTable.MESSAGE_TABLE);

	public static final Uri COMMENT_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + CommentTable.COMMENT_TABLE);

	public static final Uri FOLLOWER_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + "FOLLOWER");

	public static final Uri FRIEND_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + "FRIEND");

	public static final Uri URL_CONTENT_URI = Uri.parse("content://"
			+ AUTHORITY + "/" + UrlTable.URL_TABLE);

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, TimelineTable.TIMELINE_TABLE,
				MATCH_TIMELINE);
		sURIMatcher.addURI(AUTHORITY, TimelineTable.TIMELINE_TABLE + "/#",
				MATCH_TIMELINE_ID);

		sURIMatcher.addURI(AUTHORITY, UserTable.USER_TABLE, MATCH_USER);
		sURIMatcher.addURI(AUTHORITY, UserTable.USER_TABLE + "/#",
				MATCH_USER_ID);

		sURIMatcher
				.addURI(AUTHORITY, MessageTable.MESSAGE_TABLE, MATCH_MESSAGE);
		sURIMatcher.addURI(AUTHORITY, MessageTable.MESSAGE_TABLE + "/#",
				MATCH_MESSAGE_ID);

		sURIMatcher
				.addURI(AUTHORITY, CommentTable.COMMENT_TABLE, MATCH_COMMENT);
		sURIMatcher.addURI(AUTHORITY, CommentTable.COMMENT_TABLE + "/#",
				MATCH_COMMENT_ID);

		sURIMatcher.addURI(AUTHORITY, "FOLLOWER", MATCH_FOLLOWER);
		sURIMatcher.addURI(AUTHORITY, "FRIEND", MATCH_FRIEND);

		sURIMatcher.addURI(AUTHORITY, UrlTable.URL_TABLE, MATCH_URL);
	}

	private Database database;

	@Override
	public boolean onCreate() {
		Util.log("oncreate");
		database = new Database(getContext());

		String sql = "DELETE FROM " + TimelineTable.TIMELINE_TABLE + " WHERE "
				+ TimelineTable._ID + " NOT IN ( SELECT " + TimelineTable._ID
				+ " FROM " + TimelineTable.TIMELINE_TABLE + " ORDER BY "
				+ TimelineTable._ID + " DESC LIMIT 1000) ";

		Util.log(sql);

		database.getWritableDatabase().execSQL(sql);

		return true;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		String id = uri.getLastPathSegment();
		switch (uriType) {

		case MATCH_TIMELINE:
			rowsDeleted = sqlDB.delete(TimelineTable.TIMELINE_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_TIMELINE_ID:
			if (TextUtils.isEmpty(selection))
				selection = TimelineTable.STATUS_ID + "=" + id;
			else
				selection = selection + " AND " + TimelineTable.STATUS_ID + "="
						+ id;
			rowsDeleted = sqlDB.delete(TimelineTable.TIMELINE_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_USER:
			rowsDeleted = sqlDB.delete(UserTable.USER_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_USER_ID:
			if (TextUtils.isEmpty(selection))
				selection = UserTable.USER_ID + "=" + id;
			else
				selection = selection + " AND " + UserTable.USER_ID + "=" + id;
			rowsDeleted = sqlDB.delete(UserTable.USER_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_MESSAGE:
			rowsDeleted = sqlDB.delete(MessageTable.MESSAGE_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_MESSAGE_ID:
			if (TextUtils.isEmpty(selection))
				selection = MessageTable.STATUS_ID + "=" + id;
			else
				selection = selection + " AND " + MessageTable.STATUS_ID + "="
						+ id;
			rowsDeleted = sqlDB.delete(MessageTable.MESSAGE_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_COMMENT:
			rowsDeleted = sqlDB.delete(CommentTable.COMMENT_TABLE, selection,
					selectionArgs);
			break;
		case MATCH_COMMENT_ID:
			if (TextUtils.isEmpty(selection))
				selection = CommentTable.COMMENT_ID + "=" + id;
			else
				selection = selection + " AND " + CommentTable.COMMENT_ID + "="
						+ id;
			rowsDeleted = sqlDB.delete(CommentTable.COMMENT_TABLE, selection,
					selectionArgs);
			break;

		case MATCH_FOLLOWER:
			rowsDeleted = sqlDB.delete(RelationshipTable.RELATIONSHIP_TABLE,
					selection, selectionArgs);
			break;
		case MATCH_FRIEND:
			rowsDeleted = sqlDB.delete(RelationshipTable.RELATIONSHIP_TABLE,
					selection, selectionArgs);
			break;

		case MATCH_URL:
			rowsDeleted = sqlDB.delete(UrlTable.URL_TABLE, selection,
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = database.getWritableDatabase();
		long id = -1;
		switch (match) {

		case MATCH_TIMELINE:
			String status_id = values.getAsString(TimelineTable.STATUS_ID);
			id = db.insertWithOnConflict(TimelineTable.TIMELINE_TABLE, null,
					values, SQLiteDatabase.CONFLICT_IGNORE);
			db.update(TimelineTable.TIMELINE_TABLE, values,
					TimelineTable.STATUS_ID + "=?", new String[] { status_id });

			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(TimelineTable.TIMELINE_TABLE + "/" + id);

		case MATCH_USER:
			String user_id = values.getAsString(UserTable.USER_ID);
			id = db.insertWithOnConflict(UserTable.USER_TABLE, null, values,
					SQLiteDatabase.CONFLICT_IGNORE);
			db.updateWithOnConflict(UserTable.USER_TABLE, values,
					UserTable.USER_ID + "=?", new String[] { user_id },
					SQLiteDatabase.CONFLICT_IGNORE);

			getContext().getContentResolver().notifyChange(uri, null);
			getContext().getContentResolver().notifyChange(
					FOLLOWER_CONTENT_URI, null);
			getContext().getContentResolver().notifyChange(FRIEND_CONTENT_URI,
					null);
			return Uri.parse(UserTable.USER_TABLE + "/" + id);

		case MATCH_MESSAGE:
			id = db.insertWithOnConflict(MessageTable.MESSAGE_TABLE, null,
					values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(MessageTable.MESSAGE_TABLE + "/" + id);

		case MATCH_COMMENT:
			String comment_id = values.getAsString(CommentTable.COMMENT_ID);
			id = db.insertWithOnConflict(CommentTable.COMMENT_TABLE, null,
					values, SQLiteDatabase.CONFLICT_IGNORE);
			db.update(CommentTable.COMMENT_TABLE, values,
					CommentTable.COMMENT_ID + "=?", new String[] { comment_id });
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(CommentTable.COMMENT_TABLE + "/" + id);

		case MATCH_FOLLOWER:
			id = db.insertWithOnConflict(RelationshipTable.RELATIONSHIP_TABLE,
					null, values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(RelationshipTable.RELATIONSHIP_TABLE + "/" + id);

		case MATCH_FRIEND:
			id = db.insertWithOnConflict(RelationshipTable.RELATIONSHIP_TABLE,
					null, values, SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(RelationshipTable.RELATIONSHIP_TABLE + "/" + id);

		case MATCH_URL:
			id = db.insertWithOnConflict(UrlTable.URL_TABLE, null, values,
					SQLiteDatabase.CONFLICT_IGNORE);
			getContext().getContentResolver().notifyChange(uri, null);
			return Uri.parse(CommentTable.COMMENT_TABLE + "/" + id);

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	public String upsertSql(String table, ContentValues values) {
		String sql = "INSERT OR REPLACE INTO " + table + " (";

		String value = " ) VALUES ( ";

		long status_id = values.getAsLong(TimelineTable.STATUS_ID);

		// ---
		sql += TimelineTable.CACHED_AT;
		value = value + "(select " + TimelineTable.CACHED_AT + " from " + table
				+ " where " + TimelineTable.STATUS_ID + " = " + status_id + ")";

		for (Entry<String, Object> v : values.valueSet()) {
			sql = sql + "," + v.getKey();
			if (v.getValue() instanceof Boolean) {
				if ((Boolean) v.getValue() == false) {
					value = value + "," + 0 + " ";
				} else {
					value = value + "," + 1 + " ";
				}
			} else {
				value = value
						+ ","
						+ DatabaseUtils.sqlEscapeString(String.valueOf(v
								.getValue()));
			}
		}

		Util.log(sql + value + ");");
		return sql + value + ");";
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = null;
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		Cursor cursor = null;
		String id = uri.getLastPathSegment();

		switch (match) {

		case MATCH_TIMELINE:
			queryBuilder.setTables(TimelineTable.TIMELINE_TABLE + " LEFT JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.AUTHOR_ID + "=" + UserTable.USER_TABLE
					+ "." + UserTable.USER_ID + ") LEFT JOIN "
					+ TimelineTable.TIMELINE_TABLE + " AS RT ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.RETWEETED_STATUS + "=RT."
					+ TimelineTable.STATUS_ID + " ) LEFT JOIN "
					+ UserTable.USER_TABLE + " AS RT_USER ON ( RT."
					+ TimelineTable.AUTHOR_ID + "=RT_USER." + UserTable.USER_ID
					+ ")");
			break;
		case MATCH_TIMELINE_ID:
			queryBuilder.setTables(TimelineTable.TIMELINE_TABLE + " LEFT JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.AUTHOR_ID + "=" + UserTable.USER_TABLE
					+ "." + UserTable.USER_ID + ") LEFT JOIN "
					+ TimelineTable.TIMELINE_TABLE + " AS RT ON ("
					+ TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.RETWEETED_STATUS + "=RT."
					+ TimelineTable.STATUS_ID + " ) LEFT JOIN "
					+ UserTable.USER_TABLE + " AS RT_USER ON ( RT."
					+ TimelineTable.AUTHOR_ID + "=RT_USER." + UserTable.USER_ID
					+ ")");
			queryBuilder.appendWhere(TimelineTable.TIMELINE_TABLE + "."
					+ TimelineTable.STATUS_ID + "=" + id);
			break;

		case MATCH_USER:
			queryBuilder.setTables(UserTable.USER_TABLE);
			break;
		case MATCH_USER_ID:
			queryBuilder.setTables(UserTable.USER_TABLE);
			queryBuilder.appendWhere(UserTable.USER_ID + "=" + id);
			break;

		case MATCH_MESSAGE:
			queryBuilder.setTables(MessageTable.MESSAGE_TABLE + " LEFT JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ MessageTable.MESSAGE_TABLE + "." + MessageTable.SENDER_ID
					+ "=" + UserTable.USER_TABLE + "." + UserTable.USER_ID
					+ ")");
			break;
		case MATCH_MESSAGE_ID:
			queryBuilder.setTables(MessageTable.MESSAGE_TABLE + " LEFT JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ MessageTable.MESSAGE_TABLE + "." + MessageTable.SENDER_ID
					+ "=" + UserTable.USER_TABLE + "." + UserTable.USER_ID
					+ ")");
			queryBuilder.appendWhere(MessageTable.STATUS_ID + "=" + id);
			break;

		case MATCH_COMMENT:
			queryBuilder.setTables(

			CommentTable.COMMENT_TABLE + " LEFT JOIN " + UserTable.USER_TABLE
					+ " ON (" + CommentTable.COMMENT_TABLE + "."
					+ CommentTable.AUTHOR_ID + "=" + UserTable.USER_TABLE + "."
					+ UserTable.USER_ID + ") "

					+ "LEFT JOIN " + CommentTable.COMMENT_TABLE
					+ " AS REC ON (" + CommentTable.COMMENT_TABLE + "."
					+ CommentTable.REPLIED_COMMENT + "=REC."
					+ CommentTable.COMMENT_ID + ") "

					+ "LEFT JOIN " + UserTable.USER_TABLE
					+ " AS REC_U ON ( REC." + CommentTable.AUTHOR_ID
					+ "=REC_U." + UserTable.USER_ID + ") "

					+ "LEFT JOIN " + TimelineTable.TIMELINE_TABLE
					+ " AS RES ON (" + CommentTable.COMMENT_TABLE + "."
					+ CommentTable.REPLIED_STATUS + "= RES."
					+ TimelineTable.STATUS_ID + ") "

					+ "LEFT JOIN " + UserTable.USER_TABLE
					+ " AS RES_U ON ( RES." + TimelineTable.AUTHOR_ID
					+ "=RES_U." + UserTable.USER_ID + ") ");
			break;
		case MATCH_COMMENT_ID:
			queryBuilder.setTables(CommentTable.COMMENT_TABLE + " LEFT JOIN "
					+ UserTable.USER_TABLE + " ON ("
					+ CommentTable.COMMENT_TABLE + "." + CommentTable.AUTHOR_ID
					+ "=" + UserTable.USER_TABLE + "." + UserTable.USER_ID
					+ ") LEFT JOIN " + CommentTable.COMMENT_TABLE
					+ " AS RE_C ON (" + CommentTable.COMMENT_TABLE + "."
					+ CommentTable.REPLIED_COMMENT + "=RE_C."
					+ CommentTable.COMMENT_ID + ") LEFT JOIN "
					+ TimelineTable.TIMELINE_TABLE + " AS RE_S ON ("
					+ CommentTable.COMMENT_TABLE + "."
					+ CommentTable.REPLIED_STATUS + "= RE_S."
					+ TimelineTable.STATUS_ID + ")");
			queryBuilder.appendWhere(CommentTable.COMMENT_ID + "=" + id);
			break;

		case MATCH_FOLLOWER:
			queryBuilder.setTables(RelationshipTable.RELATIONSHIP_TABLE
					+ " LEFT JOIN " + UserTable.USER_TABLE + " ON ("
					+ UserTable.USER_TABLE + "." + UserTable.USER_ID + "="
					+ RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWER + ")");
			break;

		case MATCH_FRIEND:
			queryBuilder.setTables(RelationshipTable.RELATIONSHIP_TABLE
					+ " LEFT JOIN " + UserTable.USER_TABLE + " ON ("
					+ UserTable.USER_TABLE + "." + UserTable.USER_ID + "="
					+ RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWEE + ")");
			break;

		case MATCH_URL:
			queryBuilder.setTables(UrlTable.URL_TABLE);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		db = database.getReadableDatabase();
		cursor = queryBuilder.query(db, projection, selection, selectionArgs,
				null, null, sortOrder);
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = sURIMatcher.match(uri);
		SQLiteDatabase db = database.getWritableDatabase();
		String id = uri.getLastPathSegment();
		int rowsUpdated = 0;

		// Util.log("match " + match + " id " + id + " selection " + selection
		// + " seletionArgs " + Arrays.toString(selectionArgs));

		switch (match) {
		case MATCH_TIMELINE_ID:
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = db.update(TimelineTable.TIMELINE_TABLE, values,
						TimelineTable.STATUS_ID + "=?",
						new String[] { String.valueOf(id) });
			} else {
				rowsUpdated = db.update(TimelineTable.TIMELINE_TABLE, values,
						TimelineTable.STATUS_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;

		case MATCH_URL:
			rowsUpdated = db.update(UrlTable.URL_TABLE, values, selection,
					selectionArgs);
			break;
			
		case MATCH_USER:
			rowsUpdated = db.update(UserTable.USER_TABLE, values, selection,
					selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

}
