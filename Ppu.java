public class Ppu implements Memory{

    private int[] VRAM = new int[0x2000]; //0x8000 - 0x9FFF
    private int[] OAM = new int[0xA0]; //0xFE00 - 0xFE9F
    private static int vramAdjust = 0x8000;
    private static int oamAdjust = 0xFE00;
    private boolean ppuPower = false;

    /*
    * Game Boy PPU Registers (0xFF40 - 0xFF4B)
    * To access via array: index = address - 0xFF40
    *
    * [0]  0xFF40 : LCDC - LCD Control
    * [1]  0xFF41 : STAT - LCD Status (Mixed Read/Write)
    * [2]  0xFF42 : SCY  - Scroll Y
    * [3]  0xFF43 : SCX  - Scroll X
    * [4]  0xFF44 : LY   - LCD Y-Coordinate (Read-Only)
    * [5]  0xFF45 : LYC  - LY Compare
    * [6]  0xFF46 : DMA  - OAM DMA Transfer Trigger
    * [7]  0xFF47 : BGP  - Background Palette
    * [8]  0xFF48 : OBP0 - Object Palette 0
    * [9]  0xFF49 : OBP1 - Object Palette 1
    * [10] 0xFF4A : WY   - Window Y
    * [11] 0xFF4B : WX   - Window X
    */

    private int[] register = new int[12];

    public GameBoyDisplay display;
    private Gameboy gb;
    private long cycleCounter = 0;

    public Ppu(Gameboy gb){

        this.display = new GameBoyDisplay();
        this.gb = gb;
        /* 
        //THIS IS ONLY CORRECT FOR INITIALISING pc = 0x0100
        this.register[0] = 0x91;
        this.register[1] = 0x82;
        this.register[7] = 0xFC;
        this.register[8] = 0xFF;
        this.register[9] = 0xFF;
        this.register[6] = 0xFF;
        */
    }

    @Override 
    public int read(int address){
        if(address >= 0xFF40 && address <= 0xFF4B){
            return this.register[address - 0xFF40];
        }
        else if(address >= 0x8000 && address <= 0x9FFF){
            return enquireState() == 3 ? 0xFF : this.VRAM[address - 0x8000];
        }
        else{
            return enquireState() == 3 || enquireState() == 2 ? 0xFF : this.OAM[address - 0xFE00];
        }
    }

    @Override
    public void write(int address, int value){
        if(address >= 0xFF40 && address <= 0xFF4B){
            if(address == 0xFF44){
                return;
            }
            else if(address == 0xFF41){
                int tmp = this.register[1] & 0x07;
                tmp |= 0x80;
                int tmp2 = value & 0x78;
                this.register[address - 0xFF40] = (tmp | tmp2);
            }
            else if(address == 0xFF40){
                this.register[0] = value;
                if ((value & 0x80) == 0) {
                    this.register[4] = 0;
                    this.register[1] = (this.register[1] & 0xFC) | 0;
                    this.ppuPower = false;
                }
                else if((value & 0x80) != 0 && !this.ppuPower){
                    this.ppuPower = true;
                }
            }
            else if(address == 0xFF46){
                this.register[6] = value; 
                int sourceAddress = value << 8; 
                for (int i = 0; i < 160; i++) {
                    int data = this.gb.mmu.read(sourceAddress + i); 
                    this.OAM[i] = data;
                }
                this.gb.mmu.dmaLock = true;
                this.gb.mmu.dmaLockCount = 160;
            }
            else{
                this.register[address - 0xFF40] = value;
            }
        }
        else if(address >= 0x8000 && address <= 0x9FFF && enquireState() != 3){
            this.VRAM[address - 0x8000] = value;
        }
        else if(address >= 0xFE00 && address <= 0xFE9F && enquireState() < 2){
            this.OAM[address - 0xFE00] = value;
        }
    }

    public void step(int cycles) {
        if (!this.ppuPower) {return;}

        this.cycleCounter += cycles;
        switch (enquireState()) {
            case 2: // OAM Search
                if (this.cycleCounter >= 20) {
                    this.cycleCounter -= 20;
                    setMode(3);
                }
                break;
                
            case 3: //pixel trf
                if (this.cycleCounter >= 43) {
                    this.cycleCounter -= 43;
                    setMode(0);
                    drawScanline(); 
                }
                break;
                
            case 0: // H-Blank
                if (this.cycleCounter >= 51) {
                    this.cycleCounter -= 51;
                    this.register[4]++;
                    //System.out.println(this.register[4]);
                    updateLYCompare(); 
                    
                    if (this.register[4] == 144) {
                        setMode(1);
                        this.gb.mmu.ifRegister[0] = 1;
                    } else {
                        setMode(2);
                    }
                }
                break;
                
            case 1:  // v blank
                if (this.cycleCounter >= 114) {
                    this.cycleCounter -= 114;
                    this.register[4]++;
                    //System.out.println(this.register[4]);
                    updateLYCompare(); 
                    
                    if (this.register[4] > 153) {
                        this.register[4] = 0;
                        try{
                            Thread.sleep(16);
                        }
                        catch(Exception e){}
                        setMode(2);
                    }
                }
                break;
        }
    }

    private void setMode(int state){
        this.register[1] = (this.register[1] & 0xFC) | state;
        if(state == 0){
            if((this.register[1] & 8) != 0){
                this.gb.mmu.ifRegister[1] = 1;
            }
        }
        else if(state == 1){
            if((this.register[1] & 0x10) != 0){
                this.gb.mmu.ifRegister[1] = 1;
            }
            this.display.renderFrame();
        }
        else{
            if((this.register[1] & 0x20) != 0){
                this.gb.mmu.ifRegister[1] = 1;
            }
        }
    }

    private void updateLYCompare(){
        if(this.register[4] == this.register[5]){
            this.register[1] |= 4;
            if((this.register[1] & 64) != 0){
                this.gb.mmu.ifRegister[1] = 1;
            }
        }
        else{
            this.register[1] &= 0xFB;
        }
    }
    //bug fix bg priority
    //bug fix added hud
    private void drawScanline(){

        int frameArrIdx = (this.register[4] & 0xFF) * 160;
        int baseAddress = (this.register[0] & 0x8) == 0 ? 0x9800 : 0x9C00;
        int tile_data_base = (this.register[0] & 0x10) == 0 ? 0x9000 : 0x8000;
        int[] bgColor = new int[160];

        for(int i = 0; i < 160; i++){
            int y_pos = (this.register[4] + this.register[2]) & 0xFF;
            int x_pos = (i + this.register[3]) & 0xFF;
            int tile_y = y_pos / 8;
            int tile_x = x_pos / 8;
            byte tile_id = (byte)(this.VRAM[baseAddress + (tile_y * 32) + tile_x - vramAdjust]);
            int signed_tile_id = tile_data_base == 0x9000 ? (int)tile_id : (int)tile_id & 0xFF;
            int line_within_tile = y_pos % 8;
            int x_pos_within_tile = x_pos % 8;
            int byte1 = this.VRAM[tile_data_base + (signed_tile_id * 16) + (line_within_tile * 2) - vramAdjust];
            int byte2 = this.VRAM[tile_data_base + (signed_tile_id * 16) + (line_within_tile * 2) + 1 - vramAdjust];
            bgColor[i] = getColorID(byte1, byte2, x_pos_within_tile);
            this.display.pixelBuffer[frameArrIdx] = this.calculateColor(byte1, byte2, x_pos_within_tile, false, false);
            frameArrIdx++;
        }

        // added hud
        boolean windowEnabled = (this.register[0] & 0x20) != 0;
        if(windowEnabled && this.register[4] >= this.register[10]){
            int windowY = this.register[4] - this.register[10];
            int windowBase = (this.register[0] & 0x40) == 0 ? 0x9800 : 0x9C00;
            for(int i = 0; i < 160; i++){
                int wx = this.register[11] - 7;
                if(i < wx) continue;
                int x_pos = i - wx;
                int tile_x = x_pos / 8;
                int tile_y = windowY / 8;
                byte tile_id = (byte)(this.VRAM[windowBase + (tile_y * 32) + tile_x - vramAdjust]);
                int signed_tile_id = tile_data_base == 0x9000 ? (int)tile_id : (int)tile_id & 0xFF;
                int line_within_tile = windowY % 8;
                int x_within = x_pos % 8;
                int b1 = this.VRAM[tile_data_base + (signed_tile_id * 16) + (line_within_tile * 2) - vramAdjust];
                int b2 = this.VRAM[tile_data_base + (signed_tile_id * 16) + (line_within_tile * 2) + 1 - vramAdjust];
                this.display.pixelBuffer[(this.register[4] & 0xFF) * 160 + i] =
                    this.calculateColor(b1, b2, x_within, false, false);
            }
        }
        //^^ added hud
        if((this.register[0] & 2) == 0){
            return;
        }
        int spriteHeight = (this.register[0] & 4) == 0 ? 8 : 16;
        int[] sprites = new int[40];
        int spritesIdx = 0;
        for(int i = 0; i < 0xA0; i = i + 4){
            int spriteTop = this.OAM[i] - 16;
            if(this.register[4] >= spriteTop && this.register[4] < spriteTop + spriteHeight){
                sprites[spritesIdx++] = this.OAM[i];
                sprites[spritesIdx++] = this.OAM[i + 1];
                sprites[spritesIdx++] = this.OAM[i + 2];
                sprites[spritesIdx++] = this.OAM[i + 3];
                if(spritesIdx >= 40){
                    break;
                }
            }
        }

        for(int i = spritesIdx - 4; i >= 0; i -= 4){

            int y = sprites[i] - 16;
            int x = sprites[i + 1] - 8;
            int tile_id = sprites[i + 2];
            int attributes = sprites[i + 3];
            if(spriteHeight == 16 && tile_id % 2 == 1) {tile_id -= 1;}
            int tileAddress = 0x8000 + (tile_id * 16);

            int row = (attributes & 0x40) == 0 ? this.register[4] - y : spriteHeight - 1 - (this.register[4] - y);
            boolean colorPalette = (attributes & 0x10) != 0;
            int byte1 = this.VRAM[tileAddress + (row * 2) - vramAdjust];
            int byte2 = this.VRAM[tileAddress + (row * 2) + 1 - vramAdjust];
            for (int p = 0; p < 8; p++) {
                int pixel_x = x + p;
                if (pixel_x < 0 || pixel_x >= 160) { 
                    continue; 
                }
                int col = (attributes & 0x20) == 0 ? p : 7 - p; 
                int color = this.calculateColor(byte1, byte2, col, true, colorPalette);
                if (color == -1) { 
                    continue; 
                }
                if(bgColor[pixel_x] != 0 && (attributes & 0x80) != 0){
                    continue;
                }
                this.display.pixelBuffer[(this.register[4] & 0xFF) * 160 + pixel_x] = color;
            }
        }
    }

    private int getColorID(int byte1, int byte2, int x){
        return (((byte2 >> (7 - x)) & 1) << 1) | ((byte1 >> (7 - x)) & 1);
    }

    private int calculateColor(int byte1, int byte2, int x, boolean sprite, boolean bit4){
        int colorID = (((byte2 >> (7 - x)) & 1) << 1) | ((byte1 >> (7 - x)) & 1);
        if(sprite && colorID == 0){
            return -1;
        }
        int color = !sprite 
                    ? (this.register[7] >> colorID * 2) & 3
                    : bit4
                    ? (this.register[9] >> colorID * 2) & 3
                    : (this.register[8] >> colorID * 2) & 3;

        switch(color){
            case 0:
                return 0xFFFFFFFF;
            case 1:
                return 0xFFAAAAAA;
            case 2:
                return 0xFF555555;
            case 3:
                return 0xFF000000;
        }
        return -1;
    }
    private int enquireState(){
        return this.register[1] & 0x3;
    }
}