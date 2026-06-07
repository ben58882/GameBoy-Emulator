public interface Memory {

    public default void write(int address, int value){
        return;
    }

    public default int read(int address){
        return 0;
    }

}