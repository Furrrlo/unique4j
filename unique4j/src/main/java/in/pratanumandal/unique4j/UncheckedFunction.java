package in.pratanumandal.unique4j;

@FunctionalInterface
public interface UncheckedFunction<T, R> {

    R applyUnchecked(T t) throws Exception;
}
