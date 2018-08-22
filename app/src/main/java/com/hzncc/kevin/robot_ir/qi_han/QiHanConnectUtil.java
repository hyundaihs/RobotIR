package com.hzncc.kevin.robot_ir.qi_han;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import com.google.gson.Gson;
import com.qihancloud.communication.ConnectClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class QiHanConnectUtil {

    private static Gson gson;

    /**
     * @dec 获取通讯录中所有联系人信息，ContactInfo中含有联系人的uid
     * @param context
     * @return
     */
    public static ArrayList<ContactInfo> getContactInfo(Context context) {

        Uri uri = Uri.parse("content://com.qihancloud.contact.Provider/qlink");
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("This is a unKnow Uri " + uri.toString());
        }

        ArrayList<ContactInfo> contactInfoList = new ArrayList<ContactInfo>();
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    ContactInfo contactInfo = new ContactInfo();

                    contactInfo.setUid(cursor.getString(cursor.getColumnIndex("uid")));
                    contactInfo.setUsername(cursor.getString(cursor.getColumnIndex("alias")));
                    contactInfoList.add(contactInfo);
                }
                while (cursor.moveToNext());
            }
        } else {
            return null;
        }
        if (cursor != null) {
            cursor.close();
        }
        return contactInfoList;
    }

    /**@des   向通讯录中所有联系人发送本地照片
     * @param context   上下文
     * @param userList  通讯录中所有联系人
     * @param sharePath 本地的图片地址列表
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int sendPicture2AllContact(Context context, List<ContactInfo> userList, List<String> sharePath) {
        JSONObject body = new JSONObject();
        JSONObject content = new JSONObject();
        ShareResultBean shareResultBean = null;
        try {
            JSONArray array = new JSONArray();
            String result = null;

            for (int j = 0; j < sharePath.size(); j++) {
                array.put(sharePath.get(j));//路径
            }

            for (int i = 0; i < userList.size(); i++) {
                content.put("uid", userList.get(i).getUid());//uid
                content.put("paths", array);
                body.put("cmd", 1001900);//固定不变
                body.put("content", content);
                result = ConnectClient.getInstance(context).transmit(body.toString());
                shareResultBean = (ShareResultBean)jsonToCommand(result, ShareResultBean.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            assert shareResultBean != null;
            return shareResultBean.getResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * @dec   向通讯录中所有联系人发送文本消息
     * @param context   上下文
     * @param userList  联系人列表
     * @param contentString 文本消息
     * @param uid   单个联系人的ID
     * @return
     */
    public static boolean sendStringMessage2AllContact(Context context,List<ContactInfo> userList,String contentString,String uid){//发消息
        String result = null;
        try {
            JSONObject body = new JSONObject();
            JSONObject content = new JSONObject();
            for (int i = 0; i <userList.size() ; i++) {
                content.put("info_type", 0);
                content.put("dst_uid", userList.get(i).getUid());// 发送给谁 uid
                content.put("content", contentString);//内容
                body.put("cmd", 20);//固定不变
                body.put("content", content);
                result = ConnectClient.getInstance(context).transmit(body.toString());
            }

            JSONObject resultJs = new JSONObject(result);
            int resultCode = resultJs.optInt("result");
            int errorCode = resultJs.optInt("error");
            if (resultCode == 1 && errorCode == 0) {
                //成功
                return true;
            }
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @dec   向某个联系人发送图片
     * @param context
     * @param contactInfo   单个联系人的信息
     * @param sharePath     本地图片的地址
     * @return
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static int sendPicture2Contact(Context context, ContactInfo contactInfo, List<String> sharePath) {
        JSONObject body = new JSONObject();
        JSONObject content = new JSONObject();
        ShareResultBean shareResultBean = null;
        try {
            JSONArray array = new JSONArray();
            String result = null;

            for (int j = 0; j < sharePath.size(); j++) {
                array.put(sharePath.get(j));//路径
            }
            content.put("uid", contactInfo.getUid());//uid
            content.put("paths", array);
            body.put("cmd", 1001900);//固定不变
            body.put("content", content);
            result = ConnectClient.getInstance(context).transmit(body.toString());
            shareResultBean = (ShareResultBean)jsonToCommand(result, ShareResultBean.class);

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            assert shareResultBean != null;
            return shareResultBean.getResult();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Object jsonToCommand(String usbCommand, Class clazz) {
        if (gson == null) {
            gson = new Gson();
        }
        return gson.fromJson(usbCommand, clazz);
    }

    /**
     * @dec   向某个联系人发送文本消息
     * @param context
     * @param contentString 文本消息
     * @param uid   联系人的ID
     * @return
     */
    public static boolean sendStringMessage(Context context,String contentString,String uid){//发消息
        try {
            JSONObject body = new JSONObject();
            JSONObject content = new JSONObject();
            content.put("info_type", 0);
            content.put("dst_uid", uid);
            content.put("content", contentString);
            body.put("cmd", 20);
            body.put("content", content);
            String result = ConnectClient.getInstance(context).transmit(body.toString());
            JSONObject resultJs = new JSONObject(result);
            int resultCode = resultJs.optInt("result");
            int errorCode = resultJs.optInt("error");
            if (resultCode == 1 && errorCode == 0) {
                //成功
                return true;
            }
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
}
