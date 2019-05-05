package inc.andsoft.asimidimagic.tools;

public class DataWithLabel<T> {
    private String myStr;
    private T myData;

    public DataWithLabel(String str, T data) {
        myStr = str;
        myData = data;
    }

    @Override
    public String toString() {
        return myStr;
    }

    public T getData() {
        return myData;
    }
}
