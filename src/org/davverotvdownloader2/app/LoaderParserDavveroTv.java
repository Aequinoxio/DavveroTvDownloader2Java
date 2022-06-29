package org.davverotvdownloader2.app;//import org.jetbrains.annotations.NotNull;

//import org.jetbrains.annotations.NotNull;

//import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Versione pot 20 gennaio 2022 - rivisto il parser dell'url (bugfix sulla regexp)
 *
 * Classe di test per capire come scaricare i video da davvero.tv (agosto 2021 - dopo il cambio da Vimeo)
 * per ora recupera l'url corrispondente alla massima risoluzione
 * <p>
 * L'algoritmo è il seguente:
 * <p>
 * Esempio di iframe presente nella pagina iniziale di guarda.davvero.tv
 * <iframe style="min-height:95vh" width="100%" src="https://webtools-09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/embed/LMfnByhrOfLS?autoPlay=true" frameborder="0" allowfullscreen="">
 * </iframe>
 * <p>
 * Step 1
 * 1. caricare la pagina iniziale
 * 2. cercare iframe e salvare l'url in src=
 * <p>
 * Step 2
 * 3. caricare il link a cui punta l'iframe
 * 4. cercare negli header:
 * Location: //webtools-09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/embed/LMfnByhrOfLS?autoplay=true&T=1629141412
 * <p>
 * Step 3
 * 5. aggiungere https: all'inizio della stringa recuperata al passo precedente
 * es: https://webtools-09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/embed/LMfnByhrOfLS?autoplay=true&T=1629141412
 * <p>
 * 6. trasformarla in
 * https://09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/vod/LMfnByhrOfLS/playlist.m3u8?T=1629141412
 * <p>
 * vedere la sintassi in generateCorrectUrl()
 * <p>
 * Step 4
 * 7. caricare l'url generata e cercare negli header Location:
 * location: https://StreamCdnB9-09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/vod/LMfnByhrOfLS/playlist.m3u8?T=1629141412
 * <p>
 * Step 5
 * 8. sostituire playlist.m3u8 in original.mp4
 * https://StreamCdnB9-09bd1346f7a44cc9ac230cc1cb22ca4f.msvdn.net/vod/LMfnByhrOfLS/original.mp4?T=1629141412
 * <p>
 * 9. scaricare l'url ottenuta.
 */
