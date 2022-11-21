package app.broker;

public interface CommandLoadable {
    void loadFromCommand(RequestCommand command);
}
