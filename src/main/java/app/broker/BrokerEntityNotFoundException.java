package app.broker;

public class BrokerEntityNotFoundException extends RuntimeException{
    public BrokerEntityNotFoundException(String message) {
        super(message);
    }
}
