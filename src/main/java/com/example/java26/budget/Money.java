package com.example.java26.budget;

final class Money {
    private Money() {

    }
    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
