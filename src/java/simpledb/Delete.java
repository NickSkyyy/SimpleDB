package simpledb;

import javax.lang.model.element.TypeElement;
import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;

    private TransactionId tid;
    private OpIterator it;
    private TupleDesc td;
    private int mark;
    private int cnt;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     * 
     * @param t
     *            The transaction this delete runs in
     * @param child
     *            The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, OpIterator child) {
        // some code goes here
        tid = t;
        it = child;
        td = new TupleDesc(new Type[]{Type.INT_TYPE});
        cnt = 0;
    }

    public TupleDesc getTupleDesc() {
        // some code goes here
        return null;
    }

    public void open() throws DbException, TransactionAbortedException {
        // some code goes here
        mark = 0;
        super.open();
        it.open();
        while (it.hasNext()) {
            Tuple t = it.next();
            try {
                Database.getBufferPool().deleteTuple(tid, t);
            }
            catch (IOException ie) {
                ie.printStackTrace();
            }
            cnt++;
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
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     * 
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
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
