package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 * 
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private File f;
    private TupleDesc td;
    private int maxPage;

    private class HeapFileIterator implements DbFileIterator {
        private TransactionId tid;
        private int curPage;
        private Iterator<Tuple> it;

        public HeapFileIterator(TransactionId tid) {
            this.tid = tid;
        }
        @Override
        public void open() throws DbException, TransactionAbortedException {
            curPage = 0;
            HeapPageId pid = new HeapPageId(getId(), curPage);
            HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
            it = hp.iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            if (it == null)
                return false;
            if (it.hasNext())
                return true;
            if (curPage + 1 < f.length() / BufferPool.getPageSize()) {
                curPage++;
                HeapPageId pid = new HeapPageId(getId(), curPage);
                HeapPage hp = (HeapPage)Database.getBufferPool().getPage(tid, pid, Permissions.READ_ONLY);
                it = hp.iterator();
                return it.hasNext();
            }
            else
                return false;
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            if (!hasNext())
                throw new NoSuchElementException();
            return it.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            open();
        }

        @Override
        public void close() {
            curPage = 0;
            it = null;
        }
    }

    /**
     * Constructs a heap file backed by the specified file.
     * 
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.f = f;
        this.td = td;
        maxPage = (int)f.length() / BufferPool.getPageSize();
    }

    /**
     * Returns the File backing this HeapFile on disk.
     * 
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     * 
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     * 
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        if (f == null)
            return null;
        Page p = null;
        byte[] data = new byte[BufferPool.getPageSize()];
        try {
            RandomAccessFile raf = new RandomAccessFile(f, "r");
            raf.seek(pid.getPageNumber() * BufferPool.getPageSize());
            raf.read(data, 0, data.length);
            p = new HeapPage((HeapPageId)pid, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return p;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        // some code goes here
        return maxPage;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
        // some code goes here
        return null;
        // not necessary for lab1
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid);
    }
}
