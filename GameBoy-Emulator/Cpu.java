public class Cpu{
    int pc = 0;
    int sp = 0;
    boolean isStopped = false;
    boolean isHalted = false;
    boolean IME = true;
    private int[] registers = new int[8]; // A, B, C, D, E, H, L
    private int[] flags = new int[8];

    public int read(char register){
        if(register == 'A'){
            return registers[0];
        }
        else if(register == 'B'){
            return registers[1];
        }
        else if(register == 'C'){
            return registers[2];
        }
        else if(register == 'D'){
            return registers[3];
        }
        else if(register == 'E'){
            return registers[4];
        }
        else if(register == 'H'){
            return registers[5];
        }
        else if(register == 'L'){
            return registers[6];
        }
        else{
            throw new RuntimeException("Invalid reg");
        }
    }

    public void write(char register, int val){
        if(register == 'A'){
            registers[0] = val;
        }
        else if(register == 'B'){
            registers[1] = val;
        }
        else if(register == 'C'){
            registers[2] = val;
        }
        else if(register == 'D'){
            registers[3] = val;
        }
        else if(register == 'E'){
            registers[4] = val;
        }
        else if(register == 'H'){
            registers[5] = val;
        }
        else if(register == 'L'){
            registers[6] = val;
        }
        else{
            throw new RuntimeException("Invalid reg");
        }
    }

    public void setCYFlag(int val){
        flags[4] = val;
    }

    public int getCYFlag(){
        return flags[4];
    }

    public void setZeroFlag(int val){
        flags[7] = val;
    }

    public int getZeroFlag(){
        return flags[7];
    }

    public void setSubflag(int val){
        flags[6] = val;
    }

    public int getSubFlag(){
        return flags[6];
    }

    public void setHalfCarryFlag(int val){
        flags[5] = val;
    }

    public int getHalfCarryFlag(){
        return flags[5];
    }

    private void writeToRegisterF(int val){
        for(int i = 0; i < 8; i++){
            if(i <= 3){
                continue;
            }
            this.flags[i] = (val >> i) & 1;
        }
    }

    private int readFromRegisterF(){
    int val = 0;
    for(int i = 7; i >= 0; i--){
        val <<= 1;           
        val |= this.flags[i];
    }
    return val;
}

    public void loadRegisterPair(String s, int val){
        if(s.equals("SP")){
            this.sp = val;
            return;
        }
        if(s.equals("AF")){
            this.write(s.charAt(0), (val >> 8));
            this.writeToRegisterF(val & 0xFF);
            return;
        }
        this.write(s.charAt(1), val & 0xFF);
        this.write(s.charAt(0), (val >> 8));
    }

    public int getRegisterPair(String s){
        if(s.equals("SP")){
            return this.sp;
        }
        if(s.equals("AF")){
            return (this.read(s.charAt(0)) << 8) | this.readFromRegisterF();
        }
        return (this.read(s.charAt(0)) << 8) | (this.read(s.charAt(1)));
    }
}