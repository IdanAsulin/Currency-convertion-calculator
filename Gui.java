package il.ac.shenkar;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/** This class represents the UI which will be display on the user screen.
 * all currencies data will refresh automatically
 */
class Gui {
    private static Logger logger = Logger.getLogger(Gui.class.getName());
    /** Table headers */
    private static String[] columnsName = {"Currency", "Country", "Code", "Rate", "Units", "Change"};
    private JFrame frame;
    private JTable table;
    private JTextField amountTextField, resultTextField;
    /** Currencies dropdown lists */
    private JComboBox<String> fromCurrency, toCurrency;
    private JLabel dateText;

    static {
        try {
            logger.addAppender(new FileAppender(new SimpleLayout(), "logs.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Constructor - build the UI and set all actions listener */
    Gui() {
        JScrollPane scrollPane; // Table scroll bar
        JButton button; //Convert button
        JLabel fromLabel, toLabel, lastUpdate;
        JPanel panelTop, panelBottom;
        String[][] tableData = {};
        frame = new JFrame();
        frame.setTitle("Currency exchange system");
        table = new JTable(tableData, columnsName);
        table.setFont(new Font("SansSerif", Font.PLAIN, 15));
        table.getTableHeader().setFont(new Font("SansSerif", Font.PLAIN, 17));
        table.setRowHeight(30);
        scrollPane = new JScrollPane(table);
        button = new JButton("Convert !");
        button.addActionListener(e -> { // When a user push the convert button, do all calculates and refresh the result on screen
            String from = (String)fromCurrency.getSelectedItem();
            String to = (String)toCurrency.getSelectedItem();
            String amountText = amountTextField.getText();
            double amount, result;
            try {
                amount = Double.parseDouble(amountText);
            } catch (NumberFormatException ex) { amount = -1; }
            result = BankModel.calculate(from,to,amount);

            Runnable refreshResult = () -> {
                resultTextField.removeAll();
                if (result == -1)
                    resultTextField.setText("Error");
                else
                    resultTextField.setText(String.valueOf(result));
            };
            SwingUtilities.invokeLater(refreshResult); // Refresh the calculation result on screen on the EDT thread
        });
        amountTextField = new JTextField(16);
        resultTextField = new JTextField(16);
        resultTextField.setEditable(false);
        dateText = new JLabel();
        dateText.setFont(new Font("SansSerif", Font.PLAIN, 17));
        fromCurrency = new JComboBox<>();
        toCurrency = new JComboBox<>();
        lastUpdate = new JLabel("Last Update:");
        fromLabel = new JLabel("From:");
        toLabel = new JLabel("To:");
        panelTop = new JPanel();
        panelTop.add(lastUpdate);
        panelTop.add(dateText);
        panelBottom = new JPanel();
        panelBottom.add(amountTextField);
        panelBottom.add(fromLabel);
        panelBottom.add(fromCurrency);
        panelBottom.add(toLabel);
        panelBottom.add(toCurrency);
        panelBottom.add(button);
        panelBottom.add(resultTextField);
        frame.add(panelTop, BorderLayout.NORTH);
        frame.add(panelBottom, BorderLayout.SOUTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.setSize(1000,700);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) { // Set an alert message before exit
                if (JOptionPane.showConfirmDialog(frame,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
                    logger.info("User chose to leave...");
                    System.exit(0); // If the user selects "yes" then finish the program
                }
            }
        });
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    /** Return the dateText JLabel  */
    JLabel getDateText() {
        return dateText;
    }

    /** Get an array of table raws which contains a new data and refresh the UI table  */
    void refreshTableData(String[][] dataRows) throws CurrencyException {
        if(dataRows.length <= 0) {
            logger.info("TableData array is empty can not refresh the data");
            throw new CurrencyException("TableData array is empty can not refresh the data");
        }
        DefaultTableModel model = new DefaultTableModel(dataRows, columnsName) {
            @Override
            public boolean isCellEditable(int row, int column) { // Set all table cells as editable = false
                return false;
            }
        };
        table.setModel(model);
    }

    /** Get an array of currencies names and refresh the UI dropdown lists */
    void refreshCurrenciesDropDown(String[] currencies) throws CurrencyException {
        if(currencies.length <= 0) {
            logger.info("Currencies array is empty can not refresh the data");
            throw new CurrencyException("Currencies array is empty can not refresh the data");
        }
        fromCurrency.removeAllItems(); // Remove all items before adding the new items
        toCurrency.removeAllItems();
        fromCurrency.addItem("ILS - Israel"); // Add the ILS data which isn't in the XML
        toCurrency.addItem("ILS - Israel");
        for (String currency : currencies) {
            fromCurrency.addItem(currency);
            toCurrency.addItem(currency);
        }
    }

    /** Get newDate and refresh the UI date field */
    void refreshLastUpdateDate(String newDate) throws NullPointerException {
        if(newDate == null) {
            logger.info("newDate is null, can't refresh data");
            throw new NullPointerException();
        }
        dateText.removeAll();
        dateText.setText(newDate);
    }
}