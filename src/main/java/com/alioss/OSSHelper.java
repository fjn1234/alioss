package com.alioss;

import android.content.Context;
import android.os.StrictMode;

import com.alibaba.sdk.android.oss.OSSService;
import com.alibaba.sdk.android.oss.OSSServiceProvider;
import com.alibaba.sdk.android.oss.callback.GetFileCallback;
import com.alibaba.sdk.android.oss.callback.SaveCallback;
import com.alibaba.sdk.android.oss.model.AccessControlList;
import com.alibaba.sdk.android.oss.model.AuthenticationType;
import com.alibaba.sdk.android.oss.model.ClientConfiguration;
import com.alibaba.sdk.android.oss.model.TokenGenerator;
import com.alibaba.sdk.android.oss.storage.OSSBucket;
import com.alibaba.sdk.android.oss.storage.OSSFile;
import com.alibaba.sdk.android.oss.util.OSSToolKit;

import java.io.File;
import java.io.FileNotFoundException;

public class OSSHelper {

    public static OSSService ossService = OSSServiceProvider.getService();
    private OSSService mOssService;
    private OSSBucket mBucket;
    private String direction = "";
    private static boolean isInited = false;

    public static OSSHelper build(String bucket) {
        return new OSSHelper(bucket);
    }

    public static OSSHelper build(String bucket, String direction) {
        return new OSSHelper(bucket, direction);
    }

    private OSSHelper(String bucket) {
        mOssService = OSSServiceProvider.getService();
        mBucket = mOssService.getOssBucket(bucket);
    }

    private OSSHelper(String bucket, String direction) {
        mOssService = OSSServiceProvider.getService();
        mBucket = mOssService.getOssBucket(bucket);
        this.direction = direction;
    }

    // 上传
    public void upload(File file, SaveCallback savecallback) {
        OSSFile bigfFile = mOssService.getOssFile(mBucket, direction + file.getName());
        try {
            bigfFile.setUploadFilePath(file.getAbsolutePath(), "raw/binary");
            bigfFile.ResumableUploadInBackground(savecallback);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    // 下载
    public void download(File file, GetFileCallback getFileCallback) {
        OSSFile bigFile = mOssService.getOssFile(mBucket, file.getName());
        bigFile.ResumableDownloadToInBackground(file.getAbsolutePath(), getFileCallback);
//        bigFile.downloadToInBackground(file.getAbsolutePath(), getFileCallback);
    }

    public static void init(Context context, String accessKey, String screctKey, String hostId) {
        init(context, accessKey, screctKey, hostId, 0);
    }

    public static void init(Context context, final String accessKey, final String screctKey, String hostId, long currentTimeMillis) {
        if (isInited()) return;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // 初始化设置
        ossService.setApplicationContext(context);
        ossService.setGlobalDefaultHostId(hostId); // 设置region host 即 endpoint
        ossService.setGlobalDefaultACL(AccessControlList.PRIVATE); // 默认为private
        ossService.setAuthenticationType(AuthenticationType.ORIGIN_AKSK); // 设置加签类型为原始AK/SK加签
        ossService.setGlobalDefaultTokenGenerator(new TokenGenerator() { // 设置全局默认加签器
            @Override
            public String generateToken(String httpMethod, String md5, String type, String date,
                                        String ossHeaders, String resource) {

                String content = httpMethod + "\n" + md5 + "\n" + type + "\n" + date + "\n" + ossHeaders
                        + resource;
                return OSSToolKit.generateToken(accessKey, screctKey, content);
            }
        });
        // 由于OSS的token校验是时间相关的，您可能会担心因为手机终端系统时间不准导致无法访问OSS服务。
        // 我们准备了接口让您进行SDK的时间设置，您可以通过网络从业务服务器拿取当前的epoch时间进行设置，然后SDK操作时的时间都将与服务器的时间同步：
        if (currentTimeMillis > 0)
            ossService.setCustomStandardTimeWithEpochSec(currentTimeMillis / 1000);

        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectTimeout(15 * 1000); // 设置全局网络连接超时时间，默认30s
        conf.setSocketTimeout(15 * 1000); // 设置全局socket超时时间，默认30s
        conf.setMaxConnections(50); // 设置全局最大并发网络链接数, 默认50
        ossService.setClientConfiguration(conf);
        isInited = true;
    }

    public static boolean isInited() {
        return isInited;
    }
}
