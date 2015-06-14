package es.quirk.bladereminder.database;

import android.content.ContentResolver;
import android.provider.BaseColumns;

/**
 * Convenience definitions for UsageProvider
 */
public final class Contract {

    public static class Shaves implements BaseColumns {
        public static final String BASE_PATH = "shaves";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/es.quirk.bladereminder.shave";

        public static final String TABLE_NAME = "shaves";
        public static final String DATE = "date";
        public static final String COUNT = "count";
        public static final String COMMENT = "comment";


        public static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + "("
            + _ID + " integer primary key autoincrement, "
            + DATE + " text not null, "
            + COUNT + " integer, "
            + COMMENT + " text"
            + ");";

        public final static String SELECT = String.format(
                "select %s, strftime('%%Y-%%m-%%d', %s), %s, %s from %s ",
                _ID, DATE, COUNT, COMMENT, TABLE_NAME);
        public final static String ORDER_BY_DATE = String.format(
                " order by %s DESC", DATE);
        public final static String ORDER_BY_DATE_ASC = String.format(
                " order by %s ASC", DATE);

        // avoid instances
        private Shaves() {}
    }

    // avoid instances
    private Contract() {}
}
