package jo.recording.event;

import com.ib.client.Types.DeepSide;
import com.ib.client.Types.DeepType;

public class MarketDepthEvent extends BaseEvent {
    private int position;
    private String marketMaker;
    private DeepType operation;
    private DeepSide side;
    private double price;
    private int size;

    public MarketDepthEvent(int position, String marketMaker, DeepType operation, DeepSide side, double price, int size) {
        super("MarketDepth");
        this.position = position;
        this.marketMaker = marketMaker;
        this.operation = operation;
        this.side = side;
        this.price = price;
        this.size = size;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getMarketMaker() {
        return marketMaker;
    }

    public void setMarketMaker(String marketMaker) {
        this.marketMaker = marketMaker;
    }

    public DeepType getOperation() {
        return operation;
    }

    public void setOperation(DeepType operation) {
        this.operation = operation;
    }

    public DeepSide getSide() {
        return side;
    }

    public void setSide(DeepSide side) {
        this.side = side;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

}
