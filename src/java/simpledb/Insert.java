package simpledb;

import java.awt.image.DataBuffer;

/**
 * Inserts tuples read from the child operator into the tableId specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private OpIterator it;
    private int id;
    private TupleDesc td;
    private int cnt;
    private int mark;   // check if called more than once

    /**
     * Constructor.
     *
     * @param t
     *            The transaction running the insert.
     * @param child
     *            The child operator from which to read tuples to be inserted.
     * @param tableId
     *            The table in which to insert tuples.
     * @throws DbException
     *             if TupleDesc of child differs from table into which we are to
     *             insert.
     */
    public Insert(TransactionId t, OpIterator child, int tableId)
            throws DbException {
        // some code goes here
        tid = t;
        it = child;
        id = tableId;
        HeapFile hf = (HeapFile)Database.getCatalog().getDatabaseFile(id);
        td = new TupleDesc(new Type[]{Type.INT_TYPE});
        cnt = 0;
        if (!it.getTupleDesc().equals(hf.getTupleDesc()))
            throw new DbException("Fail to match TupleDesc.");
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        mark = 0;
        super.open();
        it.open();
        while (it.hasNext()) {
            Tuple t = it.next();
            try {
                Database.getBufferPool().insertTuple(tid, id, t);
                cnt++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void close() {
        // some code goes here
        it.close();
        super.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        // some code goes here
        mark = 0;
        it.rewind();
    }

    /**
     * Inserts tuples read from child into the tableId specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     *         null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        // some code goes here
        if (mark == 1)
            return null;
        mark = 1;
        Tuple t = new Tuple(td);
        t.setField(0, new IntField(cnt));
        return t;
    }

    @Override
    public OpIterator[] getChildren() {
        // some code goes here
        return new OpIterator[]{it};
    }

    @Override
    public void setChildren(OpIterator[] children) {
        // some code goes here
        it = children[0];
    }
}
