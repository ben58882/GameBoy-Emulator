//import java.io.FileWriter;

public class Main {
    public static void main(String args[]) {
        System.out.println("1. Loading ROM...");
        String name = "rom file path here";
        Catridge catridge = new Catridge(name, FileParser.parse(name));
        
        System.out.println("2. Initializing Gameboy...");
        Gameboy gb = new Gameboy(catridge);
        
        System.out.println("3. Starting execution loop...");
        
        while(true) {
            gb.run();
        }
    }
}