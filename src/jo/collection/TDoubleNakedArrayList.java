package jo.collection;

import java.util.function.Function;

import gnu.trove.list.array.TDoubleArrayList;

public class TDoubleNakedArrayList extends TDoubleArrayList {

    public TDoubleNakedArrayList() {
        super();
    }

    public TDoubleNakedArrayList(int capacity) {
        super(capacity);
    }

    public <R> R executeFunction(Function<double[], R> fn) {
        double[] data = _data;
        return fn.apply(data);
    }
}
