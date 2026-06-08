//halt bug NOT implemented yet
public class Gameboy{
    Cpu cpu;
    Mmu mmu;
    Ppu ppu;
    Apu apu;
    InputOutputDevices ioDevices;
    Catridge catridge;
    private long mCycles = 0;
    long tCycleTrackDiv = 0;
    long tCycleTrackTima = 0;

    public Gameboy(Catridge catridge){
        this.cpu = new Cpu();
        this.ppu = new Ppu(this);
        this.apu = new Apu();
        this.ioDevices = new InputOutputDevices();
        this.catridge = catridge;
        this.mmu = new Mmu(cpu, ppu, apu, catridge, ioDevices);
        this.mmu.loadGameBoyClass(this);
        cpu.pc = 0x100; // The standard entry point for all cartridges
        cpu.sp = 0xFFFE;
    }

    public void run(){

        if(checkInterrupt()){
            tick(5);
            return;
        }

        if(this.cpu.isHalted){
            tick(1);
            this.mCycles++;
            for(int i = 0; i <= 4; i++){
                if(this.mmu.ieRegister[i] == 1 && this.mmu.ifRegister[i] == 1){
                    this.cpu.isHalted = false;
                    break;
                }
            }
            return;
        }

        long currMCycle = this.decode();

        this.mCycles += currMCycle;

        this.ppu.step((int)currMCycle);

        tick(currMCycle);
    }


    private boolean checkInterrupt(){
        int jumpVector = 0;
        for(int i = 0; i <= 4; i++){
            if(this.mmu.ieRegister[i] == 1 && this.mmu.ifRegister[i] == 1){
                jumpVector = 0x40 + i * 0x8;
                break;
            }
        }

        if(jumpVector == 0){
            return false;
        }

        if(this.cpu.IME){
            this.cpu.IME = false;
            this.mmu.pushStack(this.cpu.pc);
            this.cpu.pc = jumpVector;
            this.mCycles += 5;
            this.mmu.ifRegister[(jumpVector - 0x40) / 0x8] = 0;
            return true;
        }

        return false;

    }

    private void tick(long mCycleIncrement){

        this.tCycleTrackDiv += mCycleIncrement * 4;
        this.tCycleTrackTima += mCycleIncrement * 4;

        if((this.mmu.TAC & 4 )!= 0){
            int speed = this.mmu.TAC & 0x3;
            int limit;
            if(speed == 0){
                limit = 1024;
            }
            else if(speed == 1){
                limit = 16;
            }
            else if(speed == 2){
                limit = 64;
            }
            else{
                limit = 256;
            }
            while(this.tCycleTrackTima >= limit){
                this.mmu.TIMA++;
                this.tCycleTrackTima -= limit;
                if(this.mmu.TIMA >= 256){
                    this.mmu.ifRegister[2] = 1;
                    this.mmu.TIMA = this.mmu.TMA;
                }
            }
        }

        while(this.tCycleTrackDiv >= 256){
            this.mmu.DIV++;
            this.tCycleTrackDiv -= 256;
            this.mmu.DIV %= 256;
        }

    }

