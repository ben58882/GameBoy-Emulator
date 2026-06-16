import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
public class MemoryBankControllerOne implements MemoryBankController {

    private String romName;
    private int[] romBank;
    private int romBanksNum;

    private int[] ramBank;
    private int ramBankNum = 0;

    private boolean ramPresent = false;
    private boolean ramEnabled = false;

    private int mode = 0;
    private int romBank5Bits = 1;
    private int ramBank2Bits = 0;

    public MemoryBankControllerOne(String romName, byte[] data) {
        this.romName = romName;
        int cartType = (int)data[0x0147] & 0xFF;
        // 0x02 is MBC1+RAM, 0x03 is MBC1+RAM+BATTERY
        if (cartType == 0x02 || cartType == 0x03) {
            ramPresent = true;
            switch ((int)data[0x0149] & 0xFF) {
                case 0x00:
                    ramPresent = false;
                    break;
                case 0x01:
                    this.ramBank = new int[0x800]; //apparently this doesnt exist
                    this.ramBankNum = 1;
                    break;
                case 0x02:
                    this.ramBank = new int[0x2000]; 
                    this.ramBankNum = 1;
                    break;
                case 0x03:
                    this.ramBank = new int[0x8000]; 
                    this.ramBankNum = 4;
                    break;
            }
        }

        this.romBanksNum = 2 * (1 << ((int)data[0x0148] & 0xFF));
        this.romBank = new int[this.romBanksNum * 0x4000];
        
        for (int i = 0; i < data.length; i++) {
            romBank[i] = (int)data[i] & 0xFF;
        }
        if(ramPresent){
            this.loadRam();
        }
    }

    private void loadRam(){
        String saveFileName = romName + ".sav";
        File saveFile = new File(saveFileName);
        if (saveFile.exists() && saveFile.isFile()) {
            System.out.println("Save file found! Loading data...");
            try {
                byte[] tmp = FileParser.parse(saveFileName);
                for(int i = 0; i < tmp.length; i++){
                    this.ramBank[i] = (int)tmp[i] & 0xFF;
                }
            } catch (Exception e) {
                System.err.println("Corrupted save file");
                System.exit(1);
            }
        }
        else{
            System.out.println("No save file found. Initializing empty RAM.");
        }
    }
    
    @Override
    public void saveRam(){
        if(!this.ramPresent){
            return;
        }
        String saveFileName = romName + ".sav";
        try {
            FileOutputStream fos = new FileOutputStream(saveFileName);
            byte[] tmp = new byte[this.ramBank.length];
            for(int i = 0; i < this.ramBank.length; i++){
                tmp[i] = (byte)this.ramBank[i];
            }
            fos.write(tmp); 
        
            fos.close();
            System.out.println("Save successful!");
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(int address, int value) {
        if (address <= 0x1FFF) {
            this.ramEnabled = (value & 0xF) == 0xA;
        } 
        else if (address <= 0x3FFF) {
            this.romBank5Bits = value & 0x1F;
            if (this.romBank5Bits == 0) this.romBank5Bits = 1;
        } 
        else if (address <= 0x5FFF) {
            this.ramBank2Bits = value & 0x3;
        } 
        else if (address <= 0x7FFF) {
            this.mode = value & 1;
        } 
        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled && this.ramPresent && this.ramBankNum > 0) {
                int currentRamBank = (this.mode == 1) ? this.ramBank2Bits : 0;
                currentRamBank = currentRamBank % this.ramBankNum;
                
                int offset = address - 0xA000;
                
                int arraySize = this.ramBank.length;
                this.ramBank[(currentRamBank * 0x2000) + (offset % arraySize)] = value;
            }
        }
    }

    @Override
    public int read(int address) {
        if (address <= 0x3FFF) {
            int currentRomBank = (this.mode == 1) ? (this.ramBank2Bits << 5) : 0;
            currentRomBank = currentRomBank % this.romBanksNum;
            return this.romBank[(currentRomBank * 0x4000) + address];
        } 

        else if (address <= 0x7FFF) {
            int currentRomBank = this.romBank5Bits;
            currentRomBank |= (this.ramBank2Bits << 5);
            currentRomBank = currentRomBank % this.romBanksNum;
            return this.romBank[(currentRomBank * 0x4000) + (address - 0x4000)];
        } 

        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled && this.ramPresent && this.ramBankNum > 0) {
                int currentRamBank = (this.mode == 1) ? this.ramBank2Bits : 0;
                currentRamBank = currentRamBank % this.ramBankNum;
                
                int offset = address - 0xA000;
                int arraySize = this.ramBank.length;
                
                return this.ramBank[(currentRamBank * 0x2000) + (offset % arraySize)];
            }
            return 0xFF;
        }
        
        return 0xFF; 
    }
}