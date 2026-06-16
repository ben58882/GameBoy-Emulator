import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class MemoryBankControllerThree implements MemoryBankController {

    private String romName;
    private int[] romBank;
    private int romBanksNum;

    private int[] ramBank;
    private int ramBankNum = 0;

    private boolean ramPresent = false;
    private boolean ramEnabled = false;


    private int romBank7Bits = 1; 
    private int ramRtcSelect = 0; 
    
    private boolean rtcPresent = false;
    private int[] rtcRegisters = new int[5];
    private boolean latchWait = false; // used for the 0x00 -> 0x01 RTC latch sequence
    private long lastTimeWeChecked = 0;
    private long haltTimerTime = 0;

    public MemoryBankControllerThree(String romName, byte[] data) {
        this.romName = romName;
        int cartType = (int)data[0x0147] & 0xFF;
        
        // MBC3 Cartridge Types: 
        // 0x0F (MBC3+TIMER+BATTERY), 0x10 (MBC3+TIMER+RAM+BATTERY)
        // 0x11 (MBC3), 0x12 (MBC3+RAM), 0x13 (MBC3+RAM+BATTERY)
        if (cartType >= 0x0F && cartType <= 0x13) {
            
            if (cartType == 0x10 || cartType == 0x12 || cartType == 0x13) {
                ramPresent = true;
                switch ((int)data[0x0149] & 0xFF) {
                    case 0x00:
                        ramPresent = false;
                        break;
                    case 0x01:
                        this.ramBank = new int[0x800]; 
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
            } else {
                ramPresent = false;
            }

            if(cartType == 0x0F || cartType == 0x10){
                this.rtcPresent = true;
            }
        }

        this.romBanksNum = 2 * (1 << ((int)data[0x0148] & 0xFF));
        this.romBank = new int[this.romBanksNum * 0x4000];
        
        for (int i = 0; i < data.length; i++) {
            romBank[i] = (int)data[i] & 0xFF;
        }
        
        if (ramPresent) {
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
                for (int i = 0; i < this.ramBank.length; i++) {
                    this.ramBank[i] = (int)tmp[i] & 0xFF;
                }
                if(tmp.length != this.ramBank.length + 8 + 5){
                    throw(new Exception());
                }
                for(int i = 0; i < 5; i++){
                    this.rtcRegisters[i] = (int)tmp[i + this.ramBank.length] & 0xFF;
                }
                System.out.println("Days " + this.rtcRegisters[3]);
                System.out.println("hours " +this.rtcRegisters[2]);
                System.out.println("minutes " +this.rtcRegisters[1]);
                System.out.println("seconds " +this.rtcRegisters[0]);
                for(int i = 0; i < 8; i++){
                    this.lastTimeWeChecked |= ((long)tmp[this.ramBank.length + 5 + i] & 0xFF) << (i * 8);
                    //fucking wrote  this.lastTimeWeChecked = ((long)tmp[this.ramBank.length + 5 + i] & 0xFF) << (i * 8); intitially
                    //many hours wasted debugging this
                }
                 System.out.println("last check " + this.lastTimeWeChecked);
            } catch (Exception e) {
                System.err.println("Corrupted save file");
                System.exit(1);
            }
        } else {
            //this.lastTimeWeChecked = System.currentTimeMillis();
            System.out.println("No save file found. Initializing empty RAM.");
        }
    }
    
    @Override
    public void saveRam() {
        if (!this.ramPresent) {
            return;
        }
        System.out.println("Days " + this.rtcRegisters[3]);
        System.out.println("hours " +this.rtcRegisters[2]);
        System.out.println("minutes " +this.rtcRegisters[1]);
        System.out.println("seconds " +this.rtcRegisters[0]);
        System.out.println("last check " + this.lastTimeWeChecked);
        String saveFileName = romName + ".sav";
        try {
            FileOutputStream fos = new FileOutputStream(saveFileName);
            byte[] tmp = new byte[this.ramBank.length + 8 + 5];
            for (int i = 0; i < this.ramBank.length; i++) {
                tmp[i] = (byte)this.ramBank[i];
            }
            for(int i = 0; i < 5; i++){
                tmp[i + this.ramBank.length] = (byte)this.rtcRegisters[i];
            }
            for(int i = 0; i < 8; i++){
                tmp[i + this.ramBank.length + 5] = (byte)((this.lastTimeWeChecked >> (i * 8)) & 0xFF);
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
            this.ramEnabled = (value & 0x0F) == 0x0A;
        } 
        else if (address <= 0x3FFF) {
            this.romBank7Bits = value & 0x7F;
            if (this.romBank7Bits == 0) this.romBank7Bits = 1;
        } 
        else if (address <= 0x5FFF) {
            this.ramRtcSelect = value;
        } 
        else if (address <= 0x7FFF) {
            if (value == 0x00) {
                this.latchWait = true;
            } else if (value == 0x01 && this.latchWait) {
                this.latchWait = false;
                //latch here
                long currArbitaryUnits;
                if((this.rtcRegisters[4] & 0x40) == 0){
                    currArbitaryUnits = (System.currentTimeMillis() - this.lastTimeWeChecked) / 1000;
                }
                else{
                    currArbitaryUnits = (this.haltTimerTime - this.lastTimeWeChecked) / 1000;
                }

                currArbitaryUnits =
                    currArbitaryUnits + this.rtcRegisters[0] + this.rtcRegisters[1] * 60 
                        + this.rtcRegisters[2] * 60 * 60 
                            + (this.rtcRegisters[3] | ((this.rtcRegisters[4] & 1) << 8)) * 24 * 60 * 60;

                long seconds = currArbitaryUnits % 60;
                currArbitaryUnits = (currArbitaryUnits - seconds) / 60;
                long minutes = currArbitaryUnits % 60;
                currArbitaryUnits = (currArbitaryUnits - minutes) / 60;
                long hours = currArbitaryUnits % 24;
                currArbitaryUnits = (currArbitaryUnits - hours) / 24;
                long days = currArbitaryUnits;
                this.rtcRegisters[0] = (int)seconds & 0xFF;
                this.rtcRegisters[1] = (int)minutes & 0xFF;
                this.rtcRegisters[2] = (int)hours & 0xFF;
                if(days > 511){
                    this.setRtcDh(7, 1);
                }
                days %= 512;
                this.rtcRegisters[3] = (int)days & 0xFF;
                if(days > 255){
                    this.setRtcDh(0, 1);
                }
                else{
                    this.setRtcDh(0, 0);
                }
                if ((this.rtcRegisters[4] & 0x40) == 0) {
                    this.lastTimeWeChecked = System.currentTimeMillis();
                }
                
            } else {
                this.latchWait = false;
            }
        } 
        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled) {
                if (this.ramRtcSelect <= 0x03 && this.ramPresent && this.ramBankNum > 0) {
                    int currentRamBank = this.ramRtcSelect % this.ramBankNum;
                    int offset = address - 0xA000;
                    this.ramBank[(currentRamBank * 0x2000) + (offset % this.ramBank.length)] = value;
                } 
                else if (this.ramRtcSelect >= 0x08 && this.ramRtcSelect <= 0x0C && this.rtcPresent) {
                    if(this.ramRtcSelect == 0x0C){
                        this.setRtcDh(0, value & 1);
                        this.setRtcDh(6, (value & 0x40) == 0 ? 0 : 1);
                        this.setRtcDh(7, (value & 0x80) == 0 ? 0 : 1);
                    }
                    else{
                        this.rtcRegisters[this.ramRtcSelect - 0x08] = value;
                    }
                }
            }
        }
    }

    @Override
    public int read(int address) {
        if (address <= 0x3FFF) {
            return this.romBank[address];
        } 
        else if (address <= 0x7FFF) {
            int currentRomBank = this.romBank7Bits % this.romBanksNum;
            return this.romBank[(currentRomBank * 0x4000) + (address - 0x4000)];
        } 
        else if (address >= 0xA000 && address <= 0xBFFF) {
            if (this.ramEnabled) {
                if (this.ramRtcSelect <= 0x03 && this.ramPresent && this.ramBankNum > 0) {
                    int currentRamBank = this.ramRtcSelect % this.ramBankNum;
                    int offset = address - 0xA000;
                    return this.ramBank[(currentRamBank * 0x2000) + (offset % this.ramBank.length)];
                } 
                else if (this.ramRtcSelect >= 0x08 && this.ramRtcSelect <= 0x0C && this.rtcPresent) {
                    return this.rtcRegisters[this.ramRtcSelect - 0x08];
                }
            }
            return 0xFF;
        }
        
        return 0xFF; 
    }

    private void setRtcDh(int bitNumber, int val){
        if(bitNumber == 0){
            this.rtcRegisters[4] = (this.rtcRegisters[4] & 0xFE) | val;
        }
        else if(bitNumber == 7){
            this.rtcRegisters[4] = (this.rtcRegisters[4] & 0x7F) | (val << 7);
        }
        else if(bitNumber == 6){
            if(val == 1 && (this.rtcRegisters[4] & 0x40) == 0){
                this.haltTimerTime = System.currentTimeMillis();
            }
            else if(val == 0 && (this.rtcRegisters[4] & 0x40) != 0){
                this.lastTimeWeChecked += (System.currentTimeMillis() - this.haltTimerTime);
            }
            this.rtcRegisters[4] = (this.rtcRegisters[4] & 0xBF) | (val << 6);
        }
    }
}