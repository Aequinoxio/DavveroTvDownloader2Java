package org.davverotvdownloader2.app;

import org.apache.commons.cli.*;
import org.davverotvdownloader2.prefs.AppPrefs;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainForm {
    private JTextField txtMainUrl;
    private JButton btnStart;
    private JTextArea txtLog;
    private JPanel MainPanel;
    private JButton btnClearLog;
    private JCheckBox cbAutoCopy;
    private JCheckBox cbCopyTitle;
    private JButton copiaNuovamenteButton;
    private JButton btnHelp;
    private JCheckBox cbLogAll;
    private JRadioButton byoBluParserRadioButton;
    private JRadioButton davveroTVParserRadioButton;
    private JProgressBar pbLoading;
    private JCheckBox cbCreaClawljob;
    private JTextField txtCrawljobSavePath;
    private JButton btnSetCrawljobPath;
    private JButton salvaPerDownThemAllButton;

//    private void createUIComponents() {
//        // TODO: place custom component creation code here
//    }

    private enum ParserType {
        DavveroTV,
        ByoBlu
    }


    protected ParserType parserType = ParserType.ByoBlu;

    protected ParsedDetailsDataSet parsedDetailsDataSet;

    public MainForm() {

        pbLoading.setVisible(true);
        pbLoading.setIndeterminate(false);

        // Imposto correttamente la visibilità per questi campi
        //txtCrawljobSavePath.setEnabled(cbCreaClawljob.isSelected());
        btnSetCrawljobPath.setEnabled(cbCreaClawljob.isSelected());

        //txtCrawljobSavePath.setText(System.getProperty("user.dir"));
        txtCrawljobSavePath.setText(AppPrefs.SaveFolderLocation.get());
        txtCrawljobSavePath.setDisabledTextColor(Color.BLACK);

        // Check di raggiungibilità del path
        File tempFile = new File(AppPrefs.SaveFolderLocation.get());
        if (tempFile.isDirectory()) {
            txtCrawljobSavePath.setBackground(Color.GREEN);
        } else {
            txtCrawljobSavePath.setBackground(Color.RED);
        }
        ////

        cbAutoCopy.setSelected(AppPrefs.AutoCopiaLowResUrlBoolean.get("true").equals("true"));

        cbCreaClawljob.setSelected(AppPrefs.AutoCopiaLowResUrlBoolean.get("true").equals("true"));
        btnSetCrawljobPath.setEnabled(cbCreaClawljob.isSelected());

        /////////////// LISTENERS ///////////////
        cbCreaClawljob.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                //txtCrawljobSavePath.setEnabled(e.getStateChange()==1);
                AppPrefs.SaveLowResCrawljobFlag.put(cbAutoCopy.isSelected() ? "true" : "false");
                btnSetCrawljobPath.setEnabled(cbCreaClawljob.isSelected());
            }
        });

        cbAutoCopy.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                AppPrefs.AutoCopiaLowResUrlBoolean.put(cbAutoCopy.isSelected() ? "true" : "false");
            }
        });

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (byoBluParserRadioButton.isSelected()) {
                    parserType = ParserType.ByoBlu;
                }
                if (davveroTVParserRadioButton.isSelected()) {
                    parserType = ParserType.DavveroTV;
                }

                ParserWorker parserWorker = new ParserWorker(parserType);

                // TEST PER OTTIMIZZAZIONE AGGIORNAMENTO UI
