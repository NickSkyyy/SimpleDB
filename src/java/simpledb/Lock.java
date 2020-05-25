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
            // 写锁不空且不为tid
            if (admin != null && !tid.equals(admin))
            {
                Pair<TransactionId, PageId> tp = new Pair<>(admin, pid);
                if (!waitList.contains(tp)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
            // 写锁为空或者拥有tid的写锁
            if (!permList.contains(tid))
                permList.add(tid);
            rLock.put(pid, permList);
        }
        else {
            // write
            // 拥有tid的写锁
            if (tid.equals(admin)) return true;
            // 写锁不为空且不是tid的写锁
            if (admin != null) {
                Pair<TransactionId, PageId> tp = new Pair<>(admin, pid);
                if (!waitList.contains(tp)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
            // 写锁为空
            // 拥有超过1个读锁
            if (permList.size() > 1) {
                for (int i = 0; i < permList.size(); i++) {
                    TransactionId pTid = permList.get(i);
                    Pair<TransactionId, PageId> tp = new Pair<>(pTid, pid);
                    if (!waitList.contains(tp) && !tid.equals(pTid)) waitList.add(tp);
                }
                wait.put(tid, waitList);
                return false;
            }
            // 只有1个读锁，且不是tid的读锁
            if (permList.size() == 1 && !permList.contains(tid)) {
                TransactionId pTid = permList.get(0);
                Pair<TransactionId, PageId> tp = new Pair<>(pTid, pid);
                if (!waitList.contains(tp) && !tid.equals(pTid)) waitList.add(tp);
                wait.put(tid, waitList);
                return false;
            }
            wLock.put(pid, tid);
        }
        // unlock waitList
        for (Pair<TransactionId, PageId> tp : waitList) {
            if (pid.equals(tp.getValue()))
                waitList.remove(tp);
        }
        return true;
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
            if (permList.isEmpty())
                rLock.remove(pid);
            else
                rLock.put(pid, permList);
        }
        if (tid.equals(admin))
            wLock.remove(pid);
        // update wait info
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
     * 检查死锁（递归）
     *
     * @param tid - 对应的事务
     * @param mark - 查环状态数组
     *               -1 正在访问dfs树下子树
     *         null / 0 未访问
     *                1 dfs树下子树全访问
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
            //System.out.println("Now trace with Pair<" + tid.toString() + ", " + nextTid.toString() + ">");
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
     * 检查死锁（入口）
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     * @param mark - 如上
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
            //System.out.println(tid.toString() + " wait " + nextTid.toString() + " on " + nextPid);
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
     * 查看一个页是否可以被清除
     *
     * @param pid - 对应的页
     * @return boolean - 如果没有事务占用返回true，否则false
     */
    public boolean isEmpty(PageId pid) {
        List<TransactionId> permList = rLock.get(pid);
        TransactionId admin = wLock.get(pid);
        if ((permList == null || permList.isEmpty()) && (admin == null)) return true;
        return false;
    }

    /**
     * 删除所有锁
     *
     * @param tid - 对应的事务
     */
    public void deleteLocks(TransactionId tid) {
        // find
        List<PageId> permList = new ArrayList<>();
        Set<PageId> key = rLock.keySet();
        permList.addAll(key);
        // unlock
        for (int i = 0; i < permList.size(); i++)
            unLock(tid, permList.get(i));
        // update waitList
        Set<TransactionId> key2 = wait.keySet();
        Iterator<TransactionId> it2 = key2.iterator();
        while (it2.hasNext()) {
            TransactionId pTid = it2.next();
            // 经过abort，tid不需要再等待任何事务的完成
            if (tid.equals(pTid)) {
                it2.remove();
                continue;
            }
            // 查找其余的tid是否在等待该事务的完成
            List<Pair<TransactionId, PageId>> waitList = wait.get(pTid);
            if (waitList == null || waitList.isEmpty()) {
                it2.remove();
                continue;
            }
            Iterator<Pair<TransactionId, PageId>> it3 = waitList.iterator();
            while (it3.hasNext()) {
                if (tid.equals(it3.next().getKey()))
                    it3.remove();
            }
            if (waitList.isEmpty())
                it2.remove();
            else
                wait.put(pTid, waitList);
        }
    }

    /**
     * 撤销对锁的依赖
     *
     * @param tid - 对应的事务
     * @param pid - 对应的页
     */
    public synchronized void unWait(TransactionId tid, PageId pid) {
        List<Pair<TransactionId, PageId>> waitList = wait.get(tid);
        for (Pair<TransactionId, PageId> tp : waitList)
            if (pid.equals(tp.getValue()))
                waitList.remove(tp);
    }
}
