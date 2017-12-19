package com.xiaomi.router.common.application;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMessage;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboShareException;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXImageObject;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXWebpageObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.xiaomi.channel.sdk.MLImgObj;
import com.xiaomi.channel.sdk.MLShareApiFactory;
import com.xiaomi.channel.sdk.MLShareMessage;
import com.xiaomi.channel.sdk.MLShareReq;
import com.xiaomi.channel.sdk.ShareConstants;
import com.xiaomi.channel.sdk.VersionManager;
import com.xiaomi.router.R;
import com.xiaomi.router.common.log.MyLog;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Created by cdingpeng on 4/7/15.
 */
public class SocialShareHelper implements IWeiboHandler.Response {
    public static final String WEIXIN_APP_ID = "wx94c0219186e20123";
    static final String WEIBO_APP_ID = "1497695421";
    private final Context mContext;
    private boolean mWeiboAvailable;

    private IWXAPI mWeixinApi;
    private IWeiboShareAPI mWeiboShareAPI;

    public SocialShareHelper(Context context) {
        mContext = context;

        //init weixin api
        mWeixinApi = WXAPIFactory.createWXAPI(mContext, WEIXIN_APP_ID, true);

        // 创建微博分享接口实例
        mWeiboAvailable = false;
        mWeiboShareAPI = WeiboShareSDK.createWeiboAPI(mContext, WEIBO_APP_ID);
        try {
            if (mWeiboShareAPI.isWeiboAppInstalled()) {
                mWeiboShareAPI.registerApp();
                mWeiboAvailable = true;
            }
        } catch (WeiboShareException e) {

        }
    }

    public void shareToMiliao(String title, String text, Bitmap icon, String url, int target) {
        if (VersionManager.isMiliaoVersionAvailable(mContext)) {
            MLShareApiFactory api = new MLShareApiFactory(mContext);

            String appName = mContext.getResources().getString(R.string.common_app_name);
            api.setPackageNameAndAppName("com.xiaomi.router", appName);

            MLShareMessage message = new MLShareMessage();
            message.title = title;// 标题，标题优先于内容显示
            message.text = text;

            if (icon != null) {
                Bitmap bmp = icon;// 这个是分享本地的图片
                message.imgObj = new MLImgObj(bmp);
                message.imgObj.mImgSize = 2 * 1024 * 1024;//图片期望的最大size
            }

            if (!TextUtils.isEmpty(url)) {
                message.url = url + "&channel=miliao";
            }
            MLShareReq req = new MLShareReq(message, target);// 分享给好友 ,SHARE_TARGET_FEEDS是分享到广播的,SHARE_TARGET_UNION是分享到公会

            api.sendReq(req, false);// 第二个参数表示点击之后是否打开应用
        }
    }

    public void shareToWeixin(int scene, String title, String desc, Bitmap icon, String url) {
        shareToWeixin(scene, title, desc, icon, url, null);
    }

    public void shareToWeixin(int scene, String title, String desc, Bitmap icon, String url, Bitmap image) {
        if (mWeixinApi.isWXAppInstalled()) {
            WXMediaMessage msg = null;

            WXMediaMessage.IMediaObject object = null;
            if (!TextUtils.isEmpty(url)) {
                WXWebpageObject webpage = new WXWebpageObject();
                webpage.webpageUrl = url + "&channel=weixin";

                object = webpage;
            } else if (image != null) {
                object = new WXImageObject(image);
            }

            if (object == null) {
                MyLog.i("Input invalid for weixin share");
                return;
            }

            msg = new WXMediaMessage(object);
            msg.title = title;
            msg.description = desc;
            if (icon != null) {
                Bitmap thumb = icon;//The file size should be within 32KB.
                msg.thumbData = bmpToByteArray(thumb, false);
                MyLog.i("thumb size {} KB", msg.thumbData.length / 1024);
            }

            SendMessageToWX.Req req = new SendMessageToWX.Req();
            req.transaction = String.valueOf(System.currentTimeMillis());
            req.scene = scene;
            req.message = msg;

            mWeixinApi.sendReq(req);
        } else {
            Toast.makeText(mContext, R.string.tool_week_usage_weixin_not_installed, Toast.LENGTH_SHORT).show();
        }

    }

