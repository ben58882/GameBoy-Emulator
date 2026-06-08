public class Catridge implements Memory{
    private int[] romBank = new int[0x8000];
    private int[] externalRam = new int[0x2000];

    public Catridge(byte[] rom){
        for(int i = 0; i < rom.length; i++){
            romBank[i] = rom[i] & 0xFF;
        }
    }

    @Override
    public void write(int address, int value){
        if(address >= 0xA000 && address <= 0xBFFF){
            externalRam[address - 0xA000] = value;
        }
    }

    @Override
    public int read(int address){
        if(address <= 0x7FFF){
            return romBank[address];
        }
        else{
            return externalRam[address - 0xA000];
        }
    }

}