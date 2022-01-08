import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class HelpDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JTextPane txtPaneHelp;

    public HelpDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        BufferedReader br = new BufferedReader(new InputStreamReader(this.getClass().getResourceAsStream("/about.html")));
        String linea;
        StringBuilder sb = new StringBuilder();
        try {
            while ((linea=br.readLine())!=null){
                sb.append(linea);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        txtPaneHelp.setText(sb.toString());

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
    }

    private void onOK() {
        // add your code here
        dispose();
    }

    public static void main(String[] args) {
        HelpDialog dialog = new HelpDialog();
        dialog.pack();
        dialog.setVisible(true);
        System.exit(0);
    }
}
