import java.nio.file.*;

public class FileParser {
    public static byte[] parse(String fileName){
        Path path = Paths.get(fileName);
        try{
            return Files.readAllBytes(path);
        }
        catch(Exception e){
            System.out.println("ERROR: " + e.getMessage());
            return null;
        }
    }
}
