package il.ac.shenkar;

/** This class represents a new type of exception */
class CurrencyException extends Exception {

    /** Constructor */
    CurrencyException(String message) {
        super(message);
    }
}