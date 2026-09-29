package com.docestoque.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "doc_estoque.db";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_QUANTITY = "quantity";
    public static final String COLUMN_COST_PRICE = "cost_price";
    public static final String COLUMN_SALE_PRICE = "sale_price";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_ACTIVE = "active";
    public static final String COLUMN_MINIMUM_STOCK = "minimum_stock";
    public static final String TABLE_MOVEMENTS = "stock_movements";
    public static final String COLUMN_MOVEMENT_ID = "id";
    public static final String COLUMN_MOVEMENT_PRODUCT_ID = "product_id";
    public static final String COLUMN_MOVEMENT_TYPE = "type";
    public static final String COLUMN_MOVEMENT_QUANTITY = "quantity";
    public static final String COLUMN_MOVEMENT_CREATED_AT = "created_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_NAME + " TEXT, "
                + COLUMN_DESCRIPTION + " TEXT, "
                + COLUMN_LOCATION + " TEXT, "
                + COLUMN_QUANTITY + " INTEGER, "
                + COLUMN_COST_PRICE + " REAL, "
                + COLUMN_SALE_PRICE + " REAL, "
                + COLUMN_IMAGE_PATH + " TEXT, "
                + COLUMN_ACTIVE + " INTEGER NOT NULL DEFAULT 1, "
                + COLUMN_MINIMUM_STOCK + " INTEGER NOT NULL DEFAULT 0"
                + ")";
        db.execSQL(CREATE_PRODUCTS_TABLE);
            createMovementsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COLUMN_LOCATION + " TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COLUMN_ACTIVE + " INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN " + COLUMN_MINIMUM_STOCK + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            createMovementsTable(db);
        }
    }

    private void createMovementsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_MOVEMENTS + " ("
                + COLUMN_MOVEMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_MOVEMENT_PRODUCT_ID + " INTEGER NOT NULL, "
                + COLUMN_MOVEMENT_TYPE + " TEXT NOT NULL, "
                + COLUMN_MOVEMENT_QUANTITY + " INTEGER NOT NULL, "
                + COLUMN_MOVEMENT_CREATED_AT + " INTEGER NOT NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_movements_product ON "
                + TABLE_MOVEMENTS + "(" + COLUMN_MOVEMENT_PRODUCT_ID + ")");
    }

    public long insertProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_LOCATION, product.getLocation());
        values.put(COLUMN_QUANTITY, product.getQuantity());
        values.put(COLUMN_COST_PRICE, product.getCostPrice());
        values.put(COLUMN_SALE_PRICE, product.getSalePrice());
        values.put(COLUMN_IMAGE_PATH, product.getImagePath());
        values.put(COLUMN_ACTIVE, product.isActive() ? 1 : 0);
        values.put(COLUMN_MINIMUM_STOCK, product.getMinimumStock());

        long id = db.insert(TABLE_PRODUCTS, null, values);
        if (id != -1 && product.getQuantity() > 0) {
            addMovement(db, id, "IN", product.getQuantity());
        }
        db.close();
        return id;
    }

    public int updateProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, product.getName());
        values.put(COLUMN_DESCRIPTION, product.getDescription());
        values.put(COLUMN_LOCATION, product.getLocation());
        values.put(COLUMN_QUANTITY, product.getQuantity());
        values.put(COLUMN_COST_PRICE, product.getCostPrice());
        values.put(COLUMN_SALE_PRICE, product.getSalePrice());
        values.put(COLUMN_IMAGE_PATH, product.getImagePath());
        values.put(COLUMN_ACTIVE, product.isActive() ? 1 : 0);
        values.put(COLUMN_MINIMUM_STOCK, product.getMinimumStock());

        int previousQuantity = getQuantity(db, product.getId());

        int result = db.update(TABLE_PRODUCTS, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(product.getId())});
        int difference = product.getQuantity() - previousQuantity;
        if (result > 0 && difference != 0) {
            addMovement(db, product.getId(), difference > 0 ? "IN" : "OUT", Math.abs(difference));
        }
        db.close();
        return result;
    }

    private int getQuantity(SQLiteDatabase db, long productId) {
        Cursor cursor = db.query(TABLE_PRODUCTS, new String[]{COLUMN_QUANTITY},
                COLUMN_ID + " = ?", new String[]{String.valueOf(productId)},
                null, null, null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    private void addMovement(SQLiteDatabase db, long productId, String type, int quantity) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MOVEMENT_PRODUCT_ID, productId);
        values.put(COLUMN_MOVEMENT_TYPE, type);
        values.put(COLUMN_MOVEMENT_QUANTITY, quantity);
        values.put(COLUMN_MOVEMENT_CREATED_AT, System.currentTimeMillis());
        db.insert(TABLE_MOVEMENTS, null, values);
    }

    public void deleteProduct(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRODUCTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public List<Product> getAllProducts() {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY " + COLUMN_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                Product product = new Product();
                product.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                product.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                product.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
                product.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
                product.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)));
                product.setCostPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_COST_PRICE)));
                product.setSalePrice(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SALE_PRICE)));
                product.setImagePath(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)));
                product.setActive(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACTIVE)) == 1);
                product.setMinimumStock(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINIMUM_STOCK)));
                products.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return products;
    }

    public List<Movement> getRecentMovements(int limit) {
        List<Movement> movements = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT p." + COLUMN_NAME + ", m." + COLUMN_MOVEMENT_TYPE + ", m."
                + COLUMN_MOVEMENT_QUANTITY + ", m." + COLUMN_MOVEMENT_CREATED_AT
                + " FROM " + TABLE_MOVEMENTS + " m INNER JOIN " + TABLE_PRODUCTS + " p ON p."
                + COLUMN_ID + " = m." + COLUMN_MOVEMENT_PRODUCT_ID
                + " ORDER BY m." + COLUMN_MOVEMENT_CREATED_AT + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
        try {
            while (cursor.moveToNext()) {
                movements.add(new Movement(cursor.getString(0), cursor.getString(1),
                        cursor.getInt(2), cursor.getLong(3)));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return movements;
    }

    public List<String> getMostMovedProducts(int limit) {
        List<String> products = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT p." + COLUMN_NAME + ", SUM(m." + COLUMN_MOVEMENT_QUANTITY + ") total"
                + " FROM " + TABLE_MOVEMENTS + " m INNER JOIN " + TABLE_PRODUCTS + " p ON p."
                + COLUMN_ID + " = m." + COLUMN_MOVEMENT_PRODUCT_ID
                + " GROUP BY m." + COLUMN_MOVEMENT_PRODUCT_ID
                + " ORDER BY total DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit)});
        try {
            while (cursor.moveToNext()) {
                products.add(cursor.getString(0) + " (" + cursor.getInt(1) + ")");
            }
        } finally {
            cursor.close();
            db.close();
        }
        return products;
    }

    public List<Movement> getMovementsSince(long sinceMillis, int limit) {
        List<Movement> movements = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT p." + COLUMN_NAME + ", m." + COLUMN_MOVEMENT_TYPE + ", m."
                + COLUMN_MOVEMENT_QUANTITY + ", m." + COLUMN_MOVEMENT_CREATED_AT
                + " FROM " + TABLE_MOVEMENTS + " m INNER JOIN " + TABLE_PRODUCTS + " p ON p."
                + COLUMN_ID + " = m." + COLUMN_MOVEMENT_PRODUCT_ID
                + " WHERE m." + COLUMN_MOVEMENT_CREATED_AT + " >= ? ORDER BY m."
                + COLUMN_MOVEMENT_CREATED_AT + " DESC LIMIT ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(sinceMillis), String.valueOf(limit)});
        try {
            while (cursor.moveToNext()) {
                movements.add(new Movement(cursor.getString(0), cursor.getString(1),
                        cursor.getInt(2), cursor.getLong(3)));
            }
        } finally {
            cursor.close();
            db.close();
        }
        return movements;
    }
}
