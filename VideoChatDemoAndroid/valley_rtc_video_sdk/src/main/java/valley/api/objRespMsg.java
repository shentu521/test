package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */

public class objRespMsg {
    protected com.rtc.client.object_msg m_msg = null;

    public String getFromUserID(){return m_msg.getFromUserID();}
    public String getToUserID(){return m_msg.getToUserID();}
    public String getMessage(){return m_msg.getData();}
    public String getToken(){return m_msg.getToken();}
    public int getMsgType(){return m_msg.getMsgType();}
    public long getMsgTime(){return m_msg.getTime();}
}
