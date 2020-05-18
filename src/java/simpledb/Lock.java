package simpledb;

import com.sun.org.apache.bcel.internal.generic.ALOAD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lock {
    private Map<PageId, List<TransactionId>> rLock; // read lock
    private Map<PageId, TransactionId> wLock;   // write lock

    public Lock() {
        rLock = new HashMap<>();
        wLock = new HashMap<>();
    }

    /**
     * 锁查找
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     * @param perm - 请求
     * @return boolean - 可以进行perm请求返回true，否则false
     */
    public synchronized boolean grantLock(TransactionId tid, PageId pid, Permissions perm) {
        if (rLock == null)
            rLock = new HashMap<>();
        if (wLock == null)
            wLock = new HashMap<>();

        // init
        List<TransactionId> permList = rLock.get(pid);
        if (permList == null) permList = new ArrayList<>();
        TransactionId admin = wLock.get(pid);

        // hit
        if (tid.equals(admin) || (permList != null && permList.contains(tid))) return true;
        if (perm == Permissions.READ_ONLY) {
            // read only
            if (admin != null) return false;
            if (!permList.contains(tid)) permList.add(tid);
            rLock.put(pid, permList);
            return true;
        }
        else {
            // write
            if (admin != null || permList.size() > 1) return false;
            if (permList.size() == 1 && !permList.contains(tid)) return false;
            wLock.put(pid, tid);
            if (!permList.contains(tid)) permList.add(tid);
            rLock.put(pid, permList);
            return true;
        }
    }

    /**
     * 解锁
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     */
    public synchronized void unLock(TransactionId tid, PageId pid) {
        if (rLock == null)
            rLock = new HashMap<>();
        List<TransactionId> permList = rLock.get(pid);
        TransactionId admin = wLock.get(pid);
        if (permList != null) {
            permList.remove(tid);
            rLock.put(pid, permList);
        }
        if (tid.equals(admin))
            wLock.remove(pid);
    }
}
