package jo.bot;

public enum BotState {
    READY_TO_OPEN,
    PENDING, // created orders, but they didn't reach IB yet
    OPENNING_POSITION,
    PROFIT_WAITING;
}
