package il.ac.shenkar;

import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 * This class represents the "Bank Of Israel" from which the system pull all the exchange data.
 */
class BankOfIsrael implements BankModel {

    private static Logger logger = Logger.getLogger(BankOfIsrael.class.getName());

    static {
        try {
            logger.addAppender(new FileAppender(new SimpleLayout(), "logs.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Constructor */
    BankOfIsrael() {
        super();
    }

    /** This method will connect and read Bank's XML file and will download a new file only in case
     * the bank has changed the exchange rates.
     * Will also build the exchangeMap & unitsMap if needed.
     * If needed also refresh gui on the EDT thread
     * BankModel interface implementation
     */
    public void downloadNewXmlFile(Gui gui) throws CurrencyException {
        try {
            URL apiUrl = new URL("https://www.boi.org.il/currency.xml"); // Represent the XML file from the Bank Of Israel API
            URLConnection conn = apiUrl.openConnection();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(conn.getInputStream());
            NodeList list = doc.getElementsByTagName("LAST_UPDATE");
            String NewDate = list.item(0).getTextContent();

            if(!(gui.getDateText().getText()).equals(NewDate)) { // Check if we need to download a new file
                TransformerFactory tfactory = TransformerFactory.newInstance();
                Transformer xform = tfactory.newTransformer();
                File myOutputFile = new File("boi.xml");
                xform.transform(new DOMSource(doc), new StreamResult(myOutputFile)); // Saving file in a local directory
                NodeList currencyCodeList = doc.getElementsByTagName("CURRENCYCODE");
                NodeList rateList = doc.getElementsByTagName("RATE");
                NodeList unitList = doc.getElementsByTagName("UNIT");
                NodeList countriesList = doc.getElementsByTagName("COUNTRY");

                exchangeMap.put("ILS - Israel", 1.0); // Add the ILS rates - doesn't appear in the bank's XML
                unitsMap.put("ILS - Israel", 1);

                for(int i = 0; i < currencyCodeList.getLength(); ++i) { // Map of key-value of currenciesCode-rates & units-rates
                    exchangeMap.put(currencyCodeList.item(i).getTextContent() + " - " + countriesList.item(i).getTextContent(), Double.parseDouble(rateList.item(i).getTextContent()));
                    unitsMap.put(currencyCodeList.item(i).getTextContent() + " - " + countriesList.item(i).getTextContent(), Integer.parseInt(unitList.item(i).getTextContent()));
                }
            }
        }
        catch (TransformerException | IOException | ParserConfigurationException | SAXException e) {
            logger.info(e.getMessage());
            throw new CurrencyException("An error was occurred with downloading file");
        }

        Runnable refreshData = () -> {
            try {
                gui.refreshTableData(this.getNewTableData());
                gui.refreshCurrenciesDropDown(this.getNewCurrencies());
                gui.refreshLastUpdateDate(this.getNewDate());
            }
            catch (CurrencyException e) {
                logger.info(e.getMessage());
                System.exit(1);
            }
        };
        SwingUtilities.invokeLater(refreshData);  // Refresh the UI with the new data on EDT thread
    }

    /** Create a new thread which always run in the background and every
     * one hour will check for new data on the Bank Of Israel API
     * BankModel interface implementation
     */
    public void checkForUpdates(Gui gui) {
        Runnable refreshData = ()-> {
            while(true) {
                try {
                    this.downloadNewXmlFile(gui);
                    TimeUnit.HOURS.sleep(1);
                } catch (InterruptedException | CurrencyException e) {
                    logger.info(e.getMessage());
                    System.exit(1);
                }
            }
        };
        Thread refreshDataThread = new Thread(refreshData); // Create a new thread which always run in background and every one hour will check for new data on the BankOfIsrael API
        refreshDataThread.start();
    }

    /*** A method for internal use - Will return a list of all "key" tags from the local XML file which downloaded from the bank API
     * key - tag name from which we want to build the returned list
     * BankModel interface implementation
     */
    public NodeList readLocalXml(String key) {
        NodeList nList;
        try {
            File fXmlFile = new File("boi.xml"); // The API route where the XML locate
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            nList = doc.getElementsByTagName(key); // Get a list of all tags of type "key" from the local XML
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            logger.info(e.getMessage());
            return null;
        }
        return nList;
    }

    /** Private method for internal use only - will return a string represents the date from the local XML */
     private String getNewDate() throws CurrencyException {
        NodeList lastUpdate = this.readLocalXml("LAST_UPDATE");
        if(lastUpdate == null) {
            logger.info("Read local xml file has failed");
            throw new CurrencyException("Read local xml file has failed");
        }

        return lastUpdate.item(0).getTextContent();
    }

    /** Private method for internal use only - Will return an array of table rows from the local XML */
     private String[][] getNewTableData() throws NullPointerException {
        NodeList NameList = this.readLocalXml("CURRENCYCODE"),
                CountryList = this.readLocalXml("COUNTRY"),
                RateList = this.readLocalXml("RATE"),
                UnitsList = this.readLocalXml("UNIT"),
                CurrencyNameList = this.readLocalXml("NAME"),
                ChangeList = this.readLocalXml("CHANGE");

        if(NameList == null || CountryList == null || RateList == null || CurrencyNameList == null || ChangeList == null || UnitsList == null) {
            logger.info("Read local xml file has failed");
            throw new NullPointerException("Read local xml file has failed");
        }

        String[][] tableData = new String[NameList.getLength() + 1][6]; // An array of all new table raws

        for(int i = 0; i < CurrencyNameList.getLength(); ++i)
            tableData[i][0] = CurrencyNameList.item(i).getTextContent();

        for(int i = 0; i < CountryList.getLength(); ++i)
            tableData[i][1] = CountryList.item(i).getTextContent();

        for(int i = 0; i < NameList.getLength(); ++i)
            tableData[i][2] = NameList.item(i).getTextContent();

        for(int i = 0; i < RateList.getLength(); ++i)
            tableData[i][3] = RateList.item(i).getTextContent();

        for(int i = 0; i < UnitsList.getLength(); ++i)
            tableData[i][4] = UnitsList.item(i).getTextContent();

        for(int i = 0; i < ChangeList.getLength(); ++i)
            tableData[i][5] = ChangeList.item(i).getTextContent();

        tableData[NameList.getLength()][0] = "NIS";   // Add of ILS data which doesn't appear in the XML file
        tableData[NameList.getLength()][1] = "Israel";
        tableData[NameList.getLength()][2] = "ILS";
        tableData[NameList.getLength()][3] = "1";
        tableData[NameList.getLength()][4] = "1";
        tableData[NameList.getLength()][5] = "0";

        return tableData;
    }

    /** Private method for internal use only - Will return a currencies array for the drop down list */
     private String[] getNewCurrencies() throws NullPointerException {
        NodeList NameList = this.readLocalXml("CURRENCYCODE");
        NodeList CountryList = this.readLocalXml("COUNTRY");

        if(NameList == null || CountryList == null) {
            logger.info("Read local xml file has failed");
            throw new NullPointerException("Read local xml file has failed");
        }

        String[] currencies = new String[NameList.getLength()]; // An array of all new currencies

        for(int i = 0; i < NameList.getLength();  i++)
            currencies[i] = NameList.item(i).getTextContent() + " - " + CountryList.item(i).getTextContent();

        return currencies;
    }
}