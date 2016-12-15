package com.emagroup.sdk;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzonePublish;
import com.tencent.connect.share.QzoneShare;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.open.utils.ThreadManager;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.provider.DocumentsContract.getDocumentId;
import static com.igexin.push.core.g.S;
import static com.igexin.push.core.g.l;
import static com.igexin.push.core.g.m;

/**
 * Created by Administrator on 2016/12/13.
 */

public class QQShareUtils {
    private  Tencent mTencent;
    private Context mContext;
    private static  QQShareUtils mIntance;
    public static int Build_VERSION_KITKAT = 19;
    public static String ACTION_OPEN_DOCUMENT = "android.intent.action.OPEN_DOCUMENT";
    private static final String PATH_DOCUMENT = "document";
    private String ImageUrl="http://img7.doubanio.com/lpic/s3635685.jpg";
    private EmaSDKListener mListener;

    public static QQShareUtils getIntance(Context context){
        if(mIntance==null){
            mIntance=new QQShareUtils(context);
        }
        return  mIntance;
    }

    private QQShareUtils(Context context) {
        this.mContext = context;
        mTencent= Tencent.createInstance(ConfigManager.getInstance(mContext).getQQAppId(),mContext);
    }

    public void setImageUrl(String imageUrl) {
        ImageUrl = imageUrl;
        final Bundle params = new Bundle();
      //  params.putString(QQShare.SHARE_TO_QQ_APP_NAME,appName);
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, ImageUrl);

