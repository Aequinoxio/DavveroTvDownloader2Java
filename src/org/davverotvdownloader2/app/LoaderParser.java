package org.davverotvdownloader2.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface LoaderParser {

    /**
     * Fa tutto il lavoro dal caricamento pagina iniziale alla produzione della videourl
     *
     * @param url
     * @return
     * @throws IOException
     */
    void caricaPaginaInizialeEGeneraVideoUrl(String url, boolean mostraTutto) ;

    ParsedDetailsDataSet getParsedDetailsDataSet();

    // void loadResolutionMapFromPlaylist(InputStream inputStream) throws IOException;
    default void loadResolutionMapFromPlaylist(InputStream inputStream, HashMap<String, String> mappaRisoluzioniChunklist) throws IOException {

        Pattern pattern = Pattern.compile("^#EXT-X-STREAM-INF.*?:.*RESOLUTION=(.*?x.*?),F.*$");
        Matcher matcher;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String linea;
            String chiave = "";
            while ((linea = br.readLine()) != null) {
                // Check se è una linea valida
                if (linea.trim().isEmpty()) {
                    continue;
                }

                // Mini parser
                if (linea.startsWith("#")) {
                    if (linea.startsWith("#EXT-X-STREAM-INF")) {
                        matcher = pattern.matcher(linea);
                        if (matcher.matches()) {
                            chiave = matcher.group(1);
                        }
                    }
                } else { // se la linea inizia per chunklist allora la associo alla chiave già trovata, altrimenti proseguo
                    if (linea.startsWith("chunklist")) {
                        mappaRisoluzioniChunklist.put(chiave, linea);
                    }
                }
            }
        }
    }


}
