package jo.signal;

import com.ib.client.Contract;
import com.ib.client.Types.BarSize;

import gnu.trove.list.array.TDoubleArrayList;
import jo.app.IApp;
import jo.model.MarketData;

// High\ Low / High shape
public class BarShapeHLHRestriction implements Signal {
    private TDoubleArrayList open;
    private TDoubleArrayList close;

    public BarShapeHLHRestriction() {

    }

    @Override
    public boolean isActive(IApp app, Contract contract, MarketData marketData) {
        if (close == null) {
            open = marketData.getBars(BarSize._5_secs).getOpen();
            close = marketData.getBars(BarSize._5_secs).getClose();
        }

        int size = close.size();
        if (size < 3) {
            return false;
        }

        int end = size - 1;

        int leftIdx = end - 2;
        int midIdx = end - 1;
        int rightIdx = end;

        double leftOpen = open.get(leftIdx);
        double leftClose = close.get(leftIdx);
        double midOpen = open.get(midIdx);
        double midClose = close.get(midIdx);
        double rightOpen = open.get(rightIdx);
        double rightClose = close.get(rightIdx);

        double leftMidDiff = leftOpen - midClose;
        double rightMidDiff = rightClose - midClose;

        // System.out.println("leftMidDiff: " + leftMidDiff);
        // System.out.println("rightMidDiff: " + rightMidDiff);

        System.out.println(String.format("LO %.2f > %.2f LC   &&   MO %.2f < %.2f MC   &&   RO %.2f < %.2f RC   %.2f > 0.04   &&   %.2f > 0.04",
                leftOpen, leftClose, midOpen, midClose, rightOpen, rightClose, leftMidDiff, rightMidDiff));

        return leftOpen > leftClose && // red
                midOpen < midClose && // green
                rightOpen < rightClose && // green
                leftMidDiff > 0.04 && rightMidDiff > 0.04;

        // return left > mid && mid < right ;
        // return leftMidDiff > 0.03 && midRightDiff > 0.03;
    }

    @Override
    public String getName() {
        return "";
    };
}