public class LoaderParserDavveroTv implements LoaderParser {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:86.0) Gecko/20100101 Firefox/86.0";
    private static final String CHUNK_PLACEHOLDER = "***CHUNKLIST***";

    private final ParsedDetailsDataSet parsedDetailsDataSet = new ParsedDetailsDataSet();

    private final HashMap<String, String> mappaRisoluzioniChunklist = parsedDetailsDataSet.getMappaRisoluzioniChunklist();
    private final HashMap<String, ArrayList<String>> mappaRisoluzioniSegmentiChunklist = parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist();
    private final ParsedDetailsDataSet.MainParametersUrlGrabbed mainParametersUrlGrabbed = parsedDetailsDataSet.getMainPatametersUrlGrabbed();

    private final WorkerUpdateCallback workerUpdateCallback;

    public LoaderParserDavveroTv(WorkerUpdateCallback workerUpdateCallback) {
        this.workerUpdateCallback = workerUpdateCallback;
    }

    /**
     * Fa tutto il lavoro dal caricamento pagina iniziale alla produzione della videourl
     *
     * @param url
     * @return
     * @throws IOException
     */
    public void caricaPaginaInizialeEGeneraVideoUrl(String url, boolean mostraTutto) {

        mainParametersUrlGrabbed.setMainSrcString(url);

        // STEP 1
        SystemConsoleLog(1);
        Document pageDocument = null;
        Connection connection;

        try {
//            connection =  Jsoup.connect(url)
//                    .userAgent(USER_AGENT)
//                    .header("Host","www.byoblu.com")
//                    .header("Accept","*/*")
//                    .followRedirects(true)
//                    ;
//            pageDocument = connection.get();

            ////////// CLOUDFLARE TEST

            // Loggo tutto
            if (mostraTutto) {
                workerUpdateCallback.updateFromWorker( "Step 1 sull'url\n\t"+url);
            }

            URL connURL = new URL(url);

            HttpURLConnection conn = (HttpURLConnection) connURL.openConnection();
            conn.setRequestProperty("User-Agent",USER_AGENT);
            conn.connect();

            // loggo tutto
            if (mostraTutto) {
                workerUpdateCallback.updateFromWorker("Step 1 connection\n\t"+conn.getResponseMessage());
            }

            /////////////// Recupera il charset ed imposta quello di default se non lo trova
            // thanx to: https://stackoverflow.com/questions/3934251/urlconnection-does-not-get-the-charset
            String contentType = conn.getContentType();
            String[] values = contentType.split(";"); // values.length should be 2
            String charSet="";

            // loggo tutto
            if (mostraTutto) {
                workerUpdateCallback.updateFromWorker("Step 1 Content type:\n\t"+contentType);
            }

            for (String value : values) {
                value = value.trim();

                if (value.toLowerCase().startsWith("charset=")) {
                    charSet = value.substring("charset=".length());
                    break; // esco al primo trovato
                }
            }

            if ("".equals(charSet)) {
                charSet = "UTF-8"; //Assumption
            }

            //////////////////// CHARSET BLOCK ////////////

            InputStream is ;
            boolean isCloudflared = conn.getResponseCode()==503;
            if (isCloudflared){
                workerUpdateCallback.updateFromWorker("**** 503 error : Cloudflare\n" );
                is = conn.getErrorStream();
            } else {
                is = conn.getInputStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

//            String str ;
//            while ((str = br.readLine())!=null){
//                sb.append(str);
//            }

            // Leggo la pagina
            pageDocument = Jsoup.parse(is,charSet, url);

            sb.append(pageDocument); // salvo il contenuto della pagina, TODO: forse può essere rimosso riorganizzando il codice a valle
            //pageDocument = Jsoup.parse(sb.toString(),charSet);

            is.close();
            br.close();
            conn.disconnect();

            // Log pagina caricata
            if (mostraTutto) {
                workerUpdateCallback.updateFromWorker("Step 1 pagina caricata\n"+sb.toString() );
            }
            if (isCloudflared){
                workerUpdateCallback.updateFromWorker( "Cloudflare parsing treatment TODO");
            }

            /////////////////////


            String iframeSrc = Objects.requireNonNull(pageDocument.select("iframe").first()).attr("src");
            String titlePage = pageDocument.select("title").text();

            // STEP 2
            SystemConsoleLog(2);
            URL iframeUrl = new URL(iframeSrc);
            HttpURLConnection httpIframeUrlConnection = (HttpURLConnection) iframeUrl.openConnection();
            httpIframeUrlConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpIframeUrlConnection.setInstanceFollowRedirects(false); // Mi basta il 302 e prendo la location
            int responseCode = httpIframeUrlConnection.getResponseCode();

            //System.out.println(String.format("URL iframe: %s - retcode %d",iframeSrc,responseCode));

            String iframeSrcRealString = httpIframeUrlConnection.getHeaderField("Location");
            //System.out.println("Redirect to:"+iframeSrcRealString);

            // STEP 3
            SystemConsoleLog(3);
            String autoplaySrcString = generateCorrectUrl("https:" + iframeSrcRealString); // Trasformo autoplay nella playlist

            URL playListUrl = new URL(autoplaySrcString);
            HttpURLConnection httpPlaylistUrlConnection = (HttpURLConnection) playListUrl.openConnection();
            httpPlaylistUrlConnection.setRequestProperty("User-Agent", USER_AGENT);
            httpPlaylistUrlConnection.setInstanceFollowRedirects(false); // Mi accontento del 302, prendo il parametro location e lo seguo a mano
            int responseCode2 = httpPlaylistUrlConnection.getResponseCode();

            // STEP 4
            SystemConsoleLog(4);
            String playlistSrcRealString = httpPlaylistUrlConnection.getHeaderField("Location");
            //System.out.println("Step4 new location finale: "+playlistSrcRealString);

            mainParametersUrlGrabbed.setPlaylistSrcString(playlistSrcRealString);

            // Carico la playlist e genero la lista dei file contenenti i chunk per ciascuna risoluzione
            URL playlistUrlReal = new URL(playlistSrcRealString);
            HttpURLConnection playListRealUrlConnection = (HttpURLConnection) playlistUrlReal.openConnection();
            playListRealUrlConnection.setRequestProperty("User-Agent", USER_AGENT);
            playListRealUrlConnection.setInstanceFollowRedirects(true); // Provo a seguire tutti gli eventuali redirect automaticamente

            // Genero la mappa risoluzioni - chunklist
            loadResolutionMapFromPlaylist(playListRealUrlConnection.getInputStream());

            // STEP 5
            SystemConsoleLog(5);
            // Genero l'url per scaricare l'originale
            String videoSrcString = playlistSrcRealString.replace("playlist.m3u8", "original.mp4");
            URL videoUrl = new URL(videoSrcString);
            HttpURLConnection videoUrlConnection = (HttpURLConnection) videoUrl.openConnection();
            long videoUrlSize = videoUrlConnection.getContentLengthLong();
            videoUrlConnection.disconnect();
            //System.out.println("Url da scaricare: "+videoSrcString + " lunghezza: "+videoUrlSize);

            mainParametersUrlGrabbed.setPageTitle(titlePage);
            mainParametersUrlGrabbed.setSizeMainMp4File(videoUrlSize);
            mainParametersUrlGrabbed.setOriginalSrcString(videoSrcString);
            mainParametersUrlGrabbed.setChunkListPrefixSrcString(playlistSrcRealString.replaceAll("playlist.m3u8.*$", ""));

            // Estraggo tutti i segmenti da ciascuna chunklist
            String chunkSrcString = playlistSrcRealString.replaceAll("playlist.m3u8.*$", CHUNK_PLACEHOLDER);
            //System.out.println("ChunkUrl da fixare con il contenuto della playlist: "+ chunkSrcString);
            loadAllChunklistsAndSegments(chunkSrcString);

            // Calcolo la lunghezza di un chunk per ciascuna risoluzione
            if (!mappaRisoluzioniSegmentiChunklist.keySet().isEmpty()) {

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
        }  catch (IOException e) {
            workerUpdateCallback.updateFromWorker(e.toString());
        }
    }

    private void loadAllChunklistsAndSegments(String chunkSrc) throws IOException {
        String tempSrc;
        String linea;

        // Ciclo su tutti i chunklist
        for (String chunkUrlKey : mappaRisoluzioniChunklist.keySet()) {
            tempSrc = chunkSrc.replace(CHUNK_PLACEHOLDER, mappaRisoluzioniChunklist.get(chunkUrlKey));
            //System.out.println(tempSrc);
            URL tempUrl = new URL(tempSrc);
            HttpURLConnection tempUrlConnection = (HttpURLConnection) tempUrl.openConnection();

            // Carico la lista dei segmenti
            try (BufferedReader br = new BufferedReader(new InputStreamReader(tempUrlConnection.getInputStream()))) {
                ArrayList<String> tempArrayList = new ArrayList<>();
                while ((linea = br.readLine()) != null) {
                    if (linea.startsWith("#") || linea.trim().isEmpty()) {
                        continue;
                    }
                    tempArrayList.add(linea);
                    //System.out.println("\t"+linea);
                }
                mappaRisoluzioniSegmentiChunklist.put(chunkUrlKey, tempArrayList);
            }
            tempUrlConnection.disconnect();
        }
    }

    public ParsedDetailsDataSet getParsedDetailsDataSet() {
        return parsedDetailsDataSet;
    }

    private void SystemConsoleLog(int step) {
        //System.out.println("**** STEP :" + step);
        workerUpdateCallback.updateFromWorker("**** STEP :" + step);
    }

    public void loadResolutionMapFromPlaylist(InputStream inputStream) throws IOException {

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
                } else { //
                    mappaRisoluzioniChunklist.put(chiave, linea);
                }
            }
        }
    }

    private @NotNull
    String generateCorrectUrl(String url) {
        String urlReplaced = url.replace("webtools-", "")
                .replace("embed", "vod")
                .replaceFirst("\\?auto[pP]lay=(true|false|.*?)&", "/playlist.m3u8\\?");

        return urlReplaced;
    }


//    public String getUrlsGrabbedOriginalSrcString(){
//        return urlsGrabbed.originalSrcString;
//    }
//    public String getUrlsGrabbedMainSrcString(){
//        return urlsGrabbed.mainSrcString;
//    }
//    public String getUrlsGrabbedPlaylistSrcString(){
//        return urlsGrabbed.playlistSrcString;
//    }
//    public String getUrlsGrabbedChunkListPrefixSrcString(){
//        return urlsGrabbed.chunkListPrefixSrcString;
//    }
//    public long getUrlsGrabbedSizeMainMp4File(){
//        return urlsGrabbed.sizeMainMp4File;
//    }
//    public HashMap<String, Long> getUrlsGrabbedChunkEstimatedSizePerResolution(){
//        return urlsGrabbed.chunkEstimatedSizePerResolution;
//    }
}
