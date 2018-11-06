package valley.api;

/**
 * Created by shawn on 2018/10/12.
 */

public class objUserList {
    protected  com.rtc.client.object_user_sheet list = new com.rtc.client.object_user_sheet();

    public int size()
    {
        return list.size();
    }
    public objUser item(int i)
    {
        int nSize = size();
        if(nSize>0 && i>=0 && i<nSize)
            return new objUser(list.item(i));
        return null;
    }
}
