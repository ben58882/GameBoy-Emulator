//import java.io.FileWriter;
//implemented dma lock

public class Mmu implements Memory{
    //FileWriter writer;
    Cpu cpu;
    Ppu ppu;
    Apu apu;
    Catridge catridge;
    InputOutputDevices inputOutputDevices;
    Gameboy gb;

    int[] WRAM = new int[0x2000];
    int[] HRAM = new int[0x7F];

    int[] ieRegister = new int[] {0, 0, 0, 0, 0, 1, 1, 1};
    int[] ifRegister = new int[] {0, 0, 0, 0, 0, 1, 1, 1};

    int DIV = 0;
    int TIMA = 0;
    int TMA = 0;
    int TAC = 0;

    boolean dmaLock = false;
    int dmaLockCount = 0;

    public Mmu(Cpu cpu, Ppu ppu, Apu apu, Catridge catridge, InputOutputDevices inputOutputDevices){
        this.cpu = cpu;
        this.ppu = ppu;
        this.apu = apu;
        this.catridge = catridge;
        this.inputOutputDevices = inputOutputDevices;
    }

    public void loadGameBoyClass(Gameboy gb){
        this.gb = gb;
    }

    @Override
    public void write(int address, int value){
        if(address == 0xFF50 && value != 0 && this.gb.bootActive){
            this.gb.bootActive = false;
        }
        else if(this.dmaLock){
            if(address >= 0xFF80 && address <= 0xFFFE){
                this.HRAM[address - 0xFF80] = value;
            }
            else if(address >= 0xFF00 && address <= 0xFF7F){
                this.inputOutputDevices.write(address, value);
            }
        }
        else if(address == 0xFF0F){
            writeToInterruptRegs(value, ifRegister);
        }
        else if(address == 0xFFFF){
            writeToInterruptRegs(value, ieRegister);
        }
        else if(address == 0xFF04){
            this.gb.tCycleTrackDiv = 0;
            this.gb.tCycleTrackTima = 0;
            this.DIV = 0;
        }
        else if(address == 0xFF05){
            this.TIMA = value;
        }
        else if(address == 0xFF06){
            this.TMA = value;
        }
        else if(address == 0xFF07){
            this.TAC = value;
        }
        else if(address >= 0xFF40 && address <= 0xFF4B){
            this.ppu.write(address, value);
        }
        else if(address <= 0x7FFF){
            this.catridge.write(address, value);
        }
        else if(address <= 0x9FFF){
            this.ppu.write(address, value);
        }
        else if(address <= 0xBFFF){
            this.catridge.write(address, value);
        }
        else if(address <= 0xDFFF){
            this.WRAM[address - 0xC000] = value;
        }
        else if(address <= 0xFDFF){
            this.WRAM[address - 0xE000] = value;
        }
        else if(address <= 0xFE9F){
            this.ppu.write(address, value);
        }
        else if(address <= 0xFF7F){
            this.inputOutputDevices.write(address, value);
        }
        else if(address <= 0xFFFE){
            this.HRAM[address - 0xFF80] = value;
        }
    }
    
    @Override
    public int read(int address){

        if(address < 0x100 && this.gb.bootActive){
            return this.gb.bootRom[address];
        }
        else if(this.dmaLock){
            if(address >= 0xFF80 && address <= 0xFFFE){
                return this.HRAM[address - 0xFF80];
            }
            else if(address >= 0xFF00 && address <= 0xFF7F){
                return this.inputOutputDevices.read(address);
            }
            else{
                return 0xFF;
            }
        }
        else if(address == 0xFF0F){
            return readFromInterruptRegs(ifRegister);
        }
        else if(address == 0xFFFF){
            return readFromInterruptRegs(ieRegister);
        }
        else if(address == 0xFF04){
            return this.DIV;
        }
        else if(address == 0xFF05){
            return this.TIMA;
        }
        else if(address == 0xFF06){
            return this.TMA;
        }
        else if(address == 0xFF07){
            return this.TAC;
        }
        else if(address >= 0xFF40 && address <= 0xFF4B){
            return this.ppu.read(address);
        }
        else if(address <= 0x7FFF){
            return this.catridge.read(address);
        }
        else if(address <= 0x9FFF){
            return this.ppu.read(address);
        }
        else if(address <= 0xBFFF){
            return this.catridge.read(address);
        }
        else if(address <= 0xDFFF){
            return this.WRAM[address - 0xC000];
        }
        else if(address <= 0xFDFF){
            return this.WRAM[address - 0xE000];
        }
        else if(address <= 0xFE9F){
            return this.ppu.read(address);
        }
        else if(address <= 0xFEFF){
            return (address & 0xF0) | ((address & 0xF0) >> 4);
        }
        else if(address <= 0xFF7F){
            return this.inputOutputDevices.read(address);
        }
        else{
            return this.HRAM[address - 0xFF80];
        }
    }
    public int readRegister(char register){
        if(register == 'M'){
            return this.read(this.cpu.read('H') << 8 | this.cpu.read('L'));
        }
        else{
            return this.cpu.read(register);
        }
    }

    public void writeRegister(char register, int value){
        if(register == 'M'){
            this.write(this.cpu.read('H') << 8 | this.cpu.read('L'), value);
        }
        else{
            this.cpu.write(register, value);
        }
    } 

    public void pushStack(int val){
        this.write(--this.cpu.sp, val >> 8);
        this.write(--this.cpu.sp, val & 0xFF);
    }

    public int popStack(){
        int res = this.read(this.cpu.sp) | (this.read(++this.cpu.sp) << 8);
        this.cpu.sp++;
        return res;
    }
    private static void writeToInterruptRegs(int val, int[] arr){
        for(int i = 0; i <= 4; i++){
            arr[i] = val & 1;
            val >>= 1;
        }
        arr[5] = 1;
        arr[6] = 1;
        arr[7] = 1;
    }

    private static int readFromInterruptRegs(int[] arr){
        int val = 0;
        for(int i = 0; i < 8; i++){
            val = val | (arr[i] << i);
        }
        return val;
    }
}