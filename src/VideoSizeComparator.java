import java.util.Comparator;

/**
 * Compara le videosize confrontate sulla risoluzione verticale
 * La stringa deve essere del tipo: 800x600
 */
public class VideoSizeComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
        int yRes1;
        int yRes2;
        try {
            yRes1 = Integer.parseInt(
                    o1.trim().split("x")[1]
            );
            yRes2 = Integer.parseInt(
                    o2.trim().split("x")[1]
            );
        } catch(Exception e){
            yRes1=0;
            yRes2=0;
        }
        return yRes1-yRes2;
    }
}

