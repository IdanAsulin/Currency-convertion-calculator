package il.ac.shenkar;

import org.apache.log4j.Logger;
import org.apache.log4j.FileAppender;
import org.apache.log4j.SimpleLayout;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;


/** A demo class which contains only the main method that run the program */
public class SystemDemo {

    private static Gui gui;
    private static final Logger logger = Logger.getLogger(SystemDemo.class.getName());
    private static BankModel bank = new BankOfIsrael();

    static {
        try {
            logger.addAppender(new FileAppender(new SimpleLayout(), "logs.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Main method
     */
    public static void main(String... args) {
        logger.info("Start running ... ");
        Runnable guiRunnable = () -> {
            gui = new Gui();
        };
        try {
            SwingUtilities.invokeAndWait(guiRunnable);
            bank.checkForUpdates(gui);
        } catch (InterruptedException | InvocationTargetException | CurrencyException e) {
            logger.info(e.getMessage());
            System.exit(1);
        }
    }
}