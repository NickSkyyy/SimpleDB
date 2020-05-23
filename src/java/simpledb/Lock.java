package simpledb;

import javafx.util.Pair;
import java.util.*;

public class Lock {
    private Map<PageId, List<TransactionId>> rLock; // read lock (PageId key)
    private Map<PageId, TransactionId> wLock;       // write lock (PageId key)
    private Map<TransactionId, List<Pair<TransactionId, PageId>>>  wait; // waitList

    public Lock() {
        rLock = new HashMap<>();
        wLock = new HashMap<>();
        wait = new HashMap<>();
    }

    /**
     * 检查四个数据结构是否为空
     */
    private void checkNull() {
        if (rLock == null)
            rLock = new HashMap<>();
        if (wLock == null)
            wLock = new HashMap<>();
        if (wait == null)
            wait = new HashMap<>();
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
        checkNull();
        // init
        List<TransactionId> permList = rLock.get(pid);
        List<Pair<TransactionId, PageId>> waitList = wait.get(tid);
        if (permList == null) permList = new ArrayList<>();
        if (waitList == null) waitList = new ArrayList<>();
        TransactionId admin = wLock.get(pid);

        if (perm == Permissions.READ_ONLY) {
            // read only
            if (admin != null && !tid.equals(admin))
            {
                Pair<TransactionId, PageId> tp = new Pair<>(admin, pid);
                if (!waitList.contains(tp)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
            if (!permList.contains(tid))
                permList.add(tid);
            rLock.put(pid, permList);
            return true;
        }
        else {
            // write
            if (tid.equals(admin)) return true;
            if (admin != null) {
                Pair<TransactionId, PageId> tp = new Pair<>(admin, pid);
                if (!waitList.contains(tp)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
            if (permList.size() > 1) {
                for (int i = 0; i < permList.size(); i++) {
                    TransactionId pTid = permList.get(i);
                    Pair<TransactionId, PageId> tp = new Pair<>(pTid, pid);
                    if (!waitList.contains(tp) && !tid.equals(pTid)) waitList.add(tp);
                }
                wait.put(tid, waitList);
                return false;
            }
            if (permList.size() == 1 && !permList.contains(tid)) {
                TransactionId pTid = permList.get(0);
                Pair<TransactionId, PageId> tp = new Pair<>(pTid, pid);
                if (!waitList.contains(tp)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
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
        checkNull();

        // init
        List<TransactionId> permList = rLock.get(pid);
        TransactionId admin = wLock.get(pid);
        if (permList != null) {
            permList.remove(tid);
            rLock.put(pid, permList);
        }
        if (tid.equals(admin))
            wLock.remove(pid);
/*
        // update wait list
        Pair<TransactionId, PageId> tp = new Pair<>(tid, pid);
        Set<TransactionId> key = wait.keySet();
        Iterator<TransactionId> it = key.iterator();
        while (it.hasNext()) {
            TransactionId pTid = it.next();
            List<Pair<TransactionId, PageId>> waitList = wait.get(pTid);
            if (waitList == null) continue;
            waitList.remove(tp);
            wait.put(pTid, waitList);
        }
*/
    }

    /**
     * 获得锁信息
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     * @return boolean - 锁存在返回true，否则false
     */
    public synchronized boolean getLock(TransactionId tid, PageId pid) {
        checkNull();

        // init
        List<TransactionId> permList = rLock.get(pid);
        if (permList != null && permList.contains(tid)) return true;
        TransactionId admin = wLock.get(pid);
        if (tid.equals(admin)) return true;
        return false;
    }

    /**
     * 递归子树tid，检查死锁
     *
     * @param tid - 子树事务
     * @param mark - 标记序列（详细如下）
     * @return boolean - 出现死锁返回true，否则false
     */
    public synchronized boolean isDead(TransactionId tid, Map<TransactionId, Integer> mark) {
        List<Pair<TransactionId, PageId>> waitList = wait.get(tid);
        mark.put(tid, -1);
        if (waitList == null) {
            mark.put(tid, 1);
            return false;
        }
        for (int i = 0; i < waitList.size(); i++) {
            Pair<TransactionId, PageId> tp = waitList.get(i);
            TransactionId nextTid = tp.getKey();
            if (!mark.containsKey(nextTid) || mark.get(nextTid) == 0) {
                // 未加入mark序列（未被访问)，在序列且标记为0
                boolean f = isDead(nextTid, mark);
                if (f) return true;
            }
            if (!tid.equals(nextTid) && mark.get(nextTid) == -1) return true;
        }
        mark.put(tid, 1);
        return false;
    }

    /**
     * 检查死锁
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     * @param mark - 查环状态数组
     *               -1 正在访问dfs树下子树
     *         null / 0 未访问
     *                1 dfs树下子树全访问
     * @return boolean - 出现死锁返回true，否则false
     */
    public synchronized boolean isDead(TransactionId tid, PageId pid, Map<TransactionId, Integer> mark) {
        if (mark == null) mark = new HashMap<>();
        mark.put(tid, -1);
        List<Pair<TransactionId, PageId>> waitList = wait.get(tid);
        if (waitList == null) {
            mark.put(tid, 1);
            return false;
        }
        for (int i = 0; i < waitList.size(); i++) {
            Pair<TransactionId, PageId> tp = waitList.get(i);
            TransactionId nextTid = tp.getKey();
            PageId nextPid = tp.getValue();
            if (pid.equals(nextPid)) {
                if (!mark.containsKey(nextTid) || mark.get(nextTid) == 0) {
                    // 未加入mark序列（未被访问)，在序列且标记为0
                    boolean f = isDead(nextTid, mark);
                    if (f) return true;
                }
                if (!tid.equals(nextTid) && mark.get(nextTid) == -1) return true;
            }
        }
        mark.put(tid, 1);
        return false;
    }

    /**
     * 解除等待信息
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     */
    public synchronized void unwait(TransactionId tid, PageId pid) {
        List<Pair<TransactionId, PageId>> waitList = wait.get(tid);
        if (waitList == null) return;
        Iterator<Pair<TransactionId, PageId>> it = waitList.iterator();
        while (it.hasNext()) {
            if (pid.equals(it.next().getValue()))
                it.remove();
        }
    }
}
