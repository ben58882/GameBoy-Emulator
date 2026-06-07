import java.io.FileWriter;
import java.io.IOException;

public class Decompiler{
    private static String[] cbRegIndex = new String[] {"B", "C", "D", "E", "H", "L", "(HL)", "A"};
    public static void decompile(byte[] program, String fileName, boolean single){
        try{
            FileWriter writer = new FileWriter(fileName, true);
            int size = single ? 1 : program.length;
            int pc = 0x0100;
            String s = "";
            int imm = 0;
            while(pc < size){
                int curr = (int)program[pc] & 0xFF;
                if(curr == 0xCB){
                    pc++;
                    curr = (int)program[pc] & 0xFF;
                    int firstNibble = curr >> 4;
                    int secondNibble = curr & 0xF;
                    String reg = cbRegConverter(secondNibble);
                    switch(firstNibble){
                        case 0x0:
                            if(secondNibble <= 7){
                                s = "RLC " + reg;
                            }
                            else{
                                s = "RRC " + reg;
                            }
                            break;
                        case 0x1:
                            if(secondNibble <= 7){
                                s = "RL " + reg;
                            }
                            else{
                                s = "RR " + reg;
                            }
                            break;
                        case 0x2:
                            if(secondNibble <= 7){
                                s = "SLA " + reg;
                            }
                            else{
                                s = "SRA " + reg;
                            }
                            break;
                        case 0x3:
                            if(secondNibble <= 7){
                                s = "SWAP " + reg;
                            }
                            else{
                                s = "SRL " + reg;
                            }
                            break;
                        case 0x4:
                            if(secondNibble <= 7){
                                s = "BIT 0, " + reg;
                            }
                            else{
                                s = "BIT 1, " + reg;
                            }
                            break;
                        case 0x5:
                            if(secondNibble <= 7){
                                s = "BIT 2, " + reg;
                            }
                            else{
                                s = "BIT 3, " + reg;
                            }
                            break;
                        case 0x6:
                            if(secondNibble <= 7){
                                s = "BIT 4, " + reg;
                            }
                            else{
                                s = "BIT 5, " + reg;
                            }
                            break;
                        case 0x7:
                            if(secondNibble <= 7){
                                s = "BIT 6, " + reg;
                            }
                            else{
                                s = "BIT 7, " + reg;
                            }
                            break;
                        case 0x8:
                            if(secondNibble <= 7){
                                s = "RES 0, " + reg;
                            }
                            else{
                                s = "RES 1, " + reg;
                            }
                            break;
                        case 0x9:
                            if(secondNibble <= 7){
                                s = "RES 2, " + reg;
                            }
                            else{
                                s = "RES 3, " + reg;
                            }
                            break;
                        case 0xA:
                            if(secondNibble <= 7){
                                s = "RES 4, " + reg;
                            }
                            else{
                                s = "RES 5, " + reg;
                            }
                            break;
                        case 0xB:
                            if(secondNibble <= 7){
                                s = "RES 6, " + reg;
                            }
                            else{
                                s = "RES 7, " + reg;
                            }
                            break;
                        case 0xC:
                            if(secondNibble <= 7){
                                s = "SET 0, " + reg;
                            }
                            else{
                                s = "SET 1, " + reg;
                            }
                            break;
                        case 0xD:
                            if(secondNibble <= 7){
                                s = "SET 2, " + reg;
                            }
                            else{
                                s = "SET 3, " + reg;
                            }
                            break;
                        case 0xE:
                            if(secondNibble <= 7){
                                s = "SET 4, " + reg;
                            }
                            else{
                                s = "SET 5, " + reg;
                            }
                            break;
                        case 0xF:
                            if(secondNibble <= 7){
                                s = "SET 6, " + reg;
                            }
                            else{
                                s = "SET 7, " + reg;
                            }
                            break;  
                    }
                }
                else{
                    switch(curr){
                    case 0x00:
                        s = "NOP";
                        break;
                    case 0x01:
                        imm = ((int)program[++pc] & 0xFF) | (((int)program[++pc] & 0xFF) << 8);
                        s = "LD BC, " + imm;
                        break;
                    case 0x02:
                        s = "LD (BC), A";
                        break;
                    case 0x03:
                        s = "INC BC";
                        break;
                    case 0x04:
                        s = "INC B";
                        break;
                    case 0x05:
                        s = "DEC B";
                        break;
                    case 0x06:
                        imm = (int)program[++pc] & 0xFF;
                        s = "LD B, " + imm;
                        break;
                    case 0x07:
                        s = "RLCA";
                        break;
                    case 0x08:
                        imm = ((int)program[++pc] & 0xFF) | (((int)program[++pc] & 0xFF) << 8);
                        s = "LD (" + imm + "), SP";
                        break;
                    case 0x09:
                        s = "ADD HL, BC";
                        break;
                    case 0x0A:
                        s = "LD A, (BC)";
                        break;
                    case 0x0B:
                        s = "DEC BC";
                        break;
                    case 0x0C:
                        s = "INC C";
                        break;
                    case 0x0D:
                        s = "DEC C";
                        break;
                    case 0x0E:
                        imm = (int)program[++pc] & 0xFF;
                        s = "LD C, " + imm;
                        break;
                    case 0x0F:
                        s = "RRCA";
                        break;
                    case 0x10:
                        s = "STOP";
                        pc++;
                        break;
                    case 0x11:
                        imm = ((int)program[++pc] & 0xFF) | (((int)program[++pc] & 0xFF) << 8);
                        s = "LD DE, " + imm;
                        break;
                    case 0x12:
                        s = "LD (DE), A";
                        break;
                    case 0x13:
                        s = "INC DE";
                        break;
                    case 0x14:
                        s = "INC D";
                        break;
                    case 0x15:
                        s = "DEC D";
                        break;
                    case 0x16:
                        imm = (int)program[++pc] & 0xFF;
                        s = "LD D, " + imm;
                        break;
                    case 0x17:
                        s = "RLA";
                        break;
                    case 0x18:
                        imm = (int)program[++pc] & 0xFF;
                        s = "JR " + imm;
                        break;
                    case 0x19:
                        s = "ADD HL, DE";
                        break;
                    case 0x1A:
                        s = "LD A, (DE)";
                        break;
                    case 0x1B:
                        s = "DEC DE";
                        break;
                    case 0x1C:
                        s = "INC E";
                        break;
                    case 0x1D:
                        s = "DEC E";
                        break;
                    case 0x1E:
                        imm = (int)program[++pc] & 0xFF;
                        s = "LD E, " + imm;
                        break;
                    case 0x1F:
                        s = "RRA";
                        break;
                    case 0x20:
                        imm = program[++pc] & 0xFF;
                        s = "JR NZ, " + imm;
                        break;
                    case 0x21:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "LD HL, " + imm;
                        break;
                    case 0x22:
                        s = "LD (HL+), A";
                        break;
                    case 0x23:
                        s = "INC HL";
                        break;
                    case 0x24:
                        s = "INC H";
                        break;
                    case 0x25:
                        s = "DEC H";
                        break;
                    case 0x26:
                        imm = program[++pc] & 0xFF;
                        s = "LD H, " + imm;
                        break;
                    case 0x27:
                        s = "DAA";
                        break;
                    case 0x28:
                        imm = program[++pc] & 0xFF;
                        s = "JR Z, " + imm;
                        break;
                    case 0x29:
                        s = "ADD HL, HL";
                        break;
                    case 0x2A:
                        s = "LD A, (HL+)";
                        break;
                    case 0x2B:
                        s = "DEC HL";
                        break;
                    case 0x2C:
                        s = "INC L";
                        break;
                    case 0x2D:
                        s = "DEC L";
                        break;
                    case 0x2E:
                        imm = (int)program[++pc] & 0xFF;
                        s = "LD L, " + imm;
                        break;
                    case 0x2F:
                        s = "CPL";
                        break;
                    case 0x30:
                        imm = program[++pc] & 0xFF;
                        s = "JR NC, " + imm;
                        break;
                    case 0x31:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "LD SP, " + imm;
                        break;
                    case 0x32:
                        s = "LD (HL-), A";
                        break;
                    case 0x33:
                        s = "INC SP";
                        break;
                    case 0x34:
                        s = "INC (HL)";
                        break;
                    case 0x35:
                        s = "DEC (HL)";
                        break;
                    case 0x36:
                        imm = program[++pc] & 0xFF;
                        s = "LD (HL), " + imm;
                        break;
                    case 0x37:
                        s = "SCF";
                        break;
                    case 0x38:
                        imm = program[++pc] & 0xFF;
                        s = "JR C, " + imm;
                        break;
                    case 0x39:
                        s = "ADD HL, SP";
                        break;
                    case 0x3A:
                        s = "LD A, (HL-)";
                        break;
                    case 0x3B:
                        s = "DEC SP";
                        break;
                    case 0x3C:
                        s = "INC A";
                        break;
                    case 0x3D:
                        s = "DEC A";
                        break;
                    case 0x3E:
                        imm = program[++pc] & 0xFF;
                        s = "LD A, " + imm;
                        break;
                    case 0x3F:
                        s = "CCF";
                        break;
                    case 0x40:
                        s = "LD B, B";
                        break;
                    case 0x41:
                        s = "LD B, C";
                        break;
                    case 0x42:
                        s = "LD B, D";
                        break;
                    case 0x43:
                        s = "LD B, E";
                        break;
                    case 0x44:
                        s = "LD B, H";
                        break;
                    case 0x45:
                        s = "LD B, L";
                        break;
                    case 0x46:
                        s = "LD B, (HL)";
                        break;
                    case 0x47:
                        s = "LD B, A";
                        break;
                    case 0x48:
                        s = "LD C, B";
                        break;
                    case 0x49:
                        s = "LD C, C";
                        break;
                    case 0x4A:
                        s = "LD C, D";
                        break;
                    case 0x4B:
                        s = "LD C, E";
                        break;
                    case 0x4C:
                        s = "LD C, H";
                        break;
                    case 0x4D:
                        s = "LD C, L";
                        break;
                    case 0x4E:
                        s = "LD C, (HL)";
                        break;
                    case 0x4F:
                        s = "LD C, A";
                        break;
                    case 0x50:
                        s = "LD D, B";
                        break;
                    case 0x51:
                        s = "LD D, C";
                        break;
                    case 0x52:
                        s = "LD D, D";
                        break;
                    case 0x53:
                        s = "LD D, E";
                        break;
                    case 0x54:
                        s = "LD D, H";
                        break;
                    case 0x55:
                        s = "LD D, L";
                        break;
                    case 0x56:
                        s = "LD D, (HL)";
                        break;
                    case 0x57:
                        s = "LD D, A";
                        break;
                    case 0x58:
                        s = "LD E, B";
                        break;
                    case 0x59:
                        s = "LD E, C";
                        break;
                    case 0x5A:
                        s = "LD E, D";
                        break;
                    case 0x5B:
                        s = "LD E, E";
                        break;
                    case 0x5C:
                        s = "LD E, H";
                        break;
                    case 0x5D:
                        s = "LD E, L";
                        break;
                    case 0x5E:
                        s = "LD E, (HL)";
                        break;
                    case 0x5F:
                        s = "LD E, A";
                        break;
                    case 0x60:
                        s = "LD H, B";
                        break;
                    case 0x61:
                        s = "LD H, C";
                        break;
                    case 0x62:
                        s = "LD H, D";
                        break;
                    case 0x63:
                        s = "LD H, E";
                        break;
                    case 0x64:
                        s = "LD H, H";
                        break;
                    case 0x65:
                        s = "LD H, L";
                        break;
                    case 0x66:
                        s = "LD H, (HL)";
                        break;
                    case 0x67:
                        s = "LD H, A";
                        break;
                    case 0x68:
                        s = "LD L, B";
                        break;
                    case 0x69:
                        s = "LD L, C";
                        break;
                    case 0x6A:
                        s = "LD L, D";
                        break;
                    case 0x6B:
                        s = "LD L, E";
                        break;
                    case 0x6C:
                        s = "LD L, H";
                        break;
                    case 0x6D:
                        s = "LD L, L";
                        break;
                    case 0x6E:
                        s = "LD L, (HL)";
                        break;
                    case 0x6F:
                        s = "LD L, A";
                        break;
                    case 0x70:
                        s = "LD (HL), B";
                        break;
                    case 0x71:
                        s = "LD (HL), C";
                        break;
                    case 0x72:
                        s = "LD (HL), D";
                        break;
                    case 0x73:
                        s = "LD (HL), E";
                        break;
                    case 0x74:
                        s = "LD (HL), H";
                        break;
                    case 0x75:
                        s = "LD (HL), L";
                        break;
                    case 0x76:
                        s = "HALT";
                        break;
                    case 0x77:
                        s = "LD (HL), A";
                        break;
                    case 0x78:
                        s = "LD A, B";
                        break;
                    case 0x79:
                        s = "LD A, C";
                        break;
                    case 0x7A:
                        s = "LD A, D";
                        break;
                    case 0x7B:
                        s = "LD A, E";
                        break;
                    case 0x7C:
                        s = "LD A, H";
                        break;
                    case 0x7D:
                        s = "LD A, L";
                        break;
                    case 0x7E:
                        s = "LD A, (HL)";
                        break;
                    case 0x7F:
                        s = "LD A, A";
                        break;
                    case 0x80:
                        s = "ADD A, B";
                        break;
                    case 0x81:
                        s = "ADD A, C";
                        break;
                    case 0x82:
                        s = "ADD A, D";
                        break;
                    case 0x83:
                        s = "ADD A, E";
                        break;
                    case 0x84:
                        s = "ADD A, H";
                        break;
                    case 0x85:
                        s = "ADD A, L";
                        break;
                    case 0x86:
                        s = "ADD A, (HL)";
                        break;
                    case 0x87:
                        s = "ADD A, A";
                        break;
                    case 0x88:
                        s = "ADC A, B";
                        break;
                    case 0x89:
                        s = "ADC A, C";
                        break;
                    case 0x8A:
                        s = "ADC A, D";
                        break;
                    case 0x8B:
                        s = "ADC A, E";
                        break;
                    case 0x8C:
                        s = "ADC A, H";
                        break;
                    case 0x8D:
                        s = "ADC A, L";
                        break;
                    case 0x8E:
                        s = "ADC A, (HL)";
                        break;
                    case 0x8F:
                        s = "ADC A, A";
                        break;
                    case 0x90:
                        s = "SUB B";
                        break;
                    case 0x91:
                        s = "SUB C";
                        break;
                    case 0x92:
                        s = "SUB D";
                        break;
                    case 0x93:
                        s = "SUB E";
                        break;
                    case 0x94:
                        s = "SUB H";
                        break;
                    case 0x95:
                        s = "SUB L";
                        break;
                    case 0x96:
                        s = "SUB (HL)";
                        break;
                    case 0x97:
                        s = "SUB A";
                        break;
                    case 0x98:
                        s = "SBC A, B";
                        break;
                    case 0x99:
                        s = "SBC A, C";
                        break;
                    case 0x9A:
                        s = "SBC A, D";
                        break;
                    case 0x9B:
                        s = "SBC A, E";
                        break;
                    case 0x9C:
                        s = "SBC A, H";
                        break;
                    case 0x9D:
                        s = "SBC A, L";
                        break;
                    case 0x9E:
                        s = "SBC A, (HL)";
                        break;
                    case 0x9F:
                        s = "SBC A, A";
                        break;
                    case 0xA0:
                        s = "AND B";
                        break;
                    case 0xA1:
                        s = "AND C";
                        break;
                    case 0xA2:
                        s = "AND D";
                        break;
                    case 0xA3:
                        s = "AND E";
                        break;
                    case 0xA4:
                        s = "AND H";
                        break;
                    case 0xA5:
                        s = "AND L";
                        break;
                    case 0xA6:
                        s = "AND (HL)";
                        break;
                    case 0xA7:
                        s = "AND A";
                        break;
                    case 0xA8:
                        s = "XOR B";
                        break;
                    case 0xA9:
                        s = "XOR C";
                        break;
                    case 0xAA:
                        s = "XOR D";
                        break;
                    case 0xAB:
                        s = "XOR E";
                        break;
                    case 0xAC:
                        s = "XOR H";
                        break;
                    case 0xAD:
                        s = "XOR L";
                        break;
                    case 0xAE:
                        s = "XOR (HL)";
                        break;
                    case 0xAF:
                        s = "XOR A";
                        break;
                    case 0xB0:
                        s = "OR B";
                        break;
                    case 0xB1:
                        s = "OR C";
                        break;
                    case 0xB2:
                        s = "OR D";
                        break;
                    case 0xB3:
                        s = "OR E";
                        break;
                    case 0xB4:
                        s = "OR H";
                        break;
                    case 0xB5:
                        s = "OR L";
                        break;
                    case 0xB6:
                        s = "OR (HL)";
                        break;
                    case 0xB7:
                        s = "OR A";
                        break;
                    case 0xB8:
                        s = "CP B";
                        break;
                    case 0xB9:
                        s = "CP C";
                        break;
                    case 0xBA:
                        s = "CP D";
                        break;
                    case 0xBB:
                        s = "CP E";
                        break;
                    case 0xBC:
                        s = "CP H";
                        break;
                    case 0xBD:
                        s = "CP L";
                        break;
                    case 0xBE:
                        s = "CP (HL)";
                        break;
                    case 0xBF:
                        s = "CP A";
                        break;
                    case 0xC0:
                        s = "RET NZ";
                        break;
                    case 0xC1:
                        s = "POP BC";
                        break;
                    case 0xC2:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "JP NZ, " + imm;
                        break;
                    case 0xC3:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "JP " + imm;
                        break;
                    case 0xC4:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "CALL NZ, " + imm;
                        break;
                    case 0xC5:
                        s = "PUSH BC";
                        break;
                    case 0xC6:
                        imm = program[++pc] & 0xFF;
                        s = "ADD A, " + imm;
                        break;
                    case 0xC7:
                        s = "RST 0";
                        break;
                    case 0xC8:
                        s = "RET Z";
                        break;
                    case 0xC9:
                        s = "RET";
                        break;
                    case 0xCA:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "JP Z, " + imm;
                        break;
                    case 0xCC:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "CALL Z, " + imm;
                        break;
                    case 0xCD:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "CALL " + imm;
                        break;
                    case 0xCE:
                        imm = program[++pc] & 0xFF;
                        s = "ADC A, " + imm;
                        break;
                    case 0xCF:
                        s = "RST 1";
                        break;
                    case 0xD0:
                        s = "RET NC";
                        break;
                    case 0xD1:
                        s = "POP DE";
                        break;
                    case 0xD2:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "JP NC, " + imm;
                        break;
                    case 0xD4:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "CALL NC, " + imm;
                        break;
                    case 0xD5:
                        s = "PUSH DE";
                        break;
                    case 0xD6:
                        imm = program[++pc] & 0xFF;
                        s = "SUB " + imm;
                        break;
                    case 0xD7:
                        s = "RST 2";
                        break;
                    case 0xD8:
                        s = "RET C";
                        break;
                    case 0xD9:
                        s = "RETI";
                        break;
                    case 0xDA:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "JP C, " + imm;
                        break;
                    case 0xDC:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "CALL C, " + imm;
                        break;
                    case 0xDE:
                        imm = program[++pc] & 0xFF;
                        s = "SBC A, " + imm;
                        break;
                    case 0xDF:
                        s = "RST 3";
                        break;
                    case 0xE0:
                        imm = program[++pc] & 0xFF;
                        s = "LD (" + imm + "), A";
                        break;
                    case 0xE1:
                        s = "POP HL";
                        break;
                    case 0xE2:
                        s = "LD (C), A";
                        break;
                    case 0xE5:
                        s = "PUSH HL";
                        break;
                    case 0xE6:
                        imm = program[++pc] & 0xFF;
                        s = "AND " + imm;
                        break;
                    case 0xE7:
                        s = "RST 4";
                        break;
                    case 0xE8:
                        imm = program[++pc] & 0xFF;
                        s = "ADD SP, " + imm;
                        break;
                    case 0xE9:
                        s = "JP (HL)";
                        break;
                    case 0xEA:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "LD (" + imm + "), A";
                        break;
                    case 0xEE:
                        imm = program[++pc] & 0xFF;
                        s = "XOR " + imm;
                        break;
                    case 0xEF:
                        s = "RST 5";
                        break;
                    case 0xF0:
                        imm = program[++pc] & 0xFF;
                        s = "LD A, (" + imm + ")";
                        break;
                    case 0xF1:
                        s = "POP AF";
                        break;
                    case 0xF2:
                        s = "LD A, (C)";
                        break;
                    case 0xF3:
                        s = "DI";
                        break;
                    case 0xF5:
                        s = "PUSH AF";
                        break;
                    case 0xF6:
                        imm = program[++pc] & 0xFF;
                        s = "OR " + imm;
                        break;
                    case 0xF7:
                        s = "RST 6";
                        break;
                    case 0xF8:
                        imm = program[++pc] & 0xFF;
                        s = "LD HL, SP+" + imm;
                        break;
                    case 0xF9:
                        s = "LD SP, HL";
                        break;
                    case 0xFA:
                        imm = (program[++pc] & 0xFF) | ((program[++pc] & 0xFF) << 8);
                        s = "LD A, (" + imm + ")";
                        break;
                    case 0xFB:
                        s = "EI";
                        break;
                    case 0xFE:
                        imm = program[++pc] & 0xFF;
                        s = "CP " + imm;
                        break;
                    case 0xFF:
                        s = "RST 7";
                        break;
                    }
                }
                writer.write(s);
                writer.write("\n");
                pc++;
            }
            writer.close();   
        }
        catch(Exception e){
            System.out.println("Error in writing file: " + e.getMessage());
        }
    }

    private static String cbRegConverter(int secondNibble){
        if(secondNibble >= 8){
            secondNibble -= 8;
        }
        return cbRegIndex[secondNibble];
    }
}