//                parserWorker.addPropertyChangeListener(new PropertyChangeListener() {
//                    @Override
//                    public void propertyChange(PropertyChangeEvent evt) {
////                        if ("progress".equals(evt.getPropertyName())){
//                            String progressOld =   evt.getOldValue().toString();
//                            String progressNew =   evt.getNewValue().toString();
//                            System.out.println("****** "+evt.getPropertyName()+ " -- "+ progressOld+" -- "+progressNew);
////                        }
//                    }
//                });

                parserWorker.execute();
            }
        };

        btnStart.addActionListener(action);
        txtMainUrl.addActionListener(action);

        btnClearLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtLog.setText("");
            }
        });
        copiaNuovamenteButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copiaLowResUrlUI(parsedDetailsDataSet);
            }
        });
        btnHelp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HelpDialog dialog = new HelpDialog();
                dialog.pack();
                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
            }
        });

        // Seleziona tutto al focus della main url
        txtMainUrl.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                txtMainUrl.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {

            }
        });
        btnSetCrawljobPath.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser jFileChooser = new JFileChooser();
                File startPath = new File(txtCrawljobSavePath.getText().trim());
                if (!startPath.exists()) { // Prioritario ciò che è scritto nella textarea ma se il file non esiste lo prendo dalle prefs
                    startPath = new File(AppPrefs.SaveFolderLocation.get());
                }

                jFileChooser.setCurrentDirectory(startPath);
                jFileChooser.setDialogTitle("Directory di salvataggio del file");
                jFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jFileChooser.setAcceptAllFileFilterUsed(false);
                if (jFileChooser.showOpenDialog(MainPanel) == JFileChooser.APPROVE_OPTION) {
                    AppPrefs.SaveFolderLocation.put(jFileChooser.getSelectedFile().getAbsolutePath().trim());
                    txtCrawljobSavePath.setText(jFileChooser.getSelectedFile().getAbsolutePath().trim());
                    txtCrawljobSavePath.setBackground(Color.GREEN);
                }
            }
        });
        salvaPerDownThemAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (parsedDetailsDataSet == null) {
                    JOptionPane.showMessageDialog(MainPanel, "Dati non presenti");
                    return;
                }

                String fileContent = generateAndLogDownThemAllPattern(parsedDetailsDataSet, false);
                // TODO: CHECK esistenza dati

                File prefsFile = new File(AppPrefs.SaveDownThemAllParamsFile.get());


                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setCurrentDirectory(prefsFile);
                jFileChooser.setSelectedFile(prefsFile);
                jFileChooser.setDialogTitle("Salva i dati per DownThemAll");

                Path filePath=null; // Path del file da salvare
                boolean writeFile = false;
                boolean continueTrying = true;

                while(continueTrying) {
                    int optionChoosed = jFileChooser.showSaveDialog(MainPanel);
                    if (optionChoosed == JFileChooser.APPROVE_OPTION) {
                        filePath = jFileChooser.getSelectedFile().toPath();

                        if (Files.exists(jFileChooser.getSelectedFile().toPath())) {
                            int warningChoosedValue = JOptionPane.showConfirmDialog(MainPanel, "Sovrascrivo il file", "Attenzione", JOptionPane.YES_NO_CANCEL_OPTION);
                            switch (warningChoosedValue){
                                case JOptionPane.OK_OPTION :
                                    writeFile=true;
                                    continueTrying=false;
                                    break;
                                case JOptionPane.CANCEL_OPTION:
                                    writeFile=false;
                                    continueTrying=false;
                                    break;
                                default:
                                    continueTrying=true;
                            }
                        } else {
                            writeFile=true;
                            continueTrying=false;
                        }
                    } else {
                        writeFile=false;
                        continueTrying=false;
                    }
                }

                if (writeFile){
                    try {
                        Files.write(filePath, fileContent.getBytes());
                        AppPrefs.SaveDownThemAllParamsFile.put(filePath.toString());
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        });
    }

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("h", "help", false, "Help");
        options.addOption("g", "gui", false, "Avvia l'interfaccia grafica");
        options.addOption("n", "nogui", true, "Avvia il download in consolle usando l'url passata per argomento");
        options.addOption("c", "copy", true, "Vale solo se è attivata l'opzione -n.\nCopia l'url relativa alla risoluzione più bassa nella clipboard al termine del parsing. Se l'argomento è 'true' o '1' allora copia anche il titolo altrimenti solo l'url.");

        CommandLineParser commandLineParser = new DefaultParser();
        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            if (commandLine.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DavveroTVVieoDoenloader2", options, true);
                return;
            }

            if (commandLine.hasOption("x") || commandLine.getOptions().length == 0) {
                JFrame frame = new JFrame("MainForm");
                frame.setContentPane(new MainForm().MainPanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else if (commandLine.hasOption("c")) {
                boolean copyInClipboard = commandLine.hasOption("m");
                boolean copyTitleInClipboard = false;
                if (copyInClipboard) {
                    copyTitleInClipboard = "true".equals(commandLine.getOptionValue("m").trim()) ||
                            "1".equals(commandLine.getOptionValue("m").trim());
                }

                String urlString = commandLine.getOptionValue("c").trim();
                System.out.println("Parsing url: " + urlString + "\n");

                // TODO: Per la consolle si parte con ByoBlu, generalzzarlo
                LoaderParser loaderParser = new LoaderParserByoBlu(new WorkerUpdateCallback() {
                    @Override
                    public void updateFromWorker(String messaggio) {
                        System.out.println(messaggio);
                    }
                });

                loaderParser.caricaPaginaInizialeEGeneraVideoUrl(urlString, false);

                System.out.println(stringifyAllData(
                        loaderParser.getParsedDetailsDataSet()
                ));

                if (copyInClipboard) {
                    copiaLowResUrlConsolle(loaderParser.getParsedDetailsDataSet(), copyTitleInClipboard);
                }
            }
        } catch (ParseException ma) {
            System.out.println(ma.getLocalizedMessage());
        }
    }

    public static String stringifyAllData(ParsedDetailsDataSet parsedDetailsDataSet) {
        StringBuilder sb = new StringBuilder();
        // parsedDetailsDataSet=loaderParserDavveroTv.getParsedDetailsDataSet();

        if (parsedDetailsDataSet.getMappaRisoluzioniChunklist().size() == 0) {
            sb.append("Qualcosa è andato storto");
            return sb.toString();
        }
        // Loggo quanto ho trovato nella textarea dedicata
        sb.append("Titolo della pagina: ").append(parsedDetailsDataSet.getMainParametersUrlGrabbed().getPageTitle());
        sb.append("\n");
        sb.append("\tUrl per la playlist risoluzioni-chunklist");
        sb.append("\n");
        sb.append(parsedDetailsDataSet
                .getMainParametersUrlGrabbed().getChunkListPrefixSrcString());
        sb.append("\n");

        sb.append("Stampo la mappa risoluzioni-chunklists");
        sb.append("\n");
        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());
        String tempUrl;
        for (String chiave : tempListOfKeys) {
            tempUrl = parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(chiave);

            sb.append("\t" + chiave + " - " + tempUrl + " dim stimate: " +
                    parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist().get(chiave).size() *
                            parsedDetailsDataSet.getMainParametersUrlGrabbed()
                                    .getChunkEstimatedSizePerResolution().get(chiave));
            sb.append("\n");
            sb.append("\t\turl completa: " + parsedDetailsDataSet.getMainParametersUrlGrabbed()
                    .getChunkListPrefixSrcString() + tempUrl
            );
            sb.append("\n");
        }

        return sb.toString();
    }


    private class ParserWorker extends SwingWorker<ParsedDetailsDataSet, String> {
        LoaderParser loaderParser;
        ParserType parserType;

        public ParserWorker(ParserType parserType) {
            this.parserType = parserType;
        }

        @Override
        protected ParsedDetailsDataSet doInBackground() {
            String mainSrc = txtMainUrl.getText().trim();

            pbLoading.setIndeterminate(true);
            MainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            btnStart.setEnabled(false);

            // TODO: migliorare in caso si usassero più di due parser
            if (parserType == ParserType.ByoBlu) {
                loaderParser = new LoaderParserByoBlu(messaggio -> publish(messaggio));
            } else {
                loaderParser = new LoaderParserDavveroTv(messaggio -> publish(messaggio));
            }

            // Se la stringa non è vuota faccio tutto quello che devo fare
            if ("".equals(mainSrc)) {
                return null;
            }

            loaderParser.caricaPaginaInizialeEGeneraVideoUrl(mainSrc, cbLogAll.isSelected());
            return loaderParser.getParsedDetailsDataSet();
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            for (String chunk : chunks) {
                txtLog.append(chunk);
                txtLog.append("\n");
            }
        }

        @Override
        protected void done() {
            super.done();

            pbLoading.setIndeterminate(false);
            MainPanel.setCursor(null);

            txtLog.append(stringifyAllData(
                    loaderParser.getParsedDetailsDataSet()
            ));

            btnStart.setEnabled(true);

            parsedDetailsDataSet = loaderParser.getParsedDetailsDataSet();

//            org.davverotvdownloader2.parsers.ParsedDetailsDataSet parsedDetailsDataSet = loaderParserDavveroTv.getParsedDetailsDataSet();
//            ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
//            tempListOfKeys.sort(new org.davverotvdownloader2.parsers.VideoSizeComparator());
//
//            String tempKey = tempListOfKeys.get(0);
//            String tempUrl = parsedDetailsDataSet.getUrlsGrabbed()
//                    .getChunkListPrefixSrcString()+
//                    parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

            if (cbAutoCopy.isSelected()) {
                copiaLowResUrlUI(parsedDetailsDataSet);
            }

            if (cbCreaClawljob.isSelected()) {
                String crawljobContent = generateLowResJDFW(parsedDetailsDataSet);
                if (crawljobContent != null) {
                    String crawljobFilename = parsedDetailsDataSet.getMainParametersUrlGrabbed().getPageTitle();
                    if (crawljobFilename.trim().equalsIgnoreCase("")) {
                        crawljobFilename = "crawljob"; // TODO: Fare meglio
                    }
                    saveLowResJDFW(crawljobFilename, crawljobContent);
                }
            }

            generateAndLogDownThemAllPattern(parsedDetailsDataSet, true);

        }
    }

    private String generateAndLogDownThemAllPattern(ParsedDetailsDataSet parsedDetailsDataSet, boolean logToTextArea) {
        HashMap<String, ArrayList<String>> tempMapResChunks = parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist();

        StringBuilder sbRetval = new StringBuilder();

        if (logToTextArea) {
            txtLog.append("Genero le url per DownThemAll");
            txtLog.append("\n");
        }

        sbRetval.append("Comando ffmpeg per riassemblare i file in un unico video:");
        sbRetval.append("\n");
        sbRetval.append("ffmpeg -f concat -i LISTA_CHUNKS.txt -c copy OUTFILE.mp4");
        sbRetval.append("\n");

        for (String keyResolutionSize : tempMapResChunks.keySet()) {
            sbRetval.append("\n");
            sbRetval.append(keyResolutionSize);
            sbRetval.append("\n");

            ArrayList<String> chunks = tempMapResChunks.get(keyResolutionSize);
            // Presuppongo che i chunks siano consecutivi per cui ottimizzo e considero solo il loro numero complessivo
            int latestChunk = chunks.size();
            String template = chunks.get(0);
            String[] splitted = template.split("-");
            splitted[1] = "[1:" + latestChunk + "]";
            String joined;
            StringBuilder sb = new StringBuilder();

            // Ricostruisco la stringa
            for (int i = 0; i < splitted.length - 1; i++) {
                String s = splitted[i];
                sb.append(s);
                sb.append("-");
            }
            sb.append(splitted[splitted.length - 1]); // parte finale

            // Stampo l'url comlleta del il file chunklist
            sbRetval.append(parsedDetailsDataSet.getMainParametersUrlGrabbed().getChunkListPrefixSrcString());
            sbRetval.append(sb);
            sbRetval.append("\n");

            // Aggiungo i chunklist solo per il file finale
            sbRetval.append("Lista CHUNKS per ffmpeg (da copiare in un file LISTA_CHUNKS.txt):");
            sbRetval.append("\n");
            for (String chunk : chunks) {
                sbRetval.append("file '");
                sbRetval.append(chunk);
                sbRetval.append("'");
                sbRetval.append("\n");
            }
        }

        if (logToTextArea) {
            txtLog.append(sbRetval.toString());
        }

        return sbRetval.toString();
    }


    /**
     * Genera il contenuto del file crawljob
     *
     * @param parsedDetailsDataSet
     * @return
     */
    private String generateLowResJDFW(ParsedDetailsDataSet parsedDetailsDataSet) {
        if (parsedDetailsDataSet == null || parsedDetailsDataSet.getMappaRisoluzioniChunklist().size() == 0) {
            JOptionPane.showMessageDialog(MainPanel, "Nessun dato per generare il file per JDownloader", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        StringBuilder sb = new StringBuilder();

        String lineSep = System.lineSeparator();

        sb.append("autoStart=TRUE").append(lineSep)
                .append("enabled=TRUE").append(lineSep)
                .append("autoStart=TRUE").append(lineSep)
                .append("autoConfirm=TRUE").append(lineSep)
                .append("overwritePackagizerEnabled=TRUE").append(lineSep)
                .append("priority=DEFAULT").append(lineSep)
                .append("forcedStart=UNSET").append(lineSep);

        // Se arrivo qui allora la mappa delle risoluzioni ha almeno un elemento
        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        // Anche se c'è stato un errore nel download (es. cloudflare) questa parte non genera un'eccezione (IndexOutOfBoundsException)
        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getMainParametersUrlGrabbed()
                .getChunkListPrefixSrcString() +
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        sb.append("text=").append(tempUrl).append(lineSep);

        String stringToBeCopied = parsedDetailsDataSet.getMainParametersUrlGrabbed().getPageTitle() + "\n";

        sb.append("packageName=").append(stringToBeCopied).append(lineSep);

        txtLog.append("---- newEntry.crawljob START ----");
        txtLog.append(lineSep);
        txtLog.append("Copia questo contenuto in un file .crawljob nella directory folderwatch di JD. Il download inizierà subito");
        txtLog.append(sb.toString());
        //txtLog.append(lineSep);
        txtLog.append("---- newEntry.crawljob END ----");
        txtLog.append(lineSep);
        return sb.toString();
    }


    /**
     * Salva il file per la cartella folderwatch di JDownloader
     *
     * @param filenameWithoutPathAndExtension
     * @param crawlJobContent
     */
    private void saveLowResJDFW(String filenameWithoutPathAndExtension, String crawlJobContent) {
        String lineSep = System.lineSeparator();

        //String crawjjobContent = generateLowResJDFW(parsedDetailsDataSet);

//        if (parsedDetailsDataSet == null || parsedDetailsDataSet.getMappaRisoluzioniChunklist().size() == 0) {
//            JOptionPane.showMessageDialog(MainPanel, "Nessun dato per generare il file per JDownloader", "Attenzione", JOptionPane.WARNING_MESSAGE);
//            return;
//        }
//
//        StringBuilder sb = new StringBuilder();
//
//
//        sb.append("autoStart=TRUE").append(lineSep)
//                .append("enabled=TRUE").append(lineSep)
//                .append("autoStart=TRUE").append(lineSep)
//                .append("autoConfirm=TRUE").append(lineSep)
//                .append("overwritePackagizerEnabled=TRUE").append(lineSep)
//                .append("priority=DEFAULT").append(lineSep)
//                .append("forcedStart=UNSET").append(lineSep);
//
//        // Se arrivo qui allora la mappa delle risoluzioni ha almeno un elemento
//        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
//        tempListOfKeys.sort(new VideoSizeComparator());
//
//        // Anche se c'è stato un errore nel download (es. cloudflare) questa parte non genera un'eccezione (IndexOutOfBoundsException)
//        String tempKey = tempListOfKeys.get(0);
//        String tempUrl = parsedDetailsDataSet.getMainPatametersUrlGrabbed()
//                .getChunkListPrefixSrcString() +
//                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);
//
//        sb.append("text=").append(tempUrl).append(lineSep);
//
//
//        sb.append("packageName=").append(stringToBeCopied).append(lineSep);
//
//        txtLog.append("---- newEntry.crawljob START ----");
//        txtLog.append(lineSep);
//        txtLog.append("Copia questo contenuto in un file .crawljob nella directory folderwatch di JD. Il download inizierà subito");
//        txtLog.append(sb.toString());
//        //txtLog.append(lineSep);
//        txtLog.append("---- newEntry.crawljob END ----");
//        txtLog.append(lineSep);

        //String stringToBeCopied = parsedDetailsDataSet.getMainPatametersUrlGrabbed().getPageTitle() + "\n";
        try {
            String filenameTemp = filenameWithoutPathAndExtension.replaceAll("[/\\\\:*?\"<>|]", "_").trim();
            filenameTemp += ".crawljob";
            // Prendo da AppPrefs per evitare che dopo la scelta della folder ci possa essere una sovrascrittura nel campo txtCrawljobSavePath
            File file = new File(
                    AppPrefs.SaveFolderLocation.get() +
                            File.separator +
                            filenameTemp
            );

            FileOutputStream fos = new FileOutputStream(file);
            fos.write(crawlJobContent.getBytes(StandardCharsets.UTF_8));
            fos.close();

            txtLog.append("Scritto il file crawljob: ");
            txtLog.append(lineSep);
            txtLog.append("  ");
            txtLog.append(file.getAbsolutePath());
            txtLog.append(lineSep);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void copiaLowResUrlUI(ParsedDetailsDataSet parsedDetailsDataSet) {

        if (parsedDetailsDataSet == null || parsedDetailsDataSet.getMappaRisoluzioniChunklist().size() == 0) {
            JOptionPane.showMessageDialog(MainPanel, "Nessun dato da copiare", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se arrivo qui allora la mappa delle risoluzioni ha almeno un elemento
        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        // Anche se c'è stato un errore nel download (es. cloudflare) questa parte non genera un'eccezione (IndexOutOfBoundsException)
        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getMainParametersUrlGrabbed()
                .getChunkListPrefixSrcString() +
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (cbCopyTitle.isSelected()) {
            stringToBeCopied = parsedDetailsDataSet.getMainParametersUrlGrabbed().getPageTitle() + "\n";
        }
        stringToBeCopied += tempUrl;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(stringToBeCopied);
        clipboard.setContents(stringSelection, null);
        JOptionPane.showMessageDialog(MainPanel,
                "Risoluzione: " + tempKey + "\nUrl copiata nella clipboard: " + tempUrl,
                "Dati copiati",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private static void copiaLowResUrlConsolle(ParsedDetailsDataSet parsedDetailsDataSet, boolean copytitle) {

        if (parsedDetailsDataSet == null) {
            System.out.println("Nessun dato da copiare");
            return;
        }

        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getMainParametersUrlGrabbed()
                .getChunkListPrefixSrcString() +
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (copytitle) {
            stringToBeCopied = parsedDetailsDataSet.getMainParametersUrlGrabbed().getPageTitle() + "\n";
        }
        stringToBeCopied += tempUrl;
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection stringSelection = new StringSelection(stringToBeCopied);
        clipboard.setContents(stringSelection, null);
        System.out.println(
                "Risoluzione: " + tempKey + "\nUrl copiata nella clipboard: " + tempUrl
        );
    }

}
