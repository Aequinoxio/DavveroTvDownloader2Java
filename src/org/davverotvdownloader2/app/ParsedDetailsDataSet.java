package org.davverotvdownloader2.app;

import java.util.ArrayList;
import java.util.HashMap;

public class ParsedDetailsDataSet {
    /*
        Mappa risoluzioni verticali, nome del file chunklist_XYX.m3u8 video corrispondente - Corrisponde all'ultima parte dell'url UrlsGrabbed.chunkListPrefixSrcString
     */
    private final HashMap<String,String> mappaRisoluzioniChunklist = new HashMap<>();

    /*
        Mappa risoluzioni verticali, nomifile.ts - Sono i file che compongono il file mp4 completo
     */
    private final HashMap<String, ArrayList<String>> mappaRisoluzioniSegmentiChunklist = new HashMap<>();

    /*
        Dati comuni corrispondenti all'url da scaricare
     */
    private final MainParametersUrlGrabbed mainParametersUrlGrabbed = new MainParametersUrlGrabbed();

    /**
     * Ritorna la mappa risoluzioni verticali->file chunklist-XYZ-m3u8
     */
    public HashMap<String, String> getMappaRisoluzioniChunklist() {
        return mappaRisoluzioniChunklist;
    }

    /**
     * Ritorna la mappa risoluzioni verticali->file sei singoli segmenti componenti (file.ts) componenti il file mp4
     * @return
     */
    public HashMap<String, ArrayList<String>> getMappaRisoluzioniSegmentiChunklist() {
        return mappaRisoluzioniSegmentiChunklist;
    }

    /**
     * Ritorna i parametri associati all'url principale scaricata
     * @return
     */
    public MainParametersUrlGrabbed getMainPatametersUrlGrabbed() {
        return mainParametersUrlGrabbed;
    }

    /**
     * Classe che rappresenta i parametri principali dell'url di cui fare il parsing e da scaricare
     */
    static class MainParametersUrlGrabbed {

        private String mainSrcString;       // Stringa contenente l'url principale da cui si parte per l'intero processo di parsing
        private String originalSrcString;   // Url del video original.mp4
        private String playlistSrcString;   // Url della playlist
        private String pageTitle;           // Titolo della pagina catturata

        private String chunkListPrefixSrcString;    // Prefisso url per scaricare i vari chunk, da prependere alla lista delle chunk scaricate
        private long sizeMainMp4File;       // Lunghezza del file original.mp4
        private final HashMap<String,Long> chunkEstimatedSizePerResolution = new HashMap<>();   // Mappa risoluzione - elenco di chunk relativi

        public void setMainSrcString(String mainSrcString) {
            this.mainSrcString = mainSrcString;
        }

        public void setPlaylistSrcString(String playlistSrcString) {
            this.playlistSrcString = playlistSrcString;
        }

        public void setOriginalSrcString(String originalSrcString) {
            this.originalSrcString = originalSrcString;
        }

        public void setChunkListPrefixSrcString(String chunkListPrefixSrcString) {
            this.chunkListPrefixSrcString = chunkListPrefixSrcString;
        }

        public void setSizeMainMp4File(long sizeMainMp4File) {
            this.sizeMainMp4File = sizeMainMp4File;
        }
        public String getOriginalSrcString(){
            return originalSrcString;
        }
        public String getMainSrcString(){
            return mainSrcString;
        }
        public String getPlaylistSrcString(){
            return playlistSrcString;
        }
        public String getChunkListPrefixSrcString(){
            return chunkListPrefixSrcString;
        }
        public long getSizeMainMp4File(){
            return sizeMainMp4File;
        }
        public HashMap<String, Long> getChunkEstimatedSizePerResolution(){
            return chunkEstimatedSizePerResolution;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public void setPageTitle(String pageTitle) {
            this.pageTitle = pageTitle;
        }
    }
}
