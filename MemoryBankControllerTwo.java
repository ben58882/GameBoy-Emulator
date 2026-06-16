import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MemoryBankControllerTwo implements MemoryBankController {

    private String romName;
    private int[] romBank;
    private int romBanksNum;

    private int[] ramBank;
    private boolean hasBattery = false;
    private boolean ramEnabled = false;

    private int romBankNumber = 1;

    public MemoryBankControllerTwo(String romName, byte[] data) {
        this.romName = romName;
        
        int cartType = (int)data[0x0147] & 0xFF;
        // 0x05 is MBC2, 0x06 is MBC2+BATTERY
        if (cartType == 0x06) {
            this.hasBattery = true;
        }

        // mbc2 has exactly 512 bytes of built-in RAM.
        this.ramBank = new int[512]; 

        this.romBanksNum = 2 * (1 << ((int)data[0x0148] & 0xFF));
        this.romBank = new int[this.romBanksNum * 0x4000];
        
        for (int i = 0; i < data.length; i++) {
            this.romBank[i] = (int)data[i] & 0xFF;
        }
        
        if (this.hasBattery) {
            this.loadRam();
        }
    }

    private void loadRam() {
        String saveFileName = romName + ".sav";
        File saveFile = new File(saveFileName);
        if (saveFile.exists() && saveFile.isFile()) {
            System.out.println("Save file found! Loading data...");
            try {
                byte[] tmp = FileParser.parse(saveFileName);
                for(int i = 0; i < tmp.length && i < 512; i++){
                    this.ramBank[i] = (int)tmp[i] & 0x0F; 
                }
            } catch (Exception e) {
                System.err.println("Corrupted save file");
                System.exit(1);
            }
        } else {
            System.out.println("No save file found. Initializing empty RAM.");
        }
    }
    
    @Override
    public void saveRam() {
        if (!this.hasBattery) {
            return;
        }
        String saveFileName = romName + ".sav";
        try {
            FileOutputStream fos = new FileOutputStream(saveFileName);
            byte[] tmp = new byte[this.ramBank.length];
            for(int i = 0; i < this.ramBank.length; i++){
                tmp[i] = (byte)(this.ramBank[i] & 0x0F);
            }
            fos.write(tmp); 
        
            fos.close();
            System.out.println("Save successful!");
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void write(int address, int value) {
        if (address <= 0x3FFF) {
            if ((address & 0x0100) == 0) {
                this.ramEnabled = ((value & 0x0F) == 0x0A);
            } else {
                this.romBankNumber = value & 0x0F;
                if (this.romBankNumber == 0) {
                    this.romBankNumber = 1;
                }
            }
        } 
        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled) {
                this.ramBank[address & 0x01FF] = value & 0x0F;
            }
        }
    }

    @Override
    public int read(int address) {
        if (address <= 0x3FFF) {
            return this.romBank[address];
        } 
        else if (address <= 0x7FFF) {
            int currentRomBank = this.romBankNumber % this.romBanksNum;
            return this.romBank[(currentRomBank * 0x4000) + (address - 0x4000)];
        } 
        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled) {
                return this.ramBank[address & 0x01FF] | 0xF0;
            }
            return 0xFF;
        }
        
        return 0xFF; 
    }
}