    public void shareToWeibo(String text, Bitmap icon) {
        if (mWeiboAvailable) {

            int supportApi = mWeiboShareAPI.getWeiboAppSupportAPI();
            if (supportApi >= 10351 /*ApiUtils.BUILD_INT_VER_2_2*/) {
                WeiboMultiMessage multiMessage = new WeiboMultiMessage();

                TextObject txt = new TextObject();
                txt.text = text;
                multiMessage.textObject = txt;

                if (icon != null) {
                    ImageObject img = new ImageObject();
                    img.setImageObject(icon);
                    multiMessage.imageObject = img;
                }

                SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
                request.transaction = String.valueOf(System.currentTimeMillis());
                request.multiMessage = multiMessage;
                mWeiboShareAPI.sendRequest(request);
            } else {

                TextObject txt = new TextObject();
                txt.text = text;

                WeiboMessage message = new WeiboMessage();
                message.mediaObject = txt;

                SendMessageToWeiboRequest request = new SendMessageToWeiboRequest();
                request.transaction = String.valueOf(System.currentTimeMillis());
                request.message = message;
                mWeiboShareAPI.sendRequest(request);
            }
        } else {
            Toast.makeText(mContext, R.string.tool_week_usage_weibo_not_installed, Toast.LENGTH_SHORT).show();
        }
    }

    public void shareToQZone(String title, String text, String imageUrl) {
        Intent it = getShareActivityIntent("com.qzonex.module.operation.ui.QZonePublishMoodActivity");
        if (it != null) {
            it.putExtra(Intent.EXTRA_TITLE, title);
            it.putExtra(Intent.EXTRA_TEXT, text);
            it.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl));
            mContext.startActivity(it);
        } else {
            Toast.makeText(mContext, R.string.tool_week_usage_qzone_not_installed, Toast.LENGTH_LONG).show();
            ;
        }
    }

    Intent getShareActivityIntent(String shareClassName) {
        String action = Intent.ACTION_SEND;
        Intent intent = new Intent();
        intent.setAction(action).setType("image/*");
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> resInfo = packageManager.queryIntentActivities(
                intent, 0);
        ActivityInfo activityInfo = null;
        if (!resInfo.isEmpty()) {
            for (ResolveInfo info : resInfo) {
                if (shareClassName.equals(info.activityInfo.name)) {
                    activityInfo = info.activityInfo;
                    break;
                }
            }
        }
        if (activityInfo == null)
            return null;

        Intent targeted = new Intent(action);
        targeted.setType("image/*");
        targeted.setClassName(activityInfo.packageName, activityInfo.name);
        targeted.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return targeted;
    }

    public static byte[] bmpToByteArray(final Bitmap bmp, final boolean needRecycle) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, output);
        if (needRecycle) {
            bmp.recycle();
        }

        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void onResponse(BaseResponse baseResp) {
        switch (baseResp.errCode) {
            case WBConstants.ErrorCode.ERR_OK:
                Toast.makeText(mContext, R.string.tool_week_usage_share_success, Toast.LENGTH_LONG).show();
                break;
            case WBConstants.ErrorCode.ERR_CANCEL:
            case WBConstants.ErrorCode.ERR_FAIL:
                Toast.makeText(mContext, R.string.tool_week_usage_share_cancel, Toast.LENGTH_LONG).show();
                break;
        }
    }

    public static interface WebviewSocialHandler {
        void shareToWeibo();
    }


    public static void clearWxApiLeakedContextReference(Context context) {
        try {
            Class wxApiClass = Class.forName("com.tencent.mm.sdk.openapi.WXApiImplV10");
            Constructor[] constructor = wxApiClass.getDeclaredConstructors();
            constructor[0].setAccessible(true);
            constructor[0].newInstance(null, null, false);

            Class gClass = Class.forName("com.tencent.a.a.a.a.g");
            constructor = gClass.getDeclaredConstructors();
            constructor[0].setAccessible(true);
            Object gInstance = constructor[0].newInstance(context);

            Field[] leakedFields = gClass.getDeclaredFields();
            for (Field field : leakedFields) {
                if (field.getType().getSuperclass() == Object.class) {
                    field.setAccessible(true);
                    field.set(gInstance, (Object)null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
