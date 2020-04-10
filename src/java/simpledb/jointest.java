package simpledb;

import java.io.*;

public class jointest {
    public static void main(String[] argv) {
        Type[] types = new Type[]{Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE};
        String[] names = new String[]{"field0", "field1", "field2"};
        TupleDesc td = new TupleDesc(types, names);

        HeapFile table1 = new HeapFile(new File("some_data_file1.dat"), td);
        Database.getCatalog().addTable(table1, "t1");
        HeapFile table2 = new HeapFile(new File("some_data_file2.dat"), td);
        Database.getCatalog().addTable(table2, "t2");

        TransactionId tid = new TransactionId();
        SeqScan ss1 = new SeqScan(tid, table1.getId(), "t1");
        SeqScan ss2 = new SeqScan(tid, table2.getId(), "t2");

        Filter sf1 = new Filter(new Predicate(0, Predicate.Op.GREATER_THAN, new IntField(1)), ss1);
        JoinPredicate p = new JoinPredicate(1, Predicate.Op.EQUALS, 1);
        Join j = new Join(p, sf1, ss2);

        try {
            j.open();
            while (j.hasNext()) {
                Tuple tup = j.next();
                System.out.println(tup);
            }
            j.close();
            Database.getBufferPool().transactionComplete(tid);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
