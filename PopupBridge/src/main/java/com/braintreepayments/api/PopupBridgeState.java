package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;

public class PopupBridgeState {

    private String status; // IDLE, COMPLETE, ERROR, UNKNOWN
    private boolean isOriginalHostActivity;
    private JSONObject payload;
    private String error;

    PopupBridgeState(String status, boolean isOriginalHostActivity, JSONObject payload, String error) {
        this.status = status;
        this.isOriginalHostActivity = isOriginalHostActivity;
        this.payload = payload;
        this.error = error;
    }

    String toJSONString() {
        try {
            return new JSONObject()
                    .put("status", status)
                    .put("isOriginalHostActivity", isOriginalHostActivity)
                    .putOpt("payload", payload)
                    .putOpt("error", error)
                    .toString();
        } catch (JSONException e) {
        }
        return null;
    }
}
