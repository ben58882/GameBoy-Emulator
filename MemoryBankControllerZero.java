public class MemoryBankControllerZero implements MemoryBankController{

    private int[] romBank = new int[0x8000];
    private int[] externalRam = new int[0x2000];

    private boolean ramPresent = false;

    public MemoryBankControllerZero(String name, byte[] data){
        if(data[0x147] == 0x9){
            ramPresent = true;
        }
        for(int i = 0; i < data.length; i++){
            romBank[i] = (int)data[i] & 0xFF;
        }
    }

    @Override
    public void write(int address, int value){
        if(address >= 0xA000 && address <= 0xBFFF && ramPresent){
            this.externalRam[address - 0xA000] = value;
        }
    }

    @Override
    public int read(int address){
        if(address < 0x8000){
            return this.romBank[address];
        }
        else{
            if(!ramPresent){
                return 0xFF;
            }
            else{
                return this.externalRam[address - 0xA000];
            }
        }
    }

    @Override
    public void saveRam(){}
}
