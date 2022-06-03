import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoaderParserByoBlu implements LoaderParser {

    //    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0";
//    private static final String CHUNK_PLACEHOLDER = "***CHUNKLIST***";
    String playlistRegexp = "https://.*?playlist.m3u8";
    private final ParsedDetailsDataSet parsedDetailsDataSet = new ParsedDetailsDataSet();
    private final HashMap<String, String> mappaRisoluzioniChunklist = parsedDetailsDataSet.getMappaRisoluzioniChunklist();
    private final HashMap<String, ArrayList<String>> mappaRisoluzioniSegmentiChunklist = parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist();
    private final ParsedDetailsDataSet.MainParametersUrlGrabbed mainParametersUrlGrabbed = parsedDetailsDataSet.getMainPatametersUrlGrabbed();
    private final WorkerUpdateCallback workerUpdateCallback;

    public LoaderParserByoBlu(WorkerUpdateCallback workerUpdateCallback) {
        this.workerUpdateCallback = workerUpdateCallback;
    }

    /**
     * Metodo principale da chiamare per caricare la pagina iniziale ed impostare la struttura dati corretta
     *
     * @param mainUrl     - Url principale contenente il video da scaricare
     * @param mostraTutto - true per mostrare i dettagli delle azioni svolte, false mostra il minimo possibile
     */
    public void caricaPaginaInizialeEGeneraVideoUrl(String mainUrl, boolean mostraTutto) {

        // Imposto la struttura dati
        mainParametersUrlGrabbed.setMainSrcString(mainUrl);

        // DEBUG
        // String mainUrl = "https://www.byoblu.com/2022/05/21/bielorussia-russia-e-ucraina-attraverso-i-secoli/";

        String playlist = null;
        String titlePage = "TODO";
        long videoUrlSize = 0;
        String videoSrcString = "TODO";
        //String playlistRealString = null;

        try {
            SystemConsoleLog(1, "Connessione url: " + mainUrl, mostraTutto);

            // Carico la pagina iniziale e recupero la playlist - STEP 1
            playlist = loadMainPage(mainUrl);

            if (playlist != null) {
                mainParametersUrlGrabbed.setPlaylistSrcString(playlist);
                // Recupero il titolo  nel metodo del caricamento pagina iniziale
                mainParametersUrlGrabbed.setSizeMainMp4File(videoUrlSize);
                mainParametersUrlGrabbed.setOriginalSrcString(videoSrcString);

                // Carico le chunklist dalla playlist - STEP 2
                SystemConsoleLog(2, "Recupero chunklist dalla playlist: " + playlist, mostraTutto);
                loadPlaylist(playlist);

                // Carico l'elenco dei chunk da ciascuna chunk list
                if (!mappaRisoluzioniChunklist.isEmpty()) {
                    SystemConsoleLog(3, "Recupero tutti i segmenti per le chunklist", mostraTutto);
                    loadAllChunklistsAndSegments(mainParametersUrlGrabbed.getChunkListPrefixSrcString());
                }

                // Calcolo la lunghezza di un chunk per ciascuna risoluzione
                if (!mappaRisoluzioniSegmentiChunklist.keySet().isEmpty()) {

                    SystemConsoleLog(4, "Calcolo la dimensione stimata dei vari videofile", mostraTutto);
                    for (String chiave : mappaRisoluzioniSegmentiChunklist.keySet()) {
                        URL chunkTempUrl = new URL(
                                mainParametersUrlGrabbed.getChunkListPrefixSrcString() +
                                        mappaRisoluzioniSegmentiChunklist.get(chiave).get(0) // Check sul primo segmento
                        );
                        HttpURLConnection chunkSegmentConnection = (HttpURLConnection) chunkTempUrl.openConnection();

                        mainParametersUrlGrabbed.getChunkEstimatedSizePerResolution().put(
                                chiave,
                                chunkSegmentConnection.getContentLengthLong()
                        );
                        chunkSegmentConnection.disconnect();
                    }
                }
            }
        } catch (IOException e) {
            SystemConsoleLog(-1, e.toString(), true); // Stampo comunque tutto in caso di eccezione
            throw new RuntimeException(e);
        }
    }

    public ParsedDetailsDataSet getParsedDetailsDataSet() {
        return parsedDetailsDataSet;
    }

    ////////////////////////////////// PRIVATE SECTION //////////////////////////////////

    /**
     * Carica la lista dei vari file .ts componenti il chuckfile
     *
     * @param chunkSrc - parte dell'url prima del nome del file chunklist-XYZ.m3u8
     * @throws IOException
     */
    private void loadAllChunklistsAndSegments(String chunkSrc) throws IOException {
        String tempSrc;
        String linea;

        // Ciclo su tutti i chunklist
        for (String chunkUrlKey : mappaRisoluzioniChunklist.keySet()) {
            tempSrc = chunkSrc + mappaRisoluzioniChunklist.get(chunkUrlKey); // Compongo l'url completa per il file chunklist corrispondente alla risoluzione (chiave)

            OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
            Request request = new Request.Builder().url(tempSrc).build();
            Response response = okHttpClient.newCall(request).execute();

            // Se c'è stato un errore continuo con il successivo. TODO: Migliorare in base ai codici ritornati
            if (response.code()>=400){
                continue;
            }

            String responseString = response.body().string();

            // Carico la lista dei segmenti
            try (BufferedReader br = new BufferedReader(new StringReader(responseString))) {
                ArrayList<String> tempArrayList = new ArrayList<>();
                while ((linea = br.readLine()) != null) {
                    if (linea.startsWith("#") || linea.trim().isEmpty()) {
                        continue;
                    }
                    tempArrayList.add(linea);
                }
                mappaRisoluzioniSegmentiChunklist.put(chunkUrlKey, tempArrayList);
            }
        }
    }

    /**
     * Carica il file playlist.m3u8
     *
     * @param playlist - url completa https://codice.dominio.suffisso/folders/playlist.m3u8
     * @throws IOException
     */
    private void loadPlaylist(String playlist) throws IOException {
        String urlPrefix = playlist.replace("playlist.m3u8", "");
        mainParametersUrlGrabbed.setChunkListPrefixSrcString(urlPrefix);

        // Carico la pagina contenente l'url della playlist
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(playlist).build();
        Response response = okHttpClient.newCall(request).execute();

        // Se c'è stato un errore esco subito. TODO: Migliorare in base ai codici ritornati
        if(response.code()>=400){
            return;
        }

        String responseString = response.body().string().trim();

        // Check emptiness
        if ("".equals(responseString)) {
            throw new IOException("loadPlayList(String) - Risposta vuota dal server");
        }

        InputStream targetStream = new ByteArrayInputStream(responseString.getBytes());
        loadResolutionMapFromPlaylist(targetStream, mappaRisoluzioniChunklist);
    }

    /**
     * Step 1: carica la pagina iniziale e ritorna la playlist trovata altrimenti null
     *
     * @param urlString - Url della pagina principale da caricare
     * @return - L'url della playlist trovata o null se non ha trovato nulla
     * @throws IOException
     */
    private String loadMainPage(String urlString) throws IOException {
        String result = null;

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Request request = new Request.Builder().url(urlString).build();
        Response response = okHttpClient.newCall(request).execute();

        // Se non c'è stato un errore proseguo. TODO: Migliorare in base ai codici ritornati
        if (response.code()<400) {
            String responseString = response.body().string();
            Document pageDocument = Jsoup.parse(responseString);
            String titlePage = pageDocument.select("title").text();
            mainParametersUrlGrabbed.setPageTitle(titlePage);

            Pattern pattern = Pattern.compile(playlistRegexp);
            Matcher matcher = pattern.matcher(responseString);

            if (matcher.find()) {
                result = matcher.group();
                SystemConsoleLog(1, "Playlist: " + result, true); //Mpstro comunque tutto
            }
        }
        return result;
    }

    /**
     * Loggo sulla consolle e chiamo la callback
     *
     * @param step        - passo di parsing
     * @param msg         - messaggio da stampare
     * @param mostraTutto - true se devo stampare, false esco subito
     */
    private void SystemConsoleLog(int step, String msg, boolean mostraTutto) {
        if (mostraTutto) {
            System.out.println("**** STEP :" + step + "\n\t" + msg + "\n");
            workerUpdateCallback.updateFromWorker("**** STEP :" + step + "\n\t" + msg + "\n");
        }
    }
}
