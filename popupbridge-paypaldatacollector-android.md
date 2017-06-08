This is a code snippet demonstrating the collection of device data for PayPal when using PopupBridge for Android.

# MainActivity.java

```
import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebView;

import com.braintreepayments.popupbridge.PopupBridge;
import com.braintreepayments.popupbridge.PopupBridgeNavigationListener;
import com.paypal.android.sdk.data.collector.SdkRiskComponent;

import java.util.UUID;

public class MainActivity extends Activity implements PopupBridgeNavigationListener {

    private WebView mWebView;
    private PopupBridge mPopupBridge;
    private String mUuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.web_view);

        mUuid = getOrGenerateUuid();
        mPopupBridge = PopupBridge.newInstance(this, mWebView);

        mPopupBridge.setNavigationListener(this);
        mWebView.loadUrl("http://10.0.2.2:3000");
    }

    private String getOrGenerateUuid() {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(this);
        String savedUuid = prefManager.getString("uuid", "");

        if (TextUtils.isEmpty(savedUuid)) {
            savedUuid = UUID.randomUUID().toString();
            prefManager.edit()
                    .putString("uuid", savedUuid)
                    .apply();
        }

        return savedUuid;
    }

    @Override
    public void onUrlOpened(String url) {
        if (url.startsWith("https://checkout.paypal.com")) {
            String id = "";
            Uri uri = Uri.parse(url);

            String checkoutId = uri.getQueryParameter("token");
            String vaultId = uri.getQueryParameter("ba_token");

            if (!TextUtils.isEmpty(checkoutId)) {
                id = checkoutId;
            } else if (!TextUtils.isEmpty(vaultId)) {
                id = vaultId;
            }

            Log.d("result", SdkRiskComponent.getClientMetadataId(this, mUuid, id));
        }
    }
}
```
