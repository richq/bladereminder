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
        public static final String DATE_DESC = "date DESC";
        public static final String COUNT = "count";
        public static final String COMMENT = "comment";
        public static final String RAZOR = "razor";


        public static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + "("
            + _ID + " integer primary key autoincrement, "
            + DATE + " text not null, "
            + COUNT + " integer, "
            + COMMENT + " text, "
            + RAZOR + " text"  // fk to razors...
            + ");";

        public final static String SELECT = String.format(
                "select %s, strftime('%%Y-%%m-%%d', %s), %s, %s, %s from %s ",
                _ID, DATE, COUNT, COMMENT, RAZOR, TABLE_NAME);
        public final static String ORDER_BY_DATE = String.format(
                " order by %s", DATE_DESC);
        public final static String ORDER_BY_DATE_ASC = String.format(
                " order by %s ASC", DATE);

        public final static String ORDER_BY_COUNT = String.format(
                " order by %s DESC", COUNT);

        public final static String ORDER_BY_COUNT_ASC = String.format(
                " order by %s ASC", COUNT);

        public final static String WHERE_COUNT_GT_1 = " where count >= 1 ";

        // avoid instances
        private Shaves() {}
    }

    public static class Razors implements BaseColumns {
        public static final String BASE_PATH = "razors";
        public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/es.quirk.bladereminder.razor";

        public static final String TABLE_NAME = "razors";
        public static final String NAME = "name";

        public static final String DATABASE_CREATE = "create table "
            + TABLE_NAME + "("
            + _ID + " integer primary key autoincrement, "
            + NAME + " text not null "
            + ");";

        public final static String SELECT_COUNT = String.format(
                "select count(%s) from %s ", _ID, TABLE_NAME);

        public final static String SELECT = String.format(
                "select %s, %s from %s ",
                _ID, NAME, TABLE_NAME);

        // avoid instances
        private Razors() {}
    }

    // avoid instances
    private Contract() {}
}
