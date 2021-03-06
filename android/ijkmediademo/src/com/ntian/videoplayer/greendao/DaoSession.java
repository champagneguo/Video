package com.ntian.videoplayer.greendao;

import java.util.Map;

import android.database.sqlite.SQLiteDatabase;
import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig videoScanRecordsDaoConfig;

    private final VideoScanRecordsDao videoScanRecordsDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        videoScanRecordsDaoConfig = daoConfigMap.get(VideoScanRecordsDao.class).clone();
        videoScanRecordsDaoConfig.initIdentityScope(type);

        videoScanRecordsDao = new VideoScanRecordsDao(videoScanRecordsDaoConfig, this);

        registerDao(VideoScanRecords.class, videoScanRecordsDao);
    }
    
    public void clear() {
        videoScanRecordsDaoConfig.getIdentityScope().clear();
    }

    public VideoScanRecordsDao getVideoScanRecordsDao() {
        return videoScanRecordsDao;
    }

}
