package com.rfidresearchgroup.activities.connect;

import com.rfidresearchgroup.activities.main.SimpleNameReaderActivity;
import com.rfidresearchgroup.activities.main.PadelConfigActivity;
import com.rfidresearchgroup.activities.tools.DeviceConnectActivity;
import com.rfidresearchgroup.callback.ConnectFailedCtxCallback;
import com.rfidresearchgroup.rfidtools.R;

public abstract class AbstractPN53XConnectActivity extends DeviceConnectActivity {

    @Override
    public Class getTarget() {
        // Check if we should go to Padel Config instead
        String targetActivity = getIntent().getStringExtra("TARGET_ACTIVITY");
        if ("PADEL_CONFIG".equals(targetActivity)) {
            return PadelConfigActivity.class;
        }
        return SimpleNameReaderActivity.class;
    }

    @Override
    public String getConnectingMsg() {
        return getString(R.string.msg_connect_common);
    }

    @Override
    public ConnectFailedCtxCallback getCallback() {
        return this;
    }
}

