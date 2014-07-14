package com.xoverto.matchthecity;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class VenueProvider extends ContentProvider {

    public static final Uri CONTENT_URI = Uri.parse("content://com.xoverto.matchthecity/venues");

    // Column names
    public static final String KEY_ID = "_id";
    public static final String KEY_VENUE_ID = "venue_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_UPDATED = "updated";
    public static final String KEY_LOCATION_LAT = "latitude";
    public static final String KEY_LOCATION_LNG = "longitude";
    public static final String KEY_TELEPHONE = "telephone";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_WEB = "web";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_POSTCODE = "postcode";

    // Create the constants used to differentiate between the different URI requests
    private static final int VENUES = 1;
    private static final int VENUE_ID = 2;

    private static final UriMatcher uriMatcher;

    // Allocate the UriMatcher object, where a URI ending in 'venues' will correspond to a request for all venues,
    // and 'venues' with a trailing '/[rowID]' will represent a single venue row.

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.xoverto.matchthecity", "venues", VENUES);
        uriMatcher.addURI("com.xoverto.matchthecity", "venues/#", VENUE_ID);
    }

    VenueDatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int count;
        switch (uriMatcher.match(uri)) {
            case VENUES:
                count = database.delete(VenueDatabaseHelper.VENUE_TABLE, selection, selectionArgs);
                break;

            case VENUE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.delete(VenueDatabaseHelper.VENUE_TABLE, KEY_ID + "=" + segment + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default: throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)) {
            case VENUES: return "vnd.android.cursor.dir/vnd.com.xoverto.matchthecity.venues";
            case VENUE_ID: return "vnd.android.cursor.item/vnd.com.xoverto.matchthecity.venues";
            default: throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        // Insert the new row. The call to the database.insert will return the row number if it is successful.
        long rowID = database.insert(VenueDatabaseHelper.VENUE_TABLE, "venue", values);

        // Return a URI to the newly inserted row on success.
        if(rowID > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(CONTENT_URI, null);
            return newUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        dbHelper = new VenueDatabaseHelper(context, VenueDatabaseHelper.DATABASE_NAME, null, VenueDatabaseHelper.DATABASE_VERSION);

        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(VenueDatabaseHelper.VENUE_TABLE);

        // If this is a row query, limit the result set to the passed in row
        switch (uriMatcher.match(uri)) {
            case VENUE_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
            default: break;
        }

        // If no sort order is specified, sort by name
        String orderBy;
        if(TextUtils.isEmpty(sortOrder)) {
            orderBy = KEY_NAME;
        } else {
            orderBy = sortOrder;
        }

        // Apply the query to the underlying database
        Cursor c = qb.query(database,
                projection,
                selection, selectionArgs,
                null, null,
                orderBy);

        // Register the contexts ContentResolver to be notified if the cursor result set changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        // Return a cursor to the query result
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int count;
        switch (uriMatcher.match(uri)) {
            case VENUES:
                count = database.update(VenueDatabaseHelper.VENUE_TABLE, values, selection, selectionArgs);
                break;

            case VENUE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.update(VenueDatabaseHelper.VENUE_TABLE, values, KEY_ID + "=" + segment + (!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    // Helper class for opening, creating and managing database version control
    private static class VenueDatabaseHelper extends SQLiteOpenHelper {
        private static final String TAG = "VenueProvider";
        private static final String DATABASE_NAME = "venues.db";
        private static final int DATABASE_VERSION = 1;
        private static final String VENUE_TABLE = "venues";
        private static final String DATABASE_CREATE = "create table " + VENUE_TABLE + " ("
                + KEY_ID + " integer primary key autoincrement, "
                + KEY_VENUE_ID + " TEXT,"
                + KEY_NAME + " TEXT, "
                + KEY_UPDATED + " INTEGER, "
                + KEY_LOCATION_LAT + " FLOAT, "
                + KEY_LOCATION_LNG + " FLOAT, "
                + KEY_TELEPHONE + " TEXT, "
                + KEY_ADDRESS + " TEXT, "
                + KEY_POSTCODE + " TEXT, "
                + KEY_WEB + " TEXT,"
                + KEY_EMAIL + " TEXT);";

        // The underlying database
        private SQLiteDatabase carParkDB;

        public VenueDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {

            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion + " which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + VENUE_TABLE);
            onCreate(db);
        }
    }
}
