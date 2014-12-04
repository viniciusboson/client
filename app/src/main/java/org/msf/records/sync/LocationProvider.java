package org.msf.records.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import static org.msf.records.sync.LocationProviderContract.LocationColumns;
import static org.msf.records.sync.LocationProviderContract.PATH_LOCATIONS;
import static org.msf.records.sync.LocationProviderContract.PATH_LOCATION_NAMES;
import static org.msf.records.sync.LocationProviderContract.PATH_SUBLOCATIONS;
import static org.msf.records.sync.PatientProviderContract.CONTENT_AUTHORITY;

/**
 * LocationProvider for the cache database for locations.
 */
public class LocationProvider implements MsfRecordsProvider.SubContentProvider {

    private static final String TAG = "LocationProvider";

    /**
     * URI ID for route: /locations
     */
    public static final int LOCATIONS = 15;

    /**
     * URI ID for route: /location/{id}
     */
    public static final int LOCATION = 16;

    /**
     * URI ID for route: /sublocations/{parent}
     */
    public static final int SUBLOCATIONS = 17;

    /**
     * URI ID for route: /locationnames
     */
    public static final int LOCATION_NAMES = 18;

    /**
     * URI ID for route: /locationnames/{id}
     */
    public static final int LOCATION_NAMES_ID = 19;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);


    static {
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_LOCATIONS, LOCATIONS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_LOCATIONS), LOCATION);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_SUBLOCATIONS), SUBLOCATIONS);
        sUriMatcher.addURI(CONTENT_AUTHORITY, PATH_LOCATION_NAMES, LOCATION_NAMES);
        sUriMatcher.addURI(CONTENT_AUTHORITY, subDirs(PATH_LOCATION_NAMES), LOCATION_NAMES_ID);
    }

    private static final String[] PATHS = new String[]{
            PATH_LOCATIONS, subDirs(PATH_LOCATIONS), subDirs(PATH_SUBLOCATIONS),
            PATH_LOCATION_NAMES, subDirs(PATH_LOCATION_NAMES)
    };

    @Override
    public String[] getPaths() {
        return PATHS;
    }

    private static String subDirs(String base) {
        return base + "/*";
    }

    @Override
    public Cursor query(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                        String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        int uriMatch = sUriMatcher.match(uri);
        Cursor c;
        switch (uriMatch) {
            case SUBLOCATIONS:
                builder.table(PatientDatabase.LOCATIONS_TABLE_NAME);
                // Return entries with the given parent id.
                String parentId = uri.getLastPathSegment();
                builder.where(LocationColumns.PARENT_UUID + "=?", parentId);
            case LOCATION:
                builder.table(PatientDatabase.LOCATIONS_TABLE_NAME);
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(LocationColumns.LOCATION_UUID + "=?", id);
            case LOCATIONS:
                builder.table(PatientDatabase.LOCATIONS_TABLE_NAME);
                break;
            case LOCATION_NAMES:
                builder.table(PatientDatabase.LOCATION_NAMES_TABLE_NAME);
                break;
            case LOCATION_NAMES_ID:
                builder.table(PatientDatabase.LOCATION_NAMES_TABLE_NAME);
                // Return a single entry, by ID.
                String locationId = uri.getLastPathSegment();
                builder.where(LocationColumns.LOCATION_UUID + "=?", locationId);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        builder.where(selection, selectionArgs);
        c = builder.query(db, projection, sortOrder);
        c.setNotificationUri(contentResolver, uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LOCATION:
            case LOCATIONS:
            case SUBLOCATIONS:
                return LocationProviderContract.LOCATION_CONTENT_TYPE;
            case LOCATION_NAMES:
            case LOCATION_NAMES_ID:
                return LocationProviderContract.LOCATION_NAME_CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        String tableName;
        Uri preIdUri;
        switch (match) {
            case LOCATION:
                String id = uri.getLastPathSegment();
                tableName = PatientDatabase.LOCATIONS_TABLE_NAME;
                preIdUri = LocationProviderContract.LOCATIONS_CONTENT_URI;
                values.put(LocationColumns.LOCATION_UUID, id);
                break;
            case LOCATIONS:
                tableName = PatientDatabase.LOCATIONS_TABLE_NAME;
                preIdUri = LocationProviderContract.LOCATIONS_CONTENT_URI;
                break;
            case LOCATION_NAMES_ID:
                String locationId = uri.getLastPathSegment();
                tableName = PatientDatabase.LOCATION_NAMES_TABLE_NAME;
                preIdUri = LocationProviderContract.LOCATION_NAMES_CONTENT_URI;
                values.put(LocationColumns.LOCATION_UUID, locationId);
                break;
            case LOCATION_NAMES:
                tableName = PatientDatabase.LOCATION_NAMES_TABLE_NAME;
                preIdUri = LocationProviderContract.LOCATION_NAMES_CONTENT_URI;
                break;
            case SUBLOCATIONS:
                throw new UnsupportedOperationException("Sublocations are query only");
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        long id = db.replaceOrThrow(tableName, null, values);
        result = Uri.parse(preIdUri + "/" + id);
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return result;
    }

    @Override
    public int delete(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      String selection, String[] selectionArgs) {
        SelectionBuilder builder = new SelectionBuilder();
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        String tableName;
        switch (match) {
            case LOCATIONS:
                tableName = PatientDatabase.LOCATIONS_TABLE_NAME;
                break;
            case LOCATION_NAMES:
                tableName = PatientDatabase.LOCATION_NAMES_TABLE_NAME;
                break;
            case LOCATION:
                String id = uri.getLastPathSegment();
                builder.where(LocationColumns._ID + "=?", id);
                tableName = PatientDatabase.LOCATIONS_TABLE_NAME;
                break;
            case LOCATION_NAMES_ID:
                String locationId = uri.getLastPathSegment();
                builder.where(LocationColumns.LOCATION_UUID + "=?", locationId);
                tableName = PatientDatabase.LOCATION_NAMES_TABLE_NAME;
                break;
            case SUBLOCATIONS:
                throw new UnsupportedOperationException("Sublocations are query only");
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = builder.table(tableName)
                .where(selection, selectionArgs)
                .delete(db);
        // Send broadcast to registered ContentObservers, to refresh UI.
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(SQLiteOpenHelper dbHelper, ContentResolver contentResolver, Uri uri,
                      ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        SelectionBuilder builder = new SelectionBuilder();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case LOCATION:
                builder.table(PatientDatabase.LOCATIONS_TABLE_NAME);
                // Return a single entry, by ID.
                String id = uri.getLastPathSegment();
                builder.where(LocationColumns.LOCATION_UUID + "=?", id);
            case LOCATIONS:
                builder.table(PatientDatabase.LOCATIONS_TABLE_NAME);
                break;
            case LOCATION_NAMES:
                builder.table(PatientDatabase.LOCATION_NAMES_TABLE_NAME);
                break;
            case LOCATION_NAMES_ID:
                builder.table(PatientDatabase.LOCATION_NAMES_TABLE_NAME);
                // Return a single entry, by ID.
                String locationId = uri.getLastPathSegment();
                builder.where(LocationColumns.LOCATION_UUID + "=?", locationId);
            case SUBLOCATIONS:
                throw new UnsupportedOperationException("Sublocations are query only");
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        int count = builder.where(selection, selectionArgs)
                .update(db, values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}