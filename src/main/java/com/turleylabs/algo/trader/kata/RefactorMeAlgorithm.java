package com.turleylabs.algo.trader.kata;

import com.turleylabs.algo.trader.kata.framework.*;
import org.lambda.actions.Action2;
import org.lambda.functions.Function1;

import java.time.LocalDate;

public class RefactorMeAlgorithm extends BaseAlgorithm {

    public static final double VIX_THRESHOLD = 25.0;
    String symbol = "TQQQ";
    public SimpleMovingAverage movingAverage200;
    public SimpleMovingAverage movingAverage50;
    public SimpleMovingAverage movingAverage21;
    public SimpleMovingAverage movingAverage10;
    double previousMovingAverage50;
    double previousMovingAverage21;
    double previousMovingAverage10;
    double previousPrice;
    LocalDate previous;
    CBOE lastVix;
    boolean boughtBelow50;
    boolean tookProfits;

    public void initialize() {
        this.setStartDate(2010, 3, 23);
        this.setEndDate(2020, 03, 06);

        this.setCash(100000);

        movingAverage200 = this.SMA(symbol, 200);
        movingAverage50 = this.SMA(symbol, 50);
        movingAverage21 = this.SMA(symbol, 21);
        movingAverage10 = this.SMA(symbol, 10);
    }

    protected void onData(Slice data) {
        LocalDate date = getDate();
        onData2(data, date, s -> data.get(s));
    }

    public void onData2(Slice data, LocalDate date, Function1<String, Bar> getBar) {

        if (data.getCBOE("VIX") != null) {
            lastVix = data.getCBOE("VIX");
        }
        if (previous == date) {
            return;
        }

        if (!movingAverage200.isReady()) {
            return;
        }
        Bar bar = getBar.call(symbol);
        if (bar == null) {
            this.log(String.format("No data for symbol %s", symbol));
            return;
        }

        if (tookProfits) {
            if (bar.getPrice() < movingAverage10.getValue()) {
                tookProfits = false;
            }
        } else if (portfolio.getOrDefault(symbol, Holding.Default).getQuantity() == 0) {

            perhapsBuyStuff(bar, lastVix.getClose(), previousMovingAverage10, previousMovingAverage21, this::setHoldings);
        } else {
            sellStuff(bar);
        }

        previous = date;
        previousMovingAverage50 = movingAverage50.getValue();
        previousMovingAverage21 = movingAverage21.getValue();
        previousMovingAverage10 = movingAverage10.getValue();
        previousPrice = bar.getPrice();
    }

    public Buy perhapsBuyStuff(Bar bar, double vix, double previousMovingAverage10, double previousMovingAverage21, Action2<String, Double> buyAction) {
        Buy buy = null;
        double price = bar.getPrice();
        double avg10 = movingAverage10.getValue();
        double avg50 = movingAverage50.getValue();
        double avg200 = movingAverage200.getValue();
        double avg21 = movingAverage21.getValue();
        if (isPriceTrendingUp(previousMovingAverage10, previousMovingAverage21, price, avg10, avg21)
                && vix < VIX_THRESHOLD
                && notAnOutliner(price, avg10, avg50, avg200)) {

            this.log(
                    String.format("Buy %s Vix %.4f. above 10 MA %.4f",
                            symbol,
                            vix,
                            (price - avg10) / avg10));

            double amount = 1.0;
            buyAction.call(symbol, amount);

            boolean boughtBelow50 = price < movingAverage50.getValue();

            buy = new Buy(symbol, amount, boughtBelow50);
            this.boughtBelow50 = boughtBelow50;
        }
        return buy;
    }

    private boolean notAnOutliner(double price, double avg10, double avg50, double avg200) {
        return !isPriceAbove15PercentOfMovingAvg50And40PercentMoving200(price, avg50, avg200)
                && isAverageTenOkay(price, avg10);
    }

    private boolean isPriceTrendingUp(double previousMovingAverage10, double previousMovingAverage21, double price, double avg10, double avg21) {
        boolean isAverageBetweenPreviousAndThePrice10 = previousMovingAverage10 < avg10 && avg10 < price;
        boolean isTenDayAverageAboveThePreviousAverages = previousMovingAverage21 < avg21 && avg21 < avg10;
        return isAverageBetweenPreviousAndThePrice10 && isTenDayAverageAboveThePreviousAverages;
    }

    private boolean isAverageTenOkay(double price, double avg10) {
        double differece = price - avg10;
        double result = differece / avg10;
        return result < 0.07;
    }

    private static boolean isPriceAbove15PercentOfMovingAvg50And40PercentMoving200(double price, double avg50, double avg200) {
        boolean above15PercentOfMovAvg50 = (avg50 * 1.15) <= price;
        boolean above40PercentMovAvg200 = (avg200 * 1.40) <= price;
        boolean result = above15PercentOfMovAvg50 && above40PercentMovAvg200;
        return result;
    }

    private void sellStuff(Bar bar) {
        double change = (bar.getPrice() - portfolio.get(symbol).getAveragePrice()) / portfolio.get(symbol).getAveragePrice();

        if (bar.getPrice() < (movingAverage50.getValue() * .93) && !boughtBelow50) {
            this.log(String.format("Sell %s loss of 50 day. Gain %.4f. Vix %.4f", symbol, change, lastVix.getClose()));
            this.liquidate(symbol);
        } else {
            if (lastVix.getClose() > 28.0) {
                this.log(String.format("Sell %s high volatility. Gain %.4f. Vix %.4f", symbol, change, lastVix.getClose()));
                this.liquidate(symbol);
            } else {
                if (movingAverage10.getValue() < 0.97 * movingAverage21.getValue()) {
                    this.log(String.format("Sell %s 10 day below 21 day. Gain %.4f. Vix %.4f", symbol, change, lastVix.getClose()));
                    this.liquidate(symbol);
                } else {
                    if (bar.getPrice() >= (movingAverage50.getValue() * 1.15) && bar.getPrice() >= (movingAverage200.getValue() * 1.40)) {
                        this.log(String.format("Sell %s taking profits. Gain %.4f. Vix %.4f", symbol, change, lastVix.getClose()));
                        this.liquidate(symbol);
                        tookProfits = true;
                    }
                }
            }
        }
    }

    public static class Buy {

        private final String symbol;
        private final double amount;
        private final boolean boughtBelow50;

        public Buy(String symbol, double amount, boolean boughtBelow50) {
            this.symbol = symbol;
            this.amount = amount;
            this.boughtBelow50 = boughtBelow50;
        }

        @Override
        public String toString() {
            return "Buy{" +
                    "symbol='" + symbol + '\'' +
                    ", amount=" + amount +
                    ", boughtBelow50=" + boughtBelow50 +
                    '}';
        }
    }
}
