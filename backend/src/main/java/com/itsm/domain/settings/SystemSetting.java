package com.itsm.domain.settings;

import jakarta.persistence.*;

/** Persisted key/value configuration (e.g. active assignment strategy). */
@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(length = 80)
    private String settingKey;

    @Column(nullable = false, length = 200)
    private String settingValue;

    protected SystemSetting() {}

    public SystemSetting(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }

    public String getKey() { return settingKey; }
    public String getValue() { return settingValue; }
    public void setValue(String value) { this.settingValue = value; }
}
