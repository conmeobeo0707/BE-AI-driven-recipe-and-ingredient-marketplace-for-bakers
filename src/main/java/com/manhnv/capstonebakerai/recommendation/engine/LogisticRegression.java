package com.manhnv.capstonebakerai.recommendation.engine;

import java.util.List;

// Binary logistic regression: p(liked) = sigmoid(bias + w . features), written in plain Java.
// Trained with batch gradient descent on the mean log-loss plus a small L2 penalty on w.
public final class LogisticRegression {

    private static final double LEARNING_RATE = 0.5;
    private static final int MAX_ITERATIONS = 3000;
    private static final double L2 = 0.01;
    private static final double TOLERANCE = 1e-7;
    private static final double EPSILON = 1e-12;

    private LogisticRegression() {
    }

    // label 1 = the customer liked the recipe, 0 = a random recipe they did not like.
    public record Sample(double[] features, int label) {
    }

    public record Result(double[] weights, double bias, double loss, int iterations) {
    }

    public static Result train(List<Sample> samples) {
        int n = samples.size();
        int d = samples.get(0).features().length;
        double[] w = new double[d];
        long positives = samples.stream().filter(s -> s.label() == 1).count();
        // Start the bias at the base rate so training only has to learn the weights.
        double bias = positives == 0 || positives == n ? 0 : Math.log((double) positives / (n - positives));

        double previousLoss = Double.POSITIVE_INFINITY;
        double loss = 0;
        int iteration = 0;
        while (iteration < MAX_ITERATIONS) {
            iteration++;
            double[] gradient = new double[d];
            double biasGradient = 0;
            loss = 0;
            for (Sample sample : samples) {
                double p = predict(w, bias, sample.features());
                double error = p - sample.label();
                for (int j = 0; j < d; j++) {
                    gradient[j] += error * sample.features()[j];
                }
                biasGradient += error;
                loss -= sample.label() * Math.log(p + EPSILON) + (1 - sample.label()) * Math.log(1 - p + EPSILON);
            }
            double penalty = 0;
            for (int j = 0; j < d; j++) {
                penalty += w[j] * w[j];
                w[j] -= LEARNING_RATE * (gradient[j] / n + L2 * w[j]);
            }
            bias -= LEARNING_RATE * biasGradient / n;
            loss = loss / n + L2 / 2 * penalty;
            if (Math.abs(previousLoss - loss) < TOLERANCE) {
                break;
            }
            previousLoss = loss;
        }
        return new Result(w, bias, loss, iteration);
    }

    static double predict(double[] w, double bias, double[] x) {
        double z = bias;
        for (int j = 0; j < w.length; j++) {
            z += w[j] * x[j];
        }
        return 1 / (1 + Math.exp(-z));
    }
}
