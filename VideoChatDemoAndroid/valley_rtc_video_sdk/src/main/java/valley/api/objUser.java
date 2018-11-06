package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */

public class objUser {
    protected  com.rtc.client.object_user user;

    protected objUser(com.rtc.client.object_user usr){
        user = usr;
    }

    public String getUserid() {return user.getUserID();}

    public String attr(String attrName) {return user.getAttr(attrName);}


}