        ThreadManager.getMainHandler().post(new Runnable() {

            @Override
            public void run() {
                if (null != mTencent) {
                    mTencent.shareToQQ((Activity) mContext, params, emIUiListener);
                }
            }
        });
    }
    public void shareQQFriendImage(EmaSDKListener listener,Bitmap bitmap){
        this.mListener = listener;
       // startPickLocaleImage();
        try {
            saveBitmap(bitmap,mContext);
            final Bundle params = new Bundle();
            //  params.putString(QQShare.SHARE_TO_QQ_APP_NAME,appName);
            params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
            params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, ImageUrl);

            ThreadManager.getMainHandler().post(new Runnable() {

                @Override
                public void run() {
                    if (null != mTencent) {
                        mTencent.shareToQQ((Activity) mContext, params, emIUiListener);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, 0);

    }

    public void shareQQFriendsWebPage(EmaSDKListener listener, String title, String url, String summary,Bitmap bitmap /*String imageUrl*/){
        this.mListener=listener;
        if(TextUtils.isEmpty(title)||TextUtils.isEmpty(summary)||bitmap==null||/*TextUtils.isEmpty(imageUrl)||*/TextUtils.isEmpty(url)){
            Toast.makeText(mContext,"请传入完整参数",Toast.LENGTH_SHORT).show();
            return ;
        }
        try {
            saveBitmap(bitmap,mContext);
        } catch (IOException e) {
            e.printStackTrace();
        }
        final Bundle params = new Bundle();
        params.putString(QQShare.SHARE_TO_QQ_TITLE, title);
        params.putString(QQShare.SHARE_TO_QQ_TARGET_URL, url);
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);
        params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, ImageUrl);
       // params.putString(QQShare.SHARE_TO_QQ_APP_NAME,appName);
        params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
      //  params.putInt(QQShare.SHARE_TO_QQ_EXT_INT, 0);
        ThreadManager.getMainHandler().post(new Runnable() {

            @Override
            public void run() {
                if (null != mTencent) {
                    mTencent.shareToQQ((Activity) mContext, params, emIUiListener);
                }
            }
        });
    }

    public void shareQzoneWebPage(EmaSDKListener listener, String title, String url, String summary,Bitmap bitmap /*String imageUrl*//*ArrayList<String> imageUrls*/){

        this.mListener=listener;
        if(TextUtils.isEmpty(title)||TextUtils.isEmpty(summary)/*||TextUtils.isEmpty(imageUrl)*/||bitmap==null||TextUtils.isEmpty(url)){
            Toast.makeText(mContext,"请传入完整参数",Toast.LENGTH_SHORT).show();
            return ;
        }

        try {
            saveBitmap(bitmap,mContext);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> imageUrls = new ArrayList<String>();
        final Bundle params = new Bundle();
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT);
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, title);
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, url);
        params.putString(QQShare.SHARE_TO_QQ_SUMMARY, summary);
      //  params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imageUrl);
        imageUrls.add(ImageUrl);
       params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
        // QZone分享要在主线程做
        ThreadManager.getMainHandler().post(new Runnable() {

            @Override
            public void run() {
                if (null != mTencent) {
                    mTencent.shareToQzone((Activity) mContext, params, emIUiListener);
                }
            }
        });
    }

    public void shareQzoneText(String summary,EmaSDKListener listener)
    {
        this.mListener=listener;
        if(TextUtils.isEmpty(summary)){
            Toast.makeText(mContext,"请传入完整参数",Toast.LENGTH_SHORT).show();
            return ;
        }
        final Bundle params = new Bundle();
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE, QzonePublish.PUBLISH_TO_QZONE_TYPE_PUBLISHMOOD);
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, summary);
      ArrayList<String> imageUrls = new ArrayList<String>();
        imageUrls.add(ImageUrl);
        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
        // QQ分享要在主线程做
        ThreadManager.getMainHandler().post(new Runnable() {

            @Override
            public void run() {
                if (null != mTencent) {
                    mTencent.publishToQzone((Activity) mContext, params,emIUiListener /*ThirdLoginUtils.getInstance(mActivity).emIUiListener*/);
                }
            }
        });

    }

    private void startPickLocaleImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);

        if (android.os.Build.VERSION.SDK_INT >= Build_VERSION_KITKAT) {
            intent.setAction(ACTION_OPEN_DOCUMENT);
        } else {
            intent.setAction(Intent.ACTION_GET_CONTENT);
        }
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        ((Activity)mContext).startActivityForResult(
                Intent.createChooser(intent, "本地图片"), 0);
    }


   public IUiListener emIUiListener=new IUiListener() {
        public void onComplete(Object o) {
            Log.i(this.getClass().getName(),"QQShareUtils ---"+o.toString());
            if(o==null){
              //  Toast.makeText(mContext,"分享失败",Toast.LENGTH_SHORT).show();
                mListener.onCallBack(BaseResp.ErrCode.ERR_AUTH_DENIED,"QQ share failed");
            }else{
            JSONObject resultJson= (JSONObject)o;
            if(resultJson.optInt("ret")==0){
              //  Toast.makeText(mContext,"分享成功",Toast.LENGTH_SHORT).show();
                mListener.onCallBack(BaseResp.ErrCode.ERR_OK,"QQ share successful");

            }
            }

        }

        @Override
        public void onError(UiError uiError) {
          //  Toast.makeText(mContext,uiError.errorMessage,Toast.LENGTH_SHORT).show();
            mListener.onCallBack(BaseResp.ErrCode.ERR_AUTH_DENIED,"QQ share failed");
        }

        @Override
        public void onCancel() {

           // Toast.makeText(mContext,"取消分享",Toast.LENGTH_SHORT).show();
            mListener.onCallBack(BaseResp.ErrCode.ERR_USER_CANCEL,"QQ share cancle");
        }
    };

    public  void saveBitmap(Bitmap bitmap, Context context) throws IOException {
         SimpleDateFormat simpleDateFormat =new SimpleDateFormat("yyyy-MM-dd hh-mm-ss");
        String fileName=simpleDateFormat.format(new Date());
        File folder = new File("/mnt/sdcard/dcim/Camera/");
        if (!folder.exists()) {
            folder.mkdir();
        }
        File file = new File("/mnt/sdcard/dcim/Camera/" + fileName + ".jpg");
      //  Toast.makeText(context, "保存图片中", Toast.LENGTH_SHORT).show();
        FileOutputStream out;
        if (!file.exists()) {

            try {
                out = new FileOutputStream(file);
                if (bitmap.compress(Bitmap.CompressFormat.PNG, 70, out)) {
                   /* Toast.makeText(context, "成功存入相册",
                            Toast.LENGTH_SHORT).show();*/
                    ImageUrl=file.getAbsolutePath();
                    out.flush();
                    out.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public  String getPath( final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= 19;

        // DocumentProvider
        if (isKitKat && isDocumentUri(mContext, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(mContext, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return getDataColumn(mContext, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(mContext, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    /**
     * Test if the given URI represents a {@link Document} backed by a
     * {@link DocumentsProvider}.
     */
    private static boolean isDocumentUri(Context context, Uri uri) {
        final List<String> paths = uri.getPathSegments();
        if (paths.size() < 2) {
            return false;
        }
        if (!PATH_DOCUMENT.equals(paths.get(0))) {
            return false;
        }

        return true;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
  public  boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context
     *            The context.
     * @param uri
     *            The Uri to query.
     * @param selection
     *            (Optional) Filter used in the query.
     * @param selectionArgs
     *            (Optional) Selection arguments used in the query.
     *            [url=home.php?mod=space&uid=7300]@return[/url] The value of
     *            the _data column, which is typically a file path.
     */
    public  String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

}
