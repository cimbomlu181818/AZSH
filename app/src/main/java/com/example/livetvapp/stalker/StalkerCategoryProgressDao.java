package com.example.livetvapp.stalker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface StalkerCategoryProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(StalkerCategoryProgress progress);

    @Update
    void update(StalkerCategoryProgress progress);

    @Query("SELECT * FROM stalker_category_progress WHERE m3uName = :m3uName AND category = :category LIMIT 1")
    StalkerCategoryProgress getByM3uAndCategory(String m3uName, String category);

    @Query("SELECT * FROM stalker_category_progress WHERE category = :category")
    List<StalkerCategoryProgress> getByCategory(String category);

    @Query("DELETE FROM stalker_category_progress WHERE m3uName = :m3uName")
    void deleteByM3uName(String m3uName);

    // YENİ: Tüm kayıtları getir (yedekleme için)
    @Query("SELECT * FROM stalker_category_progress")
    List<StalkerCategoryProgress> getAll();

    // YENİ: Tüm kayıtları sil (içe aktarma öncesi temizlik için)
    @Query("DELETE FROM stalker_category_progress")
    void deleteAll();
}