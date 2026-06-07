//import java.io.FileWriter;

public class Main {
    public static void main(String args[]) {
        System.out.println("1. Loading ROM...");
        Catridge catridge = new Catridge(FileParser.parse("drMario.gb"));
        
        System.out.println("2. Initializing Gameboy...");
        Gameboy gb = new Gameboy(catridge);
        
        System.out.println("3. Starting execution loop...");
        
        while(true) {
            gb.run();
        }
    }
}