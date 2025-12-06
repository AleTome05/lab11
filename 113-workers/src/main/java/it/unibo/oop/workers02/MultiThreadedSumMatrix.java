package it.unibo.oop.workers02;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an implementation of the calculation with matrix.
 *
 */
public final class MultiThreadedSumMatrix implements SumMatrix {

    private final int nthread;

    /**
     * Builds a multithreaded list sum.
     *
     * @param n
     *            no. of thread performing the sum.
     */
    public MultiThreadedSumMatrix(final int n) {
        this.nthread = n;
    }

    @Override
    public double sum(final double[][] matrix) {
        final int r = matrix.length;
        final int baseBlockSize = r / nthread;
        final int remainders = r % nthread;

        final List<Worker> workers = new ArrayList<>(nthread);
        int startRow = 0;

        for (int i = 0; i < nthread; i++) {
            final int rowsToProcess = baseBlockSize + (i < remainders ? 1 : 0);
            if (rowsToProcess > 0) {
                workers.add(new Worker(matrix, startRow, rowsToProcess)); // worker semplificato
            }
            startRow += rowsToProcess;
        }

        /*
         * Start them
         */
        for (final Worker w: workers) {
            w.start();
        }
        /*
         * Wait for every one of them to finish. This operation is _way_ better done by
         * using barriers and latches, and the whole operation would be better done with
         * futures.
         */
        double sum = 0.0;
        for (final Worker w: workers) {
            try {
                w.join();
                sum += w.getResult();
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        /*
         * Return the sum
         */
        return sum;
    }

    private static class Worker extends Thread {
        private final double[][] matrix;
        private final int startRow;
        private final int nRow;
        private double res;

        @SuppressWarnings("PMD.ArrayIsStoredDirectly")
        Worker(final double[][] matrix, final int startRow, final int nRow) {
            super();
            this.matrix = matrix;
            this.startRow = startRow;
            this.nRow = nRow;
        }

        @Override
        public synchronized void run() {
            // Calcolo della riga finale (minimo tra la fine teorica e la fine effettiva della matrice)
            final int endRow = Math.min(this.startRow + this.nRow, this.matrix.length);

            System.out.println("Working from row " + startRow + " to row " + (endRow - 1)); //NOPMD

            // Ciclo esterno sulle righe assegnate
            for (int i = this.startRow; i < endRow; i++) {
                // Ciclo interno su *tutte* le colonne della riga corrente
                for (int j = 0; j < this.matrix[i].length; j++) {
                    this.res += this.matrix[i][j]; // Accesso corretto all'elemento della matrice
                }
            }
        }

        /**
         * Returns the result of summing up the integers within the list.
         *
         * @return the sum of every element in the array
         */
        public synchronized double getResult() {
            return this.res;
        }

    }
}
