package simpledb;

import java.util.*;

/**
 * Knows how to compute some aggregate over a set of IntFields.
 */
public class IntegerAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;

    private int gbNum;
    private Type gbType;
    private int aNum;
    private Op op;

    private Map<Field, Integer> groups;   // (groupVal, aggregateVal)
    private Map<Field, Integer> cnt;  // count the tuples number of each field

    private List<Tuple> tuples;

    public class IntIterator implements OpIterator {
        private TupleDesc td;
        private Iterator<Tuple> it;

        public IntIterator() {
            Type[] typeAr;
            if (tuples == null)
                tuples = new ArrayList<>();
            Set<Field> keySet = groups.keySet();
            Iterator<Field> it = keySet.iterator();
            for (int i = 0; i < groups.size(); i++) {
                Field key = it.next();
                TupleDesc td;
                Tuple t;
                if (key == null) {
                    typeAr = new Type[] {Type.INT_TYPE};
                    td = new TupleDesc(typeAr);
                    t = new Tuple(td);
                    t.setField(0, new IntField(groups.get(key) / cnt.get(key)));
                }
                else {
                    typeAr = new Type[] {gbType, Type.INT_TYPE};
                    td = new TupleDesc(typeAr);
                    t = new Tuple(td);
                    t.setField(0, key);
                    t.setField(1, new IntField(groups.get(key) / cnt.get(key)));
                }
                tuples.add(t);
            }
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            it = tuples.iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            return it.hasNext();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            return it.next();
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            open();
        }

        @Override
        public TupleDesc getTupleDesc() {
            return td;
        }

        @Override
        public void close() {
            it = null;
        }
    }
    private IntIterator intIT;

    /**
     * Aggregate constructor
     * 
     * @param gbfield
     *            the 0-based index of the group-by field in the tuple, or
     *            NO_GROUPING if there is no grouping
     * @param gbfieldtype
     *            the type of the group by field (e.g., Type.INT_TYPE), or null
     *            if there is no grouping
     * @param afield
     *            the 0-based index of the aggregate field in the tuple
     * @param what
     *            the aggregation operator
     */

    public IntegerAggregator(int gbfield, Type gbfieldtype, int afield, Op what) {
        // some code goes here
        gbNum = gbfield;
        gbType = gbfieldtype;
        if (gbType == null) gbNum = -1;
        aNum = afield;
        op = what;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the
     * constructor
     * 
     * @param tup
     *            the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if (groups == null)
            groups = new HashMap<>();
        if (cnt == null)
            cnt = new HashMap<>();
        Field key = gbNum == -1 ? null : tup.getField(gbNum);
        if (op == Op.AVG) {
            int val = ((IntField)tup.getField(aNum)).getValue(), len = 1;
            if (cnt.containsKey(key)) {
                len = cnt.get(key);
                val += groups.get(key);
                len++;
            }
            groups.put(key, val);
            cnt.put(key, len);
            return;
        }
        if (op == Op.COUNT) {
            int val = 1;
            if (cnt.containsKey(key))
                val += groups.get(key);
            groups.put(key, val);
            cnt.put(key, 1);
            return;
        }
        if (op == Op.MAX) {
            int val = ((IntField)tup.getField(aNum)).getValue();
            if (groups.containsKey(key))
                val = Math.max(val, groups.get(key));
            groups.put(key, val);
            cnt.put(key, 1);
            return;
        }
        if (op == Op.MIN) {
            int val = ((IntField)tup.getField(aNum)).getValue();
            if (groups.containsKey(key))
                val = Math.min(val, groups.get(key));
            groups.put(key, val);
            cnt.put(key, 1);
            return;
        }
        if (op == Op.SUM) {
            int val = ((IntField)tup.getField(aNum)).getValue();
            if (groups.containsKey(key))
                val += groups.get(key);
            groups.put(key, val);
            cnt.put(key, 1);
            return;
        }
    }

    /**
     * Create a OpIterator over group aggregate results.
     * 
     * @return a OpIterator whose tuples are the pair (groupVal, aggregateVal)
     *         if using group, or a single (aggregateVal) if no grouping. The
     *         aggregateVal is determined by the type of aggregate specified in
     *         the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        intIT = new IntIterator();
        return intIT;
    }

}
