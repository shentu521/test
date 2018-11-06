package valley.api;

/**
 * Created by shawn on 2018/10/17.
 */

public class objRespSetUserAttr {
    protected com.rtc.client.object_user_attr attr = null;
    protected objRespSetUserAttr(com.rtc.client.object_user_attr from){
        attr = from;
    }
    public String  getUserID(){return  attr.getUserID();}
    public String  getAttrName() {return  attr.getAttrName();}
    public String  getAttrValue() {return  attr.getAttrValue();}
}
