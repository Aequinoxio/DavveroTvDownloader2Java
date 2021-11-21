import org.apache.commons.cli.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    ParsedDetailsDataSet parsedDetailsDataSet;
    public MainForm() {
        btnStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ParserWorker parserWorker = new ParserWorker();
                parserWorker.execute();
            }
        });
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
    }

    public static void main(String[] args) {

        Options options = new Options();
        options.addOption("h","help",false,"Help");
        options.addOption("g", "gui",false,"Avvia l'interfaccia grafica");
        options.addOption("n", "nogui",true,"Avvia il download in consolle usando l'url passata per argomento");
        options.addOption("c","copy" ,true,"Vale solo se è attivata l'opzione -n.\nCopia l'url relativa alla risoluzione più bassa nella clipboard al termine del parsing. Se l'argomento è 'true' o '1' allora copia anche il titolo altrimenti solo l'url.");

        CommandLineParser commandLineParser = new DefaultParser();
        try {
            CommandLine commandLine = commandLineParser.parse(options,args);
            if (commandLine.hasOption("h")){
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DavveroTVVieoDoenloader2",options,true);
                return;
            }

            if (commandLine.hasOption("x") || commandLine.getOptions().length==0){
                JFrame frame = new JFrame("MainForm");
                frame.setContentPane(new MainForm().MainPanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } else if (commandLine.hasOption("c")){
                boolean copyInClipboard = commandLine.hasOption("m");
                boolean copyTitleInClipboard = false ;
                if (copyInClipboard){
                    copyTitleInClipboard = "true".equals(commandLine.getOptionValue("m").trim()) ||
                            "1".equals(commandLine.getOptionValue("m").trim());
                }

                String urlString = commandLine.getOptionValue("c").trim();
                System.out.println("Parsing url: "+urlString+"\n");

                LoaderParserDavveroTv loaderParserDavveroTv = new LoaderParserDavveroTv(new WorkerUpdateCallback() {
                    @Override
                    public void updateFromWorker(String messaggio) {
                        System.out.println(messaggio);
                    }
                });

                loaderParserDavveroTv.caricaPaginaInizialeEGeneraVideoUrl(urlString);

                System.out.println(stringifyAllData(
                        loaderParserDavveroTv.getParsedDetailsDataSet()
                ));

                if (copyInClipboard){
                    copiaLowResUrlConsolle(loaderParserDavveroTv.getParsedDetailsDataSet(), copyTitleInClipboard);
                }
            }
        } catch (ParseException ma){
            System.out.println(ma.getLocalizedMessage());
        }
    }

    static String stringifyAllData(ParsedDetailsDataSet parsedDetailsDataSet){
        StringBuilder sb = new StringBuilder();
        // parsedDetailsDataSet=loaderParserDavveroTv.getParsedDetailsDataSet();

        if (parsedDetailsDataSet.getMappaRisoluzioniChunklist().size()==0){
            sb.append("Qualcosa è andato storto");
            return sb.toString();
        }
        // Loggo quanto ho trovato nella textarea dedicata
        sb.append("Titolo della pagina: ").append(parsedDetailsDataSet.getUrlsGrabbed().getPageTitle());sb.append("\n");
        sb.append("Url per la playlist risoluzioni-chunklist");sb.append("\n");
        sb.append(parsedDetailsDataSet
                .getUrlsGrabbed().getChunkListPrefixSrcString());sb.append("\n");

        sb.append("Stampo la mappa risoluzioni-chunklists");sb.append("\n");
        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());
        String tempUrl;
        for (String chiave : tempListOfKeys){
            tempUrl = parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(chiave);

            sb.append("\t"+chiave + " - " + tempUrl+ " dim stimate: "+
                    parsedDetailsDataSet.getMappaRisoluzioniSegmentiChunklist().get(chiave).size()*
                            parsedDetailsDataSet.getUrlsGrabbed()
                                    .getChunkEstimatedSizePerResolution().get(chiave));sb.append("\n");
            sb.append("\t\turl completa: "+ parsedDetailsDataSet.getUrlsGrabbed()
                    .getChunkListPrefixSrcString()+tempUrl
            );sb.append("\n");
        }

        return sb.toString();
    }

//    private void createUIComponents() {
//        // TODO: place custom component creation code here
//    }


    private class ParserWorker extends SwingWorker<ParsedDetailsDataSet,String>{
        LoaderParserDavveroTv loaderParserDavveroTv ;
        @Override
        protected ParsedDetailsDataSet doInBackground() {
            loaderParserDavveroTv = new LoaderParserDavveroTv(messaggio -> publish(messaggio));
            String mainSrc = txtMainUrl.getText().trim();
            loaderParserDavveroTv.caricaPaginaInizialeEGeneraVideoUrl(mainSrc);

            return loaderParserDavveroTv.getParsedDetailsDataSet();
        }

        @Override
        protected void process(List<String> chunks) {
            super.process(chunks);
            for (String chunk:chunks){
                txtLog.append(chunk);
                txtLog.append("\n");
            }
        }

        @Override
        protected void done() {
            super.done();

            txtLog.append(stringifyAllData(
                    loaderParserDavveroTv.getParsedDetailsDataSet()
            ));

            parsedDetailsDataSet = loaderParserDavveroTv.getParsedDetailsDataSet();

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

        if (parsedDetailsDataSet==null){
            JOptionPane.showMessageDialog(MainPanel,"Nessun dato da copiare", "Attenzione", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getUrlsGrabbed()
                .getChunkListPrefixSrcString()+
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (cbCopyTitle.isSelected()){
            stringToBeCopied = parsedDetailsDataSet.getUrlsGrabbed().getPageTitle()+"\n";
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

        if (parsedDetailsDataSet==null){
            System.out.println("Nessun dato da copiare");
            return;
        }

        ArrayList<String> tempListOfKeys = new ArrayList<>(parsedDetailsDataSet.getMappaRisoluzioniChunklist().keySet());
        tempListOfKeys.sort(new VideoSizeComparator());

        String tempKey = tempListOfKeys.get(0);
        String tempUrl = parsedDetailsDataSet.getUrlsGrabbed()
                .getChunkListPrefixSrcString()+
                parsedDetailsDataSet.getMappaRisoluzioniChunklist().get(tempKey);

        String stringToBeCopied = "";
        if (copytitle){
            stringToBeCopied = parsedDetailsDataSet.getUrlsGrabbed().getPageTitle()+"\n";
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
