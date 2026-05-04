package com.invman.common.repository;

import com.invman.common.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {

    Optional<AppSetting> findByKey(String key);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO app_settings (setting_key, setting_value)
            VALUES (:key, :value)
            ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value
            """, nativeQuery = true)
    void upsert(@Param("key") String key, @Param("value") String value);
}
