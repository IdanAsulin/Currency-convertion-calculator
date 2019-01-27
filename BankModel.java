package il.ac.shenkar;

import org.w3c.dom.NodeList;
import java.util.HashMap;
import java.util.Map;

/** This interface defines the methods which all kind of bank classes should implement in order to provide
 * the ability to stay updated, do the appropriate calculations and do the XML analyze
 */
public interface BankModel {
    /** Will hold a map of Key-Value of currencyNames-rate */
    Map<String, Double> exchangeMap = new HashMap<>();
    /** Will hold a map of Key-Value of currencyNames-units */
    Map<String, Integer> unitsMap = new HashMap<>();

    /** A static method which does the calculation of a given input
     * from - will hold a string represents the currency from which we want to convert
     * to - will hold a string represents the currency to which we want to convert
     * amount - will hold a double represents the amount of money we want to exchange
     */
    static double calculate(String from, String to, double amount) {
        if(amount == -1) // A flag for the caller which give a sign the input isn't legal for write error on screen
            return -1;
        double result, MiddleRate;
        MiddleRate = (amount * exchangeMap.get(from)) / unitsMap.get(from) ; // Middle conversion
        result = MiddleRate / exchangeMap.get(to);
        return result;
    }

    /** This method will connect and read Bank's XML file and will download a new file only in case
     * the bank has changed the exchange rates.
     * Will also build the exchangeMap & unitsMap if needed.
     * If needed also refresh gui on EDT thread
     */
    void downloadNewXmlFile(Gui gui) throws CurrencyException;

    /*** Will return a list of all "key" tags from the local XML file which downloaded from the bank API
     * key - tag name from which we want to build the returned list
     */
    NodeList readLocalXml(String key);

    /** Create a new thread which always run in background and every
     * one hour will check for new data on the Bank API
     */
    void checkForUpdates(Gui gui) throws InterruptedException, CurrencyException;
}