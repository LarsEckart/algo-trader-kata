package com.turleylabs.algo.trader.kata;

import org.approvaltests.Approvals;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class AlgorithmTest {

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

}