    public int decode(){
        int curr = this.mmu.read(this.cpu.pc);
        if(curr == 0xCB){
            this.cpu.pc++;
            curr = this.mmu.read(this.cpu.pc);
            int firstNibble = curr >> 4;
            int secondNibble = curr & 0xF;
            char reg = cbRegConverter(secondNibble);
            int content = this.mmu.readRegister(reg);
            this.cpu.pc++;
            switch(firstNibble){

                default:
                System.out.printf("FATAL: Unimplemented Opcode 0x%02X at PC: 0x%04X\n", curr, this.cpu.pc - 1);
                System.exit(1);
                return 1;

                case 0x0:
                    if(secondNibble <= 7){
                        //RLC
                        int bit = content >> 7;
                        content = (content << 1) & 0xFF | bit;
                        this.cpu.setCYFlag(bit);
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                    }
                    else{
                        //RRC
                        int bit = content & 1;
                        content = (content >> 1) & 0xFF | (bit << 7);
                        this.cpu.setCYFlag(bit);
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                    }
                    return reg == 'M' ? 4 : 2;
                case 0x1:
                    if(secondNibble <= 7){
                        //RL
                        int bit = content >> 7;
                        content = (content << 1) & 0xFF | this.cpu.getCYFlag();
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                        this.cpu.setCYFlag(bit);
                    }
                    else{
                        //RR
                        int bit = content & 1;
                        content = (content >> 1) | (this.cpu.getCYFlag() << 7);
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                        this.cpu.setCYFlag(bit);
                    }
                    return reg == 'M' ? 4 : 2;
                case 0x2:
                    if(secondNibble <= 7){
                        //SLA
                        int bit = content >> 7;
                        content = (content << 1) & 0xFF;
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setCYFlag(bit);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                    }
                    else{
                        //SRA
                        this.cpu.setCYFlag(content & 1);
                        int bit = content >> 7;
                        content = (content >> 1) | (bit << 7);
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                    }
                    return reg == 'M' ? 4 : 2;
                case 0x3:
                    if(secondNibble <= 7){
                        //SWAP
                        content = ((content & 0xF) << 4) | (content >> 4);
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                        this.cpu.setCYFlag(0);
                    }
                    else{
                        //SRL
                        this.cpu.setCYFlag(content & 1);
                        content = content >> 1;
                        this.mmu.writeRegister(reg, content);
                        this.cpu.setZeroFlag(content == 0 ? 1 : 0);
                        this.cpu.setSubflag(0);
                        this.cpu.setHalfCarryFlag(0);
                    }
                    return reg == 'M' ? 4 : 2;
                case 0x4:
                case 0x5:
                case 0x6:
                case 0x7:
                    //BIT OFFSET, REG
                    int offset = (curr >> 3) & 0x7;
                    int bit = (((content >> offset) & 1) + 1) % 2;
                    this.cpu.setZeroFlag(bit);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(1);
                    return reg == 'M' ? 3 : 2;
                case 0x8:
                case 0x9:
                case 0xA:
                case 0xB:
                    //RES OFFSET, REG
                    offset = (curr >> 3) & 0x7;
                    content = content & (0xFF - (1 << offset));
                    this.mmu.writeRegister(reg, content);
                    return reg == 'M' ? 4 : 2;
                case 0xC:
                case 0xD:
                case 0xE:
                case 0xF:
                    //SET OFFSET, REG
                    offset = (curr >> 3) & 0x7;
                    content = content | (1 << offset);
                    this.mmu.writeRegister(reg, content);
                    return reg == 'M' ? 4 : 2; 
            }
        }
        else{
            int imm = 0;
            int imm2 = 0;
            int bit = 0;
            int lowerByte = 0;
            int higherByte = 0;
            String rp;
            char r;
            int cycle = 0;
            switch(curr){

                default:
                System.out.printf("FATAL: Unimplemented Opcode 0x%02X at PC: 0x%04X\n", curr, this.cpu.pc - 1);
                System.exit(1);
                return 1;

                case 0x00:
                    this.cpu.pc++;
                    return 1;
                case 0x01:
                case 0x11:
                case 0x21:
                case 0x31:
                    if(curr >> 4== 0x0){
                        rp = "BC";
                    }
                    else if(curr >> 4 == 0x1){
                        rp = "DE";
                    }
                    else if(curr >> 4 == 0x2){
                        rp = "HL";
                    }
                    else{
                        rp = "SP";
                    }
                    imm = mmu.read(++this.cpu.pc) | (mmu.read(++this.cpu.pc) << 8);
                    this.cpu.loadRegisterPair(rp, imm);
                    this.cpu.pc++;
                    return 3;
                case 0x02:
                case 0x12:
                    if(curr >> 4 == 0x0){
                        rp = "BC";
                    }
                    else{
                        rp = "DE";
                    }
                    this.mmu.write(this.cpu.getRegisterPair(rp), this.cpu.read('A'));
                    this.cpu.pc++;
                    return 2;
                case 0x03:
                case 0x13:
                case 0x23:
                case 0x33:

                    if(curr >> 4== 0x0){
                        rp = "BC";
                    }
                    else if(curr >> 4 == 0x1){
                        rp = "DE";
                    }
                    else if(curr >> 4 == 0x2){
                        rp = "HL";
                    }
                    else{
                        rp = "SP";
                    }

                    imm = (this.cpu.getRegisterPair(rp) + 1) & 0xFFFF;
                    this.cpu.loadRegisterPair(rp, imm);
                    this.cpu.pc++;
                    return 2;

                case 0x04:
                case 0x14:
                case 0x24:
                case 0x34:

                    cycle = 1;

                    if(curr >> 4== 0x0){
                        r = 'B';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'D';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'H';
                    }
                    else{
                        r = 'M';
                        cycle = 3;
                    }

                    imm = this.mmu.readRegister(r);
                    this.cpu.setHalfCarryFlag((imm & 0xF) + 1 > 0xF ? 1 : 0);
                    imm = (imm + 1) & 0xFF;
                    this.mmu.writeRegister(r, imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.pc++;
                    return cycle;

                case 0x05:
                case 0x15:
                case 0x25:
                case 0x35:

                    cycle = 1;

                    if(curr >> 4== 0x0){
                        r = 'B';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'D';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'H';
                    }
                    else{
                        r = 'M';
                        cycle = 3;
                    }

                    imm = this.mmu.readRegister(r);
                    this.cpu.setHalfCarryFlag((imm & 0xF) == 0 ? 1 : 0);
                    imm = (imm - 1) & 0xFF;
                    this.mmu.writeRegister(r, imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(1);
                    this.cpu.pc++;
                    return cycle;

                case 0x06:
                case 0x16:
                case 0x26:
                case 0x36:

                    cycle = 2;

                    if(curr >> 4== 0x0){
                        r = 'B';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'D';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'H';
                    }
                    else{
                        r = 'M';
                        cycle = 3;
                    }

                    imm = this.mmu.read(++this.cpu.pc);
                    this.mmu.writeRegister(r, imm);
                    this.cpu.pc++;
                    return cycle;

                case 0x07:
                    imm = this.cpu.read('A');
                    bit = imm >> 7;
                    imm = ((imm << 1) | bit) & 0xFF;
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(bit);
                    this.cpu.write('A', imm);
                    this.cpu.pc++;
                    return 1;

                case 0x08:
                    imm = mmu.read(++this.cpu.pc) | (mmu.read(++this.cpu.pc) << 8);
                    lowerByte = this.cpu.sp & 0xFF;
                    higherByte = this.cpu.sp >> 8;
                    mmu.write(imm, lowerByte);
                    mmu.write(imm + 1, higherByte);
                    this.cpu.pc++;
                    return 5;

                case 0x09:
                case 0x19:
                case 0x29:
                case 0x39:

                    if(curr >> 4== 0x0){
                        rp = "BC";
                    }
                    else if(curr >> 4 == 0x1){
                        rp = "DE";
                    }
                    else if(curr >> 4 == 0x2){
                        rp = "HL";
                    }
                    else{
                        rp = "SP";
                    }

                    this.cpu.setSubflag(0);
                    imm = this.cpu.getRegisterPair(rp);
                    imm2 = this.cpu.getRegisterPair("HL");
                    this.cpu.setHalfCarryFlag(((imm & 0xFFF) + (imm2 & 0xFFF)) > 0xFFF ? 1 : 0);
                    this.cpu.setCYFlag(imm + imm2 > 0xFFFF ? 1 : 0);
                    this.cpu.loadRegisterPair("HL", (imm + imm2) & 0xFFFF);
                    this.cpu.pc++;
                    return 2;

                case 0x0A:
                case 0x1A:

                    if(curr >> 4== 0x0){
                        rp = "BC";
                    }
                    else{
                        rp = "DE";
                    }

                    imm = this.mmu.read(this.cpu.getRegisterPair(rp));
                    this.cpu.write('A', imm);
                    this.cpu.pc++;
                    return 2;

                case 0x0B:
                case 0x1B:
                case 0x2B:
                case 0x3B:

                    if(curr >> 4== 0x0){
                        rp = "BC";
                    }
                    else if(curr >> 4 == 0x1){
                        rp = "DE";
                    }
                    else if(curr >> 4 == 0x2){
                        rp = "HL";
                    }
                    else{
                        rp = "SP";
                    }

                    imm = this.cpu.getRegisterPair(rp);
                    this.cpu.loadRegisterPair(rp, (imm - 1) & 0xFFFF);
                    this.cpu.pc++;
                    return 2;

                case 0x0C:
                case 0x1C:
                case 0x2C:
                case 0x3C:

                    if(curr >> 4== 0x0){
                        r = 'C';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'E';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'L';
                    }
                    else{
                        r = 'A';
                    }

                    imm = this.cpu.read(r);
                    this.cpu.setHalfCarryFlag((imm & 0xF) + 1 > 0xF ? 1 : 0);
                    this.cpu.setSubflag(0);
                    imm = (imm + 1) & 0xFF;
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.write(r, imm);
                    this.cpu.pc++;
                    return 1;

                case 0x0D:
                case 0x1D:
                case 0x2D:
                case 0x3D:

                    if(curr >> 4== 0x0){
                        r = 'C';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'E';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'L';
                    }
                    else{
                        r = 'A';
                    }

                    imm = this.cpu.read(r);
                    this.cpu.setHalfCarryFlag((imm & 0xF) == 0 ? 1 : 0);
                    this.cpu.setSubflag(1);
                    imm = (imm - 1) & 0xFF;
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.write(r, imm);
                    this.cpu.pc++;
                    return 1;

                case 0x0E:
                case 0x1E:
                case 0x2E:
                case 0x3E:

                    if(curr >> 4== 0x0){
                        r = 'C';
                    }
                    else if(curr >> 4 == 0x1){
                        r = 'E';
                    }
                    else if(curr >> 4 == 0x2){
                        r = 'L';
                    }
                    else{
                        r = 'A';
                    }

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.write(r, imm);
                    this.cpu.pc++;
                    return 2;

                case 0x0F:
                    imm = this.cpu.read('A');
                    bit = imm & 1;
                    imm = (imm >> 1) | (bit << 7);
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(bit);
                    this.cpu.write('A', imm);
                    this.cpu.pc++;
                    return 1;

                case 0x10:

                    this.cpu.pc++;
                    this.cpu.pc++;
                    this.cpu.isStopped = true;
                    return 1;

                case 0x17:

                    imm = this.mmu.readRegister('A');
                    bit = imm >> 7;
                    imm = ((imm << 1) | this.cpu.getCYFlag()) & 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setCYFlag(bit);
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.pc++;
                    return 1;
       
                case 0x18:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.pc++;
                    this.cpu.pc = (this.cpu.pc + (byte)imm) & 0xFFFF;
                    return 3;
         
                case 0x1F:
                    imm = this.mmu.readRegister('A');
                    bit = imm & 1;
                    imm = (imm >> 1) | (this.cpu.getCYFlag() << 7);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(bit);
                    this.cpu.pc++;
                    return 1;            

                case 0x20:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.pc++;
                    if(this.cpu.getZeroFlag() == 0){
                        this.cpu.pc = (this.cpu.pc + (byte)imm) & 0xFFFF;
                        return 3;
                    }
                    else{
                        return 2;
                    }

                case 0x22:
                case 0x32:

                    int incr = curr == 0x22 ? 1 : -1;
                    this.mmu.writeRegister('M', this.mmu.readRegister('A'));
                    this.cpu.loadRegisterPair("HL", (this.cpu.getRegisterPair("HL") + incr) & 0xFFFF);
                    this.cpu.pc++;
                    return 2;

                case 0x27:
                    int adjust = 0;
                    imm = this.mmu.readRegister('A');
                    int carry = this.cpu.getCYFlag();
                    if(this.cpu.getSubFlag() == 1){
                        if(this.cpu.getHalfCarryFlag() == 1){
                            adjust = adjust | 0x6;
                        }
                        if(this.cpu.getCYFlag() == 1){
                            adjust = adjust | 0x60;
                        }
                        imm -= adjust;
                    }
                    else{
                        if(this.cpu.getHalfCarryFlag() == 1 || (imm & 0x0F) > 0x09){
                            adjust = adjust | 0x6;
                        }
                        if(this.cpu.getCYFlag() == 1 || imm > 0x99){
                            adjust = adjust | 0x60;
                            carry = 1;
                        }
                        imm += adjust;
                    }
                    imm &= 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(carry);
                    this.cpu.pc++;
                    return 1;

                case 0x28:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.pc++;
                    if(this.cpu.getZeroFlag() == 1){
                        this.cpu.pc = (this.cpu.pc + (byte)imm) & 0xFFFF;
                        return 3;
                    }
                    else{
                        return 2;
                    }

                case 0x2A:
                case 0x3A:

                    if(curr == 0x2A){
                        imm = 1;
                    }
                    else{
                        imm = -1;
                    }
                    this.mmu.writeRegister('A', this.mmu.readRegister('M'));
                    this.cpu.loadRegisterPair("HL", this.cpu.getRegisterPair("HL") + imm);
                    this.cpu.pc++;
                    return 2;

                case 0x2F:
                    imm = this.mmu.readRegister('A');
                    imm = (~imm) & 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setSubflag(1);
                    this.cpu.setHalfCarryFlag(1);
                    this.cpu.pc++;
                    return 1;

                case 0x30:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.pc++;
                    if(this.cpu.getCYFlag() == 0){
                        this.cpu.pc = (this.cpu.pc + (byte)imm) & 0xFFFF;
                        return 3;
                    }
                    else{
                        return 2;
                    }

                case 0x37:

                    this.cpu.setCYFlag(1);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.pc++;
                    return 1;

                case 0x38:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.cpu.pc++;
                    if(this.cpu.getCYFlag() == 1){
                        this.cpu.pc = (this.cpu.pc + (byte)imm) & 0xFFFF;
                        return 3;
                    }
                    else{
                        return 2;
                    }
                    
                case 0x3F:
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(this.cpu.getCYFlag() ^ 1);
                    this.cpu.pc++;
                    return 1;

                case 0x40:
                case 0x41:
                case 0x42:
                case 0x43:
                case 0x44:
                case 0x45:
                case 0x46:
                case 0x47:
                case 0x48:
                case 0x49:
                case 0x4A:
                case 0x4B:
                case 0x4C:
                case 0x4D:
                case 0x4E:
                case 0x4F:
                case 0x50:
                case 0x51:
                case 0x52:
                case 0x53:
                case 0x54:
                case 0x55:
                case 0x56:
                case 0x57:
                case 0x58:
                case 0x59:
                case 0x5A:
                case 0x5B:
                case 0x5C:
                case 0x5D:
                case 0x5E:
                case 0x5F:
                case 0x60:
                case 0x61:
                case 0x62:
                case 0x63:
                case 0x64:
                case 0x65:
                case 0x66:
                case 0x67:
                case 0x68:
                case 0x69:
                case 0x6A:
                case 0x6B:
                case 0x6C:
                case 0x6D:
                case 0x6E:
                case 0x6F:
                case 0x70:
                case 0x71:
                case 0x72:
                case 0x73:
                case 0x74:
                case 0x75:
                case 0x77:
                case 0x78:
                case 0x79:
                case 0x7A:
                case 0x7B:
                case 0x7C:
                case 0x7D:
                case 0x7E:
                case 0x7F:

                    char dest = cbRegConverter((curr >> 3) & 0x7);
                    char src = cbRegConverter(curr & 0x7);
                    this.mmu.writeRegister(dest, this.mmu.readRegister(src));
                    this.cpu.pc++;
                    if(dest == 'M' || src == 'M'){
                        return 2;
                    }
                    else{
                        return 1;
                    }
                case 0x76:
                    this.cpu.isHalted = true;
                    this.cpu.pc++;
                    return 1;
            
                case 0x80:
                case 0x81:
                case 0x82:
                case 0x83:
                case 0x84:
                case 0x85:
                case 0x86:
                case 0x87:
                case 0x88:
                case 0x89:
                case 0x8A:
                case 0x8B:
                case 0x8C:
                case 0x8D:
                case 0x8E:
                case 0x8F:

                    bit = this.cpu.getCYFlag();
                    if(curr < 0x88){
                        bit = 0;
                    }
                    r = cbRegConverter(curr & 0x7);
                    imm = this.mmu.readRegister(r);
                    imm2 = this.mmu.readRegister('A');
                    if((imm & 0xF) + (imm2 & 0xF) + bit > 0xF){
                        this.cpu.setHalfCarryFlag(1);
                    }
                    else{
                        this.cpu.setHalfCarryFlag(0);
                    }
                    imm = imm + imm2 + bit;
                    if(imm > 0xFF){
                        this.cpu.setCYFlag(1);
                    }
                    else{
                        this.cpu.setCYFlag(0);
                    }

                    imm &= 0xFF;
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0x90:
                case 0x91:
                case 0x92:
                case 0x93:
                case 0x94:
                case 0x95:
                case 0x96:
                case 0x97:
                case 0x98:
                case 0x99:
                case 0x9A:
                case 0x9B:
                case 0x9C:
                case 0x9D:
                case 0x9E:
                case 0x9F:

                    bit = this.cpu.getCYFlag();
                    if(curr < 0x98){
                        bit = 0;
                    }
                    r = cbRegConverter(curr & 0x7);
                    imm2 = this.mmu.readRegister(r);
                    imm = this.mmu.readRegister('A');
                    if((imm & 0xF) - (imm2 & 0xF) - bit < 0){
                        this.cpu.setHalfCarryFlag(1);
                    }
                    else{
                        this.cpu.setHalfCarryFlag(0);
                    }
                    if(imm - imm2 - bit < 0){
                        this.cpu.setCYFlag(1);
                    }
                    else{
                        this.cpu.setCYFlag(0);
                    }

                    imm = (imm - imm2 - bit) & 0xFF;
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(1);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0xA0:
                case 0xA1:
                case 0xA2:
                case 0xA3:
                case 0xA4:
                case 0xA5:
                case 0xA6:
                case 0xA7:

                    r = cbRegConverter(curr & 0x7);
                    imm = this.mmu.readRegister('A') & this.mmu.readRegister(r);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(1);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0xA8:
                case 0xA9:
                case 0xAA:
                case 0xAB:
                case 0xAC:
                case 0xAD:
                case 0xAE:
                case 0xAF:

                    r = cbRegConverter(curr & 0x7);
                    imm = this.mmu.readRegister('A') ^ this.mmu.readRegister(r);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0xB0:
                case 0xB1:
                case 0xB2:
                case 0xB3:
                case 0xB4:
                case 0xB5:
                case 0xB6:
                case 0xB7:

                    r = cbRegConverter(curr & 0x7);
                    imm = this.mmu.readRegister('A') | this.mmu.readRegister(r);
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0xB8:
                case 0xB9:
                case 0xBA:
                case 0xBB:
                case 0xBC:
                case 0xBD:
                case 0xBE:
                case 0xBF:

                    r = cbRegConverter(curr & 0x7);
                    imm = this.mmu.readRegister('A');
                    imm2 = this.mmu.readRegister(r);
                    this.cpu.setZeroFlag(((imm - imm2) & 0xFF) == 0 ? 1 : 0);
                    this.cpu.setSubflag(1);
                    this.cpu.setHalfCarryFlag((imm & 0xF) < (imm2 & 0xF) ? 1 : 0);
                    this.cpu.setCYFlag(imm < imm2 ? 1 : 0);
                    this.cpu.pc++;
                    return r == 'M' ? 2 : 1;

                case 0xC0:
                case 0xD0:

                    if(curr == 0xC0){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    if(bit == 1){
                        this.cpu.pc++;
                        return 2;
                    }
                    imm = this.mmu.read(this.cpu.sp) | (this.mmu.read(++this.cpu.sp) << 8);
                    this.cpu.sp++;
                    this.cpu.pc = imm;
                    return 5;

                case 0xC1:
                case 0xD1:
                case 0xE1:
                case 0xF1:

                    curr = curr >> 4;
                    if(curr == 0xC){
                        rp = "BC";
                    }
                    else if(curr == 0xD){
                        rp = "DE";
                    }
                    else if(curr == 0xE){
                        rp = "HL";
                    }
                    else{
                        rp = "AF";
                    }
                    imm = this.mmu.read(this.cpu.sp) | (this.mmu.read(++this.cpu.sp) << 8);
                    this.cpu.sp++;
                    this.cpu.loadRegisterPair(rp, imm);
                    this.cpu.pc++;
                    return 3;

                case 0xC2:
                case 0xD2:

                    if(curr == 0xC2){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);

                    if(bit == 1){
                        this.cpu.pc++;
                        return 3;
                    }

                    this.cpu.pc = imm;
                    return 4;

                case 0xC3:

                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    this.cpu.pc = imm;
                    return 4;

                case 0xC4:
                case 0xD4:

                    if(curr == 0xC4){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    this.cpu.pc++;
                    if(bit == 1){
                        return 3;
                    }
                    this.mmu.write(--this.cpu.sp, this.cpu.pc >> 8);
                    this.mmu.write(--this.cpu.sp, this.cpu.pc & 0xFF);
                    this.cpu.pc = imm;
                    return 6;

                case 0xC5:
                case 0xD5:
                case 0xE5:
                case 0xF5:

                    curr = curr >> 4;
                    if(curr == 0xC){
                        rp = "BC";
                    }
                    else if(curr == 0xD){
                        rp = "DE";
                    }
                    else if(curr == 0xE){
                        rp = "HL";
                    }
                    else{
                        rp = "AF";
                    }

                    imm = this.cpu.getRegisterPair(rp);
                    this.mmu.write(--this.cpu.sp, imm >> 8);
                    this.mmu.write(--this.cpu.sp, imm & 0XFF);
                    this.cpu.pc++;
                    return 4;

                case 0xC6:

                    imm = this.mmu.read(++this.cpu.pc);
                    imm2 = this.mmu.readRegister('A');
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag((imm & 0xF) + (imm2 & 0xF) > 0xF ? 1 : 0);
                    imm += imm2;
                    this.cpu.setCYFlag(imm > 0xFF ? 1 : 0);
                    imm &= 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.pc++;
                    return 2;

                case 0xC7:
                case 0xD7:
                case 0xE7:
                case 0xF7:
                case 0xCF:
                case 0xDF:
                case 0xEF:
                case 0xFF:

                    imm = (curr & 0x38);
                    this.cpu.pc++;
                    this.mmu.write(--this.cpu.sp, this.cpu.pc >> 8);
                    this.mmu.write(--this.cpu.sp, this.cpu.pc & 0xFF);
                    this.cpu.pc = imm;
                    return 4;

                case 0xC8:
                case 0xD8:
                    
                    if(curr == 0xC8){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    if(bit == 0){
                        this.cpu.pc++;
                        return 2;
                    }
                    imm = this.mmu.read(this.cpu.sp) | (this.mmu.read(++this.cpu.sp) << 8);
                    this.cpu.sp++;
                    this.cpu.pc = imm;
                    return 5;

                case 0xC9:
                case 0xD9:
                    this.cpu.pc = mmu.popStack();
                    if(curr == 0xD9){
                        this.cpu.IME = true;
                    }
                    return 4;

                case 0xCA:
                case 0xDA:

                    if(curr == 0xCA){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);

                    if(bit == 0){
                        this.cpu.pc++;
                        return 3;
                    }

                    this.cpu.pc = imm;
                    return 4;

                case 0xCC:
                case 0xDC:

                    if(curr == 0xCC){
                        bit = this.cpu.getZeroFlag();
                    }
                    else{
                        bit = this.cpu.getCYFlag();
                    }
                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    this.cpu.pc++;
                    if(bit == 0){
                        return 3;
                    }
                    this.mmu.write(--this.cpu.sp, this.cpu.pc >> 8);
                    this.mmu.write(--this.cpu.sp, this.cpu.pc & 0xFF);
                    this.cpu.pc = imm;
                    return 6;

                case 0xCD:
                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    this.cpu.pc++;
                    this.mmu.pushStack(this.cpu.pc);
                    this.cpu.pc = imm;
                    return 6;

                case 0xCE:
                    imm = this.mmu.read(++this.cpu.pc);
                    imm2 = this.mmu.readRegister('A');
                    bit = this.cpu.getCYFlag();
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag((imm & 0xF) + (imm2 & 0xF) + bit > 0xF ? 1 : 0);
                    imm += imm2 + bit;
                    this.cpu.setCYFlag(imm > 0xFF ? 1 : 0);
                    imm &= 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.pc++;
                    return 2;

                case 0xD6:

                    imm2 = this.mmu.read(++this.cpu.pc);
                    imm = this.mmu.readRegister('A');
                    this.cpu.setSubflag(1);
                    this.cpu.setHalfCarryFlag((imm & 0xF) < (imm2 & 0xF) ? 1 : 0);
                    this.cpu.setCYFlag(imm < imm2 ? 1 : 0);
                    imm -= imm2;
                    imm &= 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.pc++;
                    return 2;

    
                case 0xDE:
                    
                    imm2 = this.mmu.read(++this.cpu.pc);
                    imm = this.mmu.readRegister('A');
                    bit = this.cpu.getCYFlag();
                    this.cpu.setSubflag(1);
                    this.cpu.setHalfCarryFlag((imm & 0xF) - (imm2 & 0xF) - bit < 0 ? 1 : 0);
                    this.cpu.setCYFlag(imm - imm2 - bit < 0? 1 : 0);
                    imm -= imm2;
                    imm -= bit;
                    imm &= 0xFF;
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.pc++;
                    return 2;
    
                case 0xE0:
                    imm = this.mmu.read(++this.cpu.pc);
                    imm2 = this.mmu.readRegister('A');
                    this.mmu.write(0xFF00 | imm, imm2);
                    this.cpu.pc++;
                    return 3;
              
                case 0xE2:
                
                    imm = this.mmu.readRegister('C');
                    imm2 = this.mmu.readRegister('A');
                    this.mmu.write(0xFF00 | imm, imm2);
                    this.cpu.pc++;
                    return 2;
              
                case 0xE6:
                    imm = this.mmu.read(++this.cpu.pc);
                    imm = imm & this.mmu.readRegister('A');
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(1);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return 2;
                /* 
                case 0xE8:
                    //gemini say is wrong need to double cfm
                    imm = this.mmu.read(++this.cpu.pc);
                    imm2 = (byte)imm + this.cpu.sp;
                    this.cpu.setCYFlag(imm2 > 0xFFFF ? 1 : 0);
                    this.cpu.setHalfCarryFlag((byte)imm + (this.cpu.sp & 0xFFF) > 0xFFF ? 1 : 0);
                    this.cpu.sp = imm2 & 0xFFFF;
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.pc++;
                    return 4;
                */
                
                case 0xE8:
                    imm = this.mmu.read(++this.cpu.pc);
                    int signedByte = (byte)imm; // Must be signed!
                    
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    // Half-carry relies on the lower 4 bits
                    this.cpu.setHalfCarryFlag(((this.cpu.sp & 0xF) + (signedByte & 0xF)) > 0xF ? 1 : 0);
                    // Carry relies on the lower 8 bits
                    this.cpu.setCYFlag(((this.cpu.sp & 0xFF) + (signedByte & 0xFF)) > 0xFF ? 1 : 0);
                    
                    this.cpu.sp = (this.cpu.sp + signedByte) & 0xFFFF;
                    this.cpu.pc++;
                    return 4;

                case 0xE9:

                    this.cpu.pc = this.cpu.getRegisterPair("HL");
                    return 1;

                case 0xEA:

                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    imm2 = this.mmu.readRegister('A');
                    this.mmu.write(imm, imm2);
                    this.cpu.pc++;
                    return 4;

                case 0xEE:

                    imm = this.mmu.read(++this.cpu.pc);
                    imm = imm ^ this.mmu.readRegister('A');
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return 2;

                case 0xF0:

                    imm = this.mmu.read(++this.cpu.pc);
                    this.mmu.writeRegister('A', this.mmu.read(0xFF00 | imm));
                    this.cpu.pc++;
                    return 3;
          
                case 0xF2:

                    imm = this.mmu.readRegister('C');
                    this.mmu.writeRegister('A', this.mmu.read(0xFF00 | imm));
                    this.cpu.pc++;
                    return 2;

                case 0xF3:

                    this.cpu.IME = false;
                    this.cpu.pc++;
                    return 1;

                case 0xF6:

                    imm = this.mmu.read(++this.cpu.pc);
                    imm = imm | this.mmu.readRegister('A');
                    this.mmu.writeRegister('A', imm);
                    this.cpu.setZeroFlag(imm == 0 ? 1 : 0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(0);
                    this.cpu.setCYFlag(0);
                    this.cpu.pc++;
                    return 2;
                /* 
                case 0xF8:

                    imm = this.mmu.read(++this.cpu.pc);
                    imm2 = (byte)imm + this.cpu.sp;
                    this.cpu.setCYFlag(imm2 > 0xFFFF ? 1 : 0);
                    this.cpu.setHalfCarryFlag((byte)imm + (this.cpu.sp & 0xFFF) > 0xFFF ? 1 : 0);
                    this.cpu.loadRegisterPair("HL", imm2 & 0xFFFF);
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.pc++;
                    return 3;
                */
                    
                case 0xF8:
                    imm = this.mmu.read(++this.cpu.pc);
                    int offset = (byte)imm; // Must be signed!
                    
                    this.cpu.setZeroFlag(0);
                    this.cpu.setSubflag(0);
                    this.cpu.setHalfCarryFlag(((this.cpu.sp & 0xF) + (offset & 0xF)) > 0xF ? 1 : 0);
                    this.cpu.setCYFlag(((this.cpu.sp & 0xFF) + (offset & 0xFF)) > 0xFF ? 1 : 0);
                    
                    this.cpu.loadRegisterPair("HL", (this.cpu.sp + offset) & 0xFFFF);
                    this.cpu.pc++;
                    return 3;

                case 0xF9:

                    this.cpu.sp = this.cpu.getRegisterPair("HL");
                    this.cpu.pc++;
                    return 2;

                case 0xFA:

                    imm = this.mmu.read(++this.cpu.pc) | (this.mmu.read(++this.cpu.pc) << 8);
                    this.mmu.writeRegister('A', this.mmu.read(imm));
                    this.cpu.pc++;
                    return 4;

                case 0xFB:
                    this.cpu.IME = true;
                    this.cpu.pc++;
                    return 1;

                case 0xFE:
                    imm2 = this.mmu.read(++this.cpu.pc);
                    imm = this.mmu.readRegister('A');
                    this.cpu.setSubflag(1);
                    this.cpu.setZeroFlag(imm - imm2 == 0 ? 1 : 0);
                    this.cpu.setCYFlag(imm - imm2 < 0 ? 1 : 0);
                    this.cpu.setHalfCarryFlag((imm & 0xF) - (imm2 & 0xF) < 0 ? 1 : 0);
                    this.cpu.pc++;
                    return 2;
            }
        }
    }
    private static char[] cbRegIndex = new char[] {'B', 'C', 'D', 'E', 'H', 'L', 'M', 'A'};
    //M is stand in for (HL)

    private static char cbRegConverter(int secondNibble){
        if(secondNibble >= 8){
            secondNibble -= 8;
        }
        return cbRegIndex[secondNibble];
    }
}