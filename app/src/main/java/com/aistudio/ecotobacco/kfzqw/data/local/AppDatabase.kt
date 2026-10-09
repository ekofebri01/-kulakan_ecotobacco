package com.aistudio.ecotobacco.kfzqw.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProcurementDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.ProductDao
import com.aistudio.ecotobacco.kfzqw.data.local.daos.TransactionDao
import com.aistudio.ecotobacco.kfzqw.data.local.entities.CategoryEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementItemEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProcurementPlanEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.ProductEntity
import com.aistudio.ecotobacco.kfzqw.data.local.entities.TransactionEntity

@Database(
    entities = [
        ProductEntity::class,
        TransactionEntity::class,
        ProcurementPlanEntity::class,
        ProcurementItemEntity::class,
        CategoryEntity::class
    ],
    version = 10,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun procurementDao(): ProcurementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val migration1to2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `transactions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `productName` TEXT NOT NULL, `quantity` REAL NOT NULL, `unitPrice` REAL NOT NULL, `total` REAL NOT NULL)")
                db.execSQL("INSERT INTO `transactions_new` (`id`, `date`, `productName`, `quantity`, `unitPrice`, `total`) SELECT `id`, `date`, `productName`, CAST(`quantity` AS REAL), `unitPrice`, `total` FROM `transactions`")
                db.execSQL("DROP TABLE `transactions`")
                db.execSQL("ALTER TABLE `transactions_new` RENAME TO `transactions`")
            }
        }
        val migration2to3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0")
            }
        }
        val migration4to5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `supplier` TEXT NOT NULL DEFAULT ''")
            }
        }
        val migration5to6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)")
            }
        }
        val migration6to7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
                db.execSQL("CREATE TABLE IF NOT EXISTS `procurement_plans_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` INTEGER NOT NULL, `supplierName` TEXT NOT NULL)")
                db.execSQL("INSERT INTO `procurement_plans_new` (`id`, `date`, `supplierName`) SELECT `id`, `date`, `supplierName` FROM `procurement_plans`")
                db.execSQL("DROP TABLE `procurement_plans`")
                db.execSQL("ALTER TABLE `procurement_plans_new` RENAME TO `procurement_plans`")
            }
        }
        val migration7to8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
                db.execSQL("ALTER TABLE `procurement_items` ADD COLUMN `unit` TEXT NOT NULL DEFAULT 'kg'")
            }
        }
        val migration8to9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `sellingPrice` REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE `products` ADD COLUMN `sellingUnit` TEXT NOT NULL DEFAULT 'pcs'")
            }
        }
        val migration9to10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `products` ADD COLUMN `contentQuantity` REAL NOT NULL DEFAULT 1.0")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "eco_tobacco_database"
                )
                .addMigrations(migration1to2, migration2to3, migration4to5, migration5to6, migration6to7, migration7to8, migration8to9, migration9to10)
                .fallbackToDestructiveMigration(dropAllTables = false)
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
