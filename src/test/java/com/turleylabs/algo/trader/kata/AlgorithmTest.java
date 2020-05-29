package com.turleylabs.algo.trader.kata;

import com.turleylabs.algo.trader.kata.framework.Bar;
import com.turleylabs.algo.trader.kata.framework.SimpleMovingAverage;
import org.approvaltests.combinations.CombinationApprovals;
import org.approvaltests.reporters.ClipboardReporter;
import org.approvaltests.reporters.UseReporter;
import org.junit.Test;

public class AlgorithmTest {

    /*
        @Disabled("too slow")
        @Test
        void algorithmExecutesTrades() {

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

                RefactorMeAlgorithm refactorAlgorithm = new RefactorMeAlgorithm();
                refactorAlgorithm.run();

                Approvals.verify(baos.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    */
    @UseReporter({ClipboardReporter.class})
    //@Test
    public void test_on_data_combinations() {
        var prices = new Double[] {0.06, 0.4, 1.0, 1.4, 2.4, 3.4};
        var diffs = new Integer[] {1, -1};
        var movingAverages = new Double[] {1.0, 2.0, 3.0};

        CombinationApprovals.verifyAllCombinations(
                AlgorithmTest::executeBuy, prices, diffs, movingAverages, movingAverages, movingAverages, movingAverages, movingAverages, movingAverages);
    }

    @Test
    public void test_almost_there() {
        executeBuy(3.21, -1, 3.0, 2.9, 3.025, 3.1, 2.9, 2.8);
    }

    public static RefactorMeAlgorithm.Buy executeBuy(
            Double price, Integer differenceFromVixThreshold, Double movingAvg10, Double movingAvg21, Double movingAvg50, Double movingAvg200, Double previousMovingAverage10,
            Double previousMovingAverage21) {
        RefactorMeAlgorithm refactorAlgorithm = new RefactorMeAlgorithm();

        refactorAlgorithm.movingAverage10 = createMovingAverage(movingAvg10);
        refactorAlgorithm.movingAverage21 = createMovingAverage(movingAvg21);
        refactorAlgorithm.movingAverage50 = createMovingAverage(movingAvg50);
        refactorAlgorithm.movingAverage200 = createMovingAverage(movingAvg200);

        //when
        return refactorAlgorithm.perhapsBuyStuff(new Bar(price), RefactorMeAlgorithm.VIX_THRESHOLD + differenceFromVixThreshold, previousMovingAverage10,
                previousMovingAverage21, (a, b) -> {
                });
    }

    private static SimpleMovingAverage createMovingAverage(double... input) {
        SimpleMovingAverage simpleMovingAverage = new SimpleMovingAverage("anySymbol", input.length);
        for (double i : input) {
            simpleMovingAverage.addData(i);
        }
        return simpleMovingAverage;
    }
}
