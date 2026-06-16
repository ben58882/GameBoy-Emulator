public class Catridge implements Memory{
    /* 
    private int[] romBank;
    private int[] externalRam = new int[0x2000];
    */
    
    private MemoryBankController mbc;

    public Catridge(String name, byte[] rom){
        byte mbcType = rom[0x0147];
        System.out.println(mbcType);
        switch(mbcType){
            case 0x00:
            case 0x09:
                this.mbc = new MemoryBankControllerZero(name, rom);
                break;
            case 0x01:
            case 0x02:
            case 0x03:
                this.mbc = new MemoryBankControllerOne(name, rom);
                break;
            case 0x05:
            case 0x06:
                this.mbc = new MemoryBankControllerTwo(name, rom);
                break;
            case 0x0F:
            case 0x10:
            case 0x11:
            case 0x12:
            case 0x13:
                this.mbc = new MemoryBankControllerThree(name, rom);
                break;
        }
    }

    public void saveRam(){
        this.mbc.saveRam();
    }

    @Override
    public void write(int address, int value){
        this.mbc.write(address, value);
    }

    @Override
    public int read(int address){
        return this.mbc.read(address);
    }
}