import org.apache.commons.cli.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.ArrayList;
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

    private enum ParserType {
        DavveroTV,
        ByoBlu
    }


    ParserType parserType = ParserType.ByoBlu;

    ParsedDetailsDataSet parsedDetailsDataSet;

    public MainForm() {

        pbLoading.setVisible(true);
        pbLoading.setIndeterminate(false);

        Action action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (byoBluParserRadioButton.isSelected()){
                    parserType=ParserType.ByoBlu;
                }
                if (davveroTVParserRadioButton.isSelected()){
                    parserType=ParserType.DavveroTV;
                }

                ParserWorker parserWorker = new ParserWorker(parserType);
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

    static String stringifyAllData(ParsedDetailsDataSet parsedDetailsDataSet) {
        StringBuilder sb = new StringBuilder();
        // parsedDetailsDataSet=loaderParserDavveroTv.getParsedDetailsDataSet();

        if (parsedDetailsDataSet.getMappaRisoluzioniChunklist().size() == 0) {
            sb.append("Qualcosa è andato storto");
            return sb.toString();
        }
        // Loggo quanto ho trovato nella textarea dedicata
        sb.append("Titolo della pagina: ").append(parsedDetailsDataSet.getMainPatametersUrlGrabbed().getPageTitle());
        sb.append("\n");
        sb.append("Url per la playlist risoluzioni-chunklist");
        sb.append("\n");
        sb.append(parsedDetailsDataSet
                .getMainPatametersUrlGrabbed().getChunkListPrefixSrcString());
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
                            parsedDetailsDataSet.getMainPatametersUrlGrabbed()
                                    .getChunkEstimatedSizePerResolution().get(chiave));
            sb.append("\n");
            sb.append("\t\turl completa: " + parsedDetailsDataSet.getMainPatametersUrlGrabbed()
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

            // TODO: migliorare in caso si usassero più di due parser
            if (parserType==ParserType.ByoBlu) {
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

            txtLog.append(stringifyAllData(
                    loaderParser.getParsedDetailsDataSet()
            ));

            parsedDetailsDataSet = loaderParser.getParsedDetailsDataSet();

//            ParsedDetailsDataSet parsedDetailsDataSet = loaderParserDavveroTv.getParsedDetailsDataSet();
//            ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
//            tempListOfKeys.sort(new VideoSizeComparator());
//
//            String tempKey = tempListOfKeys.get(0);
//            String tempUrl = parsedDetailsDataSet.getUrlsGrabbed()
//                    .getChunkListPrefixSrcString()+
//                    parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

            if (cbAutoCopy.isSelected()) {
                copiaLowResUrlUI(parsedDetailsDataSet);
            }

//            ParsedDetailsDataSet parsedDetailsDataSet=loaderParserDavveroTv.getParsedDetailsDataSet();
//
//            if (parsedDetailsDataSet.getMappaRisoluzioniChunklist().size()==0){
//                txtLog.append("Qualcosa è andato storto");
//                return;
//            }
//            // Loggo quanto ho trovato nella textarea dedicata
//            txtLog.append("Url per la playlist risoluzioni-chunklist");txtLog.append("\n");
//            txtLog.append(loaderParserDavveroTv.getParsedDetailsDataSet()
//                    .getUrlsGrabbed().getChunkListPrefixSrcString());txtLog.append("\n");
//
//
//            txtLog.append("Stampo la mappa risoluzioni-chunklists");txtLog.append("\n");
//            ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
//            tempListOfKeys.sort(new VideoSizeComparator());
//            String tempUrl;
//            for (String chiave : tempListOfKeys){
//                tempUrl = parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(chiave);
//
//                txtLog.append("\t"+chiave + " - " + tempUrl+ " dim stimate: "+
//                        parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist().get(chiave).size()*
//                                loaderParserDavveroTv.getParsedDetailsDataSet().getUrlsGrabbed()
//                                        .getChunkEstimatedSizePerResolution().get(chiave));txtLog.append("\n");
//                txtLog.append("\t\turl completa: "+ loaderParserDavveroTv.getParsedDetailsDataSet().getUrlsGrabbed()
//                        .getChunkListPrefixSrcString()+tempUrl
//                );txtLog.append("\n");
//            }
        }
    }

    private void copiaLowResUrlUI(ParsedDetailsDataSet parsedDetailsDataSet) {

        if (parsedDetailsDataSet == null || parsedDetailsDataSet.getMappaRisoluzioniChunklist().size()==0) {
            JOptionPane.showMessageDialog(MainPanel, "Nessun dato da copiare", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Se arrivo qui allora la mappa delle risoluzioni ha aleno un elemento
        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        // Anche se c'è stato un errore nel download (es. cloudflare) questa parte non genera un'eccezione (IndexOutOfBoundsException)
        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getMainPatametersUrlGrabbed()
                .getChunkListPrefixSrcString() +
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (cbCopyTitle.isSelected()) {
            stringToBeCopied = parsedDetailsDataSet.getMainPatametersUrlGrabbed().getPageTitle() + "\n";
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
        String tempUrl = parsedDetailsDataSet.getMainPatametersUrlGrabbed()
                .getChunkListPrefixSrcString() +
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (copytitle) {
            stringToBeCopied = parsedDetailsDataSet.getMainPatametersUrlGrabbed().getPageTitle() + "\n";
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
