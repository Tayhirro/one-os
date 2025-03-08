package newOs.tools;

public class ProcessTool {
    public static int getPid(String processName){
        int hashCode = processName.hashCode();
        return Math.abs(hashCode) % 256;
    }
